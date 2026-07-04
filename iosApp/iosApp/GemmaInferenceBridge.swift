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
final class GemmaInferenceBridge {
    static let shared = GemmaInferenceBridge()

    private let lock = NSLock()
    private var engines: [String: Engine] = [:]

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
    /// cache lookup is lock-guarded; initialization runs outside the lock (holding a lock across
    /// `await` is unsafe). Two concurrent first-loads could both initialize — a benign, rare race for
    /// Tier 6 (invoked at most once per parse) where the last writer simply wins the cache slot.
    private func engine(for modelPath: String) async throws -> Engine {
        lock.lock()
        if let cached = engines[modelPath] {
            lock.unlock()
            return cached
        }
        lock.unlock()

        let engineConfig = try EngineConfig(
            modelPath: modelPath,
            backend: .cpu(),
            maxNumTokens: 512,
            cacheDir: NSTemporaryDirectory()
        )
        let engine = Engine(engineConfig: engineConfig)
        try await engine.initialize()

        lock.lock()
        engines[modelPath] = engine
        lock.unlock()
        return engine
    }
}
