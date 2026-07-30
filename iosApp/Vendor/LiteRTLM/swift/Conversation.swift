// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import Foundation
import OSLog
import CLiteRTLM

typealias CConversationHandle = OpaquePointer

private let logger = Logger(
  subsystem: "com.google.odml.litertlm.swift",
  category: "Conversation"
)

private let recurringToolCallLimit = 25

/// Represents a conversation with the LiteRT-LM model.
///
/// Example usage:
/// ```swift
/// // Assuming 'engine' is an instance of Engine
/// let conversation = try await engine.createConversation()
///
/// // Send a message and get the response.
/// let response = try conversation.sendMessage(Message("Hello world"))
///
/// // Send a message async with response chunks as AsyncThrowingStream.
/// for try await chunk in conversation.sendMessageStream(Message("Hello world")) {
///   print(chunk.text)
/// }
/// ```
///
/// This class facilitates interaction with the LiteRT-LM model by handling message sending
/// and response reception.
public class Conversation {
  private let logger = Logger(
    subsystem: "com.google.ai.edge.litertlm.swift",
    category: "Conversation"
  )

  private var handle: CConversationHandle?
  private let toolManager: ToolManager

  /// Whether the conversation is alive and ready to be used.
  public var isAlive: Bool {
    return handle != nil
  }

  init(handle: CConversationHandle, toolManager: ToolManager) {
    self.handle = handle
    self.toolManager = toolManager
  }

  deinit {
    if let handle = handle {
      litert_lm_conversation_delete(handle)
    }
  }

  /// Sends a message to the model and returns the response. This is a synchronous call.
  ///
  /// - Parameter message: The message to send to the model.
  /// - Parameter extraContext: The extra context to send to the model.
  /// - Returns: The model's response message.
  /// - Throws: A `LiteRTLMError` if sending the message fails or the model
  ///   returns an invalid response.
  public func sendMessage(_ message: Message, extraContext: [String: Any]? = nil) async throws
    -> Message
  {
    let handle = try checkIsAlive()

    var currentMessageJson: [String: Any] = message.toJson

    for _ in 0..<recurringToolCallLimit {
      let (responseJson, responseString) = try attemptSendMessage(
        handle: handle, messageJson: currentMessageJson, extraContext: extraContext)

      guard let toolCalls = responseJson["tool_calls"] as? [[String: Any]] else {
        if responseJson["content"] != nil || responseJson["channels"] != nil {
          return try Conversation.jsonToMessage(responseString)
        } else {
          throw LiteRTLMError.conversation(.invalidResponse(responseString))
        }
      }
      currentMessageJson = try await handleToolCalls(toolCalls)
    }
    throw LiteRTLMError.conversation(.recurringToolCallLimitExceeded(limit: recurringToolCallLimit))
  }

  private func attemptSendMessage(
    handle: CConversationHandle, messageJson: [String: Any], extraContext: [String: Any]?
  ) throws
    -> (responseJson: [String: Any], responseString: String)
  {
    let messageData = try JSONSerialization.data(withJSONObject: messageJson)
    guard let messageString = String(data: messageData, encoding: .utf8) else {
      throw LiteRTLMError.conversation(.failedToSerializeMessage)
    }

    var extraContextString: String? = nil
    if let extraContext = extraContext, !extraContext.isEmpty,
      let extraData = try? JSONSerialization.data(withJSONObject: extraContext)
    {
      extraContextString = String(data: extraData, encoding: .utf8)
    }
    let optionalArgs = litert_lm_conversation_optional_args_create()
    if let visualTokenBudget = ExperimentalFlags.visualTokenBudget {
      litert_lm_conversation_optional_args_set_visual_token_budget(optionalArgs, Int32(visualTokenBudget))
    }
    defer { litert_lm_conversation_optional_args_delete(optionalArgs) }

    guard let responsePtr = litert_lm_conversation_send_message(
        handle, messageString, extraContextString, optionalArgs)
    else {
      throw LiteRTLMError.conversation(.invalidResponse("Native sendMessage returned null."))
    }
    let responsePtrRef = responsePtr
    defer { litert_lm_json_response_delete(responsePtrRef) }

    guard let responseChars = litert_lm_json_response_get_string(responsePtr) else {
      throw LiteRTLMError.conversation(
        .invalidResponse("Native get string for response returned null."))
    }
    let responseString = String(cString: responseChars)

    guard let responseData = responseString.data(using: .utf8),
      let responseJson = try JSONSerialization.jsonObject(with: responseData) as? [String: Any]
    else {
      throw LiteRTLMError.conversation(.invalidJson("Failed to parse native response JSON."))
    }
    return (responseJson, responseString)
  }

  func handleToolCalls(_ toolCalls: [[String: Any]]) async throws -> [String: Any] {
    var toolResponses: [[String: Any]] = []

    for toolCall in toolCalls {
      guard let function = toolCall["function"] as? [String: Any],
        let name = function["name"] as? String,
        let argsObject = function["arguments"] as? [String: Any]
      else {
        continue
      }
      do {
        let result = try await toolManager.execute(name: name, arguments: argsObject)

        toolResponses.append([
          "type": "tool_response",
          "name": name,
          "response": result,
        ])
      } catch {
        throw LiteRTLMError.conversation(.toolExecutionError(name: name, error: "\(error)"))
      }
    }

    return ["role": "tool", "content": toolResponses]
  }

  /// Throws an error if the conversation is not alive.
  ///
  /// - Returns: The `OpaquePointer` handle if the conversation is alive.
  /// - Throws: A `LiteRTLMError` if `handle` is nil, indicating the conversation is not alive.
  func checkIsAlive() throws -> OpaquePointer {
    guard let handle else {
      throw LiteRTLMError.conversation(.notAlive)
    }
    return handle
  }

  /// Cancels the ongoing asynchronous inference process.
  public func cancel() throws {
    let handle = try checkIsAlive()
    litert_lm_conversation_cancel_process(handle)
  }

  /// Renders the message into a string for testing and logging.
  ///
  /// This function does not need to be called for actual message sending, as the `sendMessage` and
  /// `sendMessageStream` functions will handle rendering internally.
  ///
  /// - Parameter message: The message to render.
  /// - Returns: The rendered message string.
  /// - Throws: A `LiteRTLMError` if the conversation is not alive, serializing fails, or rendering fails.
  public func renderMessageIntoString(_ message: Message) throws -> String {
    let handle = try checkIsAlive()
    let messageData = try JSONSerialization.data(withJSONObject: message.toJson)
    guard let messageString = String(data: messageData, encoding: .utf8) else {
      throw LiteRTLMError.conversation(.failedToSerializeMessage)
    }

    guard let cString = litert_lm_conversation_render_message_to_string(handle, messageString)
    else {
      throw LiteRTLMError.conversation(.invalidResponse("Failed to render message into string."))
    }
    return String(cString: cString)
  }
}
