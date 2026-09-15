//
//  GemmaInferenceBridgeCacheTests.swift
//  PayslipMaxTests
//
//  Covers LoadOrCreateCache.loadOrCreate, the get-or-create logic behind
//  GemmaInferenceBridge.engine(for:). The real cached value is a LiteRT-LM `Engine` loading a
//  ~500MB model — too heavy to construct in a unit test — so this exercises the generic
//  KeyedCache/loadOrCreate machinery directly with a cheap stand-in value.
//

import XCTest
@testable import PayslipMax

private actor CallCounter {
    private(set) var count = 0

    func increment() -> Int {
        count += 1
        return count
    }
}

final class GemmaInferenceBridgeCacheTests: XCTestCase {

    func test_createRunsExactlyOnce_forRepeatedLookupsOfSameKey() async throws {
        let cache = KeyedCache<String, Int>()
        let counter = CallCounter()

        func load() async throws -> Int {
            try await LoadOrCreateCache.loadOrCreate(key: "model-a", cache: cache) {
                await counter.increment()
            }
        }

        let first = try await load()
        let second = try await load()
        let third = try await load()

        let calls = await counter.count
        XCTAssertEqual(calls, 1, "create must run exactly once across repeated lookups of the same key")
        XCTAssertEqual(first, second)
        XCTAssertEqual(second, third)
    }

    func test_createRunsSeparately_forDifferentKeys() async throws {
        let cache = KeyedCache<String, String>()
        let counter = CallCounter()

        let a = try await LoadOrCreateCache.loadOrCreate(key: "model-a", cache: cache) {
            "value-\(await counter.increment())"
        }
        let b = try await LoadOrCreateCache.loadOrCreate(key: "model-b", cache: cache) {
            "value-\(await counter.increment())"
        }

        let calls = await counter.count
        XCTAssertEqual(calls, 2, "different keys must each get their own create() call")
        XCTAssertNotEqual(a, b)
    }

    func test_secondLookup_returnsSameCachedInstance() async throws {
        let cache = KeyedCache<String, UUID>()

        let first = try await LoadOrCreateCache.loadOrCreate(key: "k", cache: cache) { UUID() }
        let second = try await LoadOrCreateCache.loadOrCreate(key: "k", cache: cache) { UUID() }

        XCTAssertEqual(first, second, "a cache hit must return the exact stored value, not a new one")
    }
}
