//
//  AuthTokenFetcherTests.swift
//  PayslipMaxTests
//
//  Covers AuthTokenFetcher.fetchIdToken, the retry/completion-once state machine behind the
//  Firebase ID-token bridge (AuthTokenProvider.companion.tokenProviderDelegate). Firebase's User/Auth
//  types aren't fakeable directly, so the fetcher is generic over a test double instead.
//

import XCTest
@testable import PayslipMax

private struct FakeUser: Equatable {
    let id: String
}

final class AuthTokenFetcherTests: XCTestCase {

    // MARK: - No retry needed

    func test_returnsToken_whenCurrentUserPresentAndTokenFetchSucceeds() {
        let expectation = expectation(description: "completion")
        var received: String?

        AuthTokenFetcher.fetchIdToken(
            currentUser: FakeUser(id: "u1"),
            signInAnonymously: { _ in XCTFail("must not sign in again when a token was fetched") },
            getIDToken: { user, onToken in
                XCTAssertEqual(user, FakeUser(id: "u1"))
                onToken("valid-token", nil)
            },
            completion: { token in
                received = token
                expectation.fulfill()
            }
        )

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(received, "valid-token")
    }

    // MARK: - Retry gate: only error code 17999 retries

    func test_retriesSignIn_onlyOnInternalErrorCode17999() {
        let expectation = expectation(description: "completion")
        var signInCallCount = 0
        var received: String?

        AuthTokenFetcher.fetchIdToken(
            currentUser: FakeUser(id: "u1"),
            signInAnonymously: { onUser in
                signInCallCount += 1
                onUser(FakeUser(id: "u2"))
            },
            getIDToken: { user, onToken in
                if user == FakeUser(id: "u1") {
                    onToken(nil, 17999)
                } else {
                    onToken("retry-token", nil)
                }
            },
            completion: { token in
                received = token
                expectation.fulfill()
            }
        )

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(signInCallCount, 1)
        XCTAssertEqual(received, "retry-token")
    }

    func test_doesNotRetry_onNonRetryableErrorCode() {
        let expectation = expectation(description: "completion")
        var received: String? = "not-yet-set"

        AuthTokenFetcher.fetchIdToken(
            currentUser: FakeUser(id: "u1"),
            signInAnonymously: { _ in XCTFail("must not retry on a non-17999 error") },
            getIDToken: { _, onToken in onToken(nil, 42) },
            completion: { token in
                received = token
                expectation.fulfill()
            }
        )

        wait(for: [expectation], timeout: 1.0)
        XCTAssertNil(received)
    }

    // MARK: - Completion called exactly once, on every path

    func test_completesOnce_whenRetrySignInFails() {
        let expectation = expectation(description: "completion")
        var completionCallCount = 0

        AuthTokenFetcher.fetchIdToken(
            currentUser: FakeUser(id: "u1"),
            signInAnonymously: { onUser in onUser(nil) },
            getIDToken: { _, onToken in onToken(nil, 17999) },
            completion: { token in
                completionCallCount += 1
                XCTAssertNil(token)
                expectation.fulfill()
            }
        )

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(completionCallCount, 1)
    }

    func test_completesOnce_whenNoCurrentUserAndInitialSignInFails() {
        let expectation = expectation(description: "completion")
        var completionCallCount = 0

        AuthTokenFetcher.fetchIdToken(
            currentUser: FakeUser?.none,
            signInAnonymously: { onUser in onUser(nil) },
            getIDToken: { _, _ in XCTFail("must not fetch a token with no signed-in user") },
            completion: { token in
                completionCallCount += 1
                XCTAssertNil(token)
                expectation.fulfill()
            }
        )

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(completionCallCount, 1)
    }

    func test_signsInThenFetchesToken_whenNoCurrentUser() {
        let expectation = expectation(description: "completion")
        var received: String?

        AuthTokenFetcher.fetchIdToken(
            currentUser: FakeUser?.none,
            signInAnonymously: { onUser in onUser(FakeUser(id: "fresh")) },
            getIDToken: { user, onToken in
                XCTAssertEqual(user, FakeUser(id: "fresh"))
                onToken("fresh-token", nil)
            },
            completion: { token in
                received = token
                expectation.fulfill()
            }
        )

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(received, "fresh-token")
    }
}
