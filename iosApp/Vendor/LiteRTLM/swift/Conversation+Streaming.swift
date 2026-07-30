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

private let logger = Logger(
  subsystem: "com.google.odml.litertlm.swift",
  category: "ConversationStreaming"
)

private let recurringToolCallLimit = 25

extension Conversation {
  /// Context object to bridge the C callback to the Swift AsyncThrowingStream.
  class StreamContext {
    let continuation: AsyncThrowingStream<Message, Error>.Continuation
    let conversation: Conversation
    var toolCallCount: Int = 0
    var pendingToolCalls: [[String: Any]] = []

    init(continuation: AsyncThrowingStream<Message, Error>.Continuation, conversation: Conversation)
    {
      self.continuation = continuation
      self.conversation = conversation
    }
  }

  /// Sends a message to the model and returns an async stream of response chunks.
  ///
  /// - Parameter message: The message to send.
  /// - Parameter extraContext: The extra context to send to the model.
  /// - Returns: An async throwing stream of `Message` chunks.
  public func sendMessageStream(_ message: Message, extraContext: [String: Any]? = nil)
    -> AsyncThrowingStream<Message, Error>
  {
    return AsyncThrowingStream { continuation in
      do {
        let handle = try self.checkIsAlive()
        let messageJson: [String: Any] = message.toJson
        let context = StreamContext(continuation: continuation, conversation: self)

        try self.sendToStream(
          handle: handle, messageJson: messageJson, extraContext: extraContext, context: context)
      } catch {
        continuation.finish(throwing: error)
      }
    }
  }

  /// Sends a message to the model and handles the response via a streaming callback.
  func sendToStream(
    handle: CConversationHandle,
    messageJson: [String: Any],
    extraContext: [String: Any]? = nil,
    context: StreamContext
  ) throws {
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

    let contextPtr = Unmanaged.passRetained(context).toOpaque()

    let status = litert_lm_conversation_send_message_stream(
      handle,
      messageString,
      extraContextString,
      optionalArgs,
      streamCallback,
      contextPtr
    )

    guard status == 0 else {
      Unmanaged<StreamContext>.fromOpaque(contextPtr).release()
      throw LiteRTLMError.conversation(.failedToStartStream(status: Int(status)))
    }
  }
}

/// A callback function to bridge the C callback to the Swift AsyncThrowingStream.
func streamCallback(
  userData: UnsafeMutableRawPointer?,
  responseJson: UnsafePointer<CChar>?,
  isFinal: Bool,
  errorMessage: UnsafePointer<CChar>?
) {
  guard let userData = userData else { return }

  let context = Unmanaged<Conversation.StreamContext>.fromOpaque(userData).takeUnretainedValue()

  if let errorMessage = errorMessage {
    let errorString = String(cString: errorMessage)
    let error = LiteRTLMError.conversation(.invalidResponse(errorString))
    context.continuation.finish(throwing: error)

    Unmanaged<Conversation.StreamContext>.fromOpaque(userData).release()
    return
  }

  if let responseJson = responseJson {
    let responseString = String(cString: responseJson)
    do {
      guard let responseData = responseString.data(using: .utf8),
        let jsonObject = try JSONSerialization.jsonObject(with: responseData) as? [String: Any]
      else {
        throw LiteRTLMError.conversation(.invalidJson("Invalid JSON chunk"))
      }

      if let toolCalls = jsonObject["tool_calls"] as? [[String: Any]] {
        context.pendingToolCalls.append(contentsOf: toolCalls)
      }

      if jsonObject["content"] != nil || jsonObject["channels"] != nil {
        let message = try Conversation.jsonToMessage(responseString)
        context.continuation.yield(message)
      }
    } catch {
      logger.error("Failed to parse response JSON: \(error.localizedDescription)")
      context.continuation.finish(throwing: error)
      Unmanaged<Conversation.StreamContext>.fromOpaque(userData).release()
      return
    }
  }

  if isFinal {
    if !context.pendingToolCalls.isEmpty {
      if context.toolCallCount >= recurringToolCallLimit {
        context.continuation.finish(
          throwing: LiteRTLMError.conversation(
            .recurringToolCallLimitExceeded(limit: recurringToolCallLimit)))
        Unmanaged<Conversation.StreamContext>.fromOpaque(userData).release()
        return
      }

      context.toolCallCount += 1
      let toolCalls = context.pendingToolCalls
      context.pendingToolCalls = []

      Task {
        do {
          let toolResponseJson = try await context.conversation.handleToolCalls(toolCalls)
          try context.conversation.sendToStream(
            handle: context.conversation.checkIsAlive(),
            messageJson: toolResponseJson,
            context: context
          )
        } catch {
          context.continuation.finish(throwing: error)
        }
        Unmanaged<Conversation.StreamContext>.fromOpaque(userData).release()
      }
    } else {
      context.continuation.finish()
      Unmanaged<Conversation.StreamContext>.fromOpaque(userData).release()
    }
  }
}
