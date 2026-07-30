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
import CLiteRTLM

extension Conversation {
  /// Retrieves the benchmark information from the conversation.
  ///
  /// - Returns: The benchmark information
  /// - Throws: A `LiteRTLMError` if the benchmark flag is not enabled or info is unavailable.
  public func getBenchmarkInfo() throws -> BenchmarkInfo {
    let handle = try checkIsAlive()

    if !ExperimentalFlags.enableBenchmark {
      throw LiteRTLMError.conversation(.benchmarkNotEnabled)
    }

    guard let benchmarkInfoPtr = litert_lm_conversation_get_benchmark_info(handle) else {
      throw LiteRTLMError.conversation(.benchmarkInfoUnavailable)
    }
    defer { litert_lm_benchmark_info_delete(benchmarkInfoPtr) }

    let numPrefillTurns = litert_lm_benchmark_info_get_num_prefill_turns(benchmarkInfoPtr)
    let numDecodeTurns = litert_lm_benchmark_info_get_num_decode_turns(benchmarkInfoPtr)

    let initTimeInSecond = litert_lm_benchmark_info_get_total_init_time_in_second(benchmarkInfoPtr)
    let timeToFirstTokenInSecond = litert_lm_benchmark_info_get_time_to_first_token(
      benchmarkInfoPtr)

    let lastPrefillTokenCount: Int =
      numPrefillTurns > 0
      ? Int(
        litert_lm_benchmark_info_get_prefill_token_count_at(
          benchmarkInfoPtr, numPrefillTurns - 1)) : 0
    let lastPrefillTokensPerSec: Double =
      numPrefillTurns > 0
      ? litert_lm_benchmark_info_get_prefill_tokens_per_sec_at(
        benchmarkInfoPtr, numPrefillTurns - 1) : 0.0

    let lastDecodeTokenCount: Int =
      numDecodeTurns > 0
      ? Int(
        litert_lm_benchmark_info_get_decode_token_count_at(
          benchmarkInfoPtr, numDecodeTurns - 1)) : 0
    let lastDecodeTokensPerSec: Double =
      numDecodeTurns > 0
      ? litert_lm_benchmark_info_get_decode_tokens_per_sec_at(
        benchmarkInfoPtr, numDecodeTurns - 1) : 0.0

    return BenchmarkInfo(
      initTimeInSecond: initTimeInSecond,
      timeToFirstTokenInSecond: timeToFirstTokenInSecond,
      lastPrefillTokenCount: lastPrefillTokenCount,
      lastDecodeTokenCount: lastDecodeTokenCount,
      lastPrefillTokensPerSecond: lastPrefillTokensPerSec,
      lastDecodeTokensPerSecond: lastDecodeTokensPerSec
    )
  }
}
