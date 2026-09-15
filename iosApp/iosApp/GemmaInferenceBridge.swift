import Foundation
import LiteRTLM
import composeApp

/// Bridges the shared Kotlin `GemmaEngine.inferenceDelegate` to LiteRT-LM's Swift `Engine`/`Conversation`
/// API. Kotlin/Native cinterop cannot bind LiteRT-LM's pure-Swift package (it ships no Objective-C
/// headers), so the shared `GemmaEngine.ios.kt` fails loudly unless this closure is registered at
/// startup — the same "Swift-only capability behind a plain closure" pattern the Firebase Auth token
/// provider uses.
///
/// One `Engine` is loaded per model path and cached across prompts: loading the ~500MB `.litertlm`
/// model on every Tier 6 call would be prohibitively slow. LiteRT-LM is session-based, so each prompt
/// runs through a fresh, stateless `Conversation` (Tier 6 extraction has no multi-turn history — history
/// must never bleed between payslips).
/// Actor-isolated cache keyed by an arbitrary `Hashable` key. Actors are the Swift 6-mandated
/// async-safe replacement for locking a mutable dictionary across `await` boundaries. Generic (not
/// `Engine`-specific) so `loadOrCreate`'s caching behavior is testable without loading a real model.
actor KeyedCache<Key: Hashable, Value> {
    private var values: [Key: Value] = [:]

    func value(for key: Key) -> Value? {
        values[key]
    }

    func store(_ value: Value, for key: Key) {
        values[key] = value
    }
}

enum LoadOrCreateCache {
    /// Returns the cached value for `key` if present; otherwise runs `create()` once, caches the
    /// result, and returns it. Extracted for testability (see `GemmaInferenceBridgeCacheTests`) —
    /// verifies `create` runs at most once per key, which is what keeps the ~500MB Gemma model load
    /// off the hot path for every Tier 6 call after the first.
    static func loadOrCreate<Key: Hashable, Value>(
        key: Key,
        cache: KeyedCache<Key, Value>,
        create: () async throws -> Value
    ) async throws -> Value {
        if let cached = await cache.value(for: key) {
            return cached
        }
        let value = try await create()
        await cache.store(value, for: key)
        return value
    }
}

final class GemmaInferenceBridge {
    static let shared = GemmaInferenceBridge()

    private let store = KeyedCache<String, Engine>()

    private init() {}

    /// Registers the bridge on the shared Kotlin companion. Call once at app startup.
    static func register() {
        GemmaEngine.companion.inferenceDelegate = { modelPath, prompt, completion in
            GemmaInferenceBridge.shared.generate(modelPath: modelPath, prompt: prompt) { text, error in
                _ = completion(text, error)
            }
        }
    }

    /// Runs one stateless inference turn. Invokes `completion(text, nil)` on success or
    /// `completion(nil, message)` on failure — exactly one argument is non-nil, matching the contract
    /// the Kotlin `GemmaEngine.ios.kt` continuation expects.
    func generate(modelPath: String, prompt: String, completion: @escaping (String?, String?) -> Void) {
        Task {
            do {
                let engine = try await self.engine(for: modelPath)
                // Sampler defaults mirror the Kotlin `GemmaEngineConfig` defaults (temperature 0.2,
                // topK 40, topP 0.95). iOS `PdfParser` always constructs the engine with those defaults,
                // so this is the matching single-source behaviour, not a second knob.
                let samplerConfig = try SamplerConfig(topK: 40, topP: 0.95, temperature: 0.2)
                let conversationConfig = ConversationConfig(samplerConfig: samplerConfig)
                let conversation = try await engine.createConversation(with: conversationConfig)
                let response = try await conversation.sendMessage(Message(prompt))
                completion(response.toString, nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    /// Returns the cached `Engine` for `modelPath`, loading and initializing it on first use. The
    /// cache is guarded by the `EngineStore` actor; initialization runs between the lookup and the
    /// store call, outside actor isolation. Two concurrent first-loads could both initialize — a
    /// benign, rare race for Tier 6 (invoked at most once per parse) where the last writer simply
    /// wins the cache slot.
    private func engine(for modelPath: String) async throws -> Engine {
        try await LoadOrCreateCache.loadOrCreate(key: modelPath, cache: store) {
            let engineConfig = try EngineConfig(
                modelPath: modelPath,
                backend: .cpu(),
                maxNumTokens: 512,
                cacheDir: NSTemporaryDirectory()
            )
            let engine = Engine(engineConfig: engineConfig)
            try await engine.initialize()
            return engine
        }
    }
}
