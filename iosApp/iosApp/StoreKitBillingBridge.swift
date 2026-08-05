import Foundation
import StoreKit
import composeApp

/// Bridges shared Kotlin `IosBillingManager` delegates to StoreKit 2 APIs.
/// Provides offline sandbox support using Xcode `.storekit` configuration.
final class StoreKitBillingBridge {
    static let shared = StoreKitBillingBridge()
    private let PRODUCT_ID = "com.payslipmax.pro.yearly"

    private var updateListenerTask: Task<Void, Error>? = nil

    private init() {}

    /// Registers the StoreKit 2 bridge with shared KMP `IosBillingManager`. Call once at app startup.
    static func register() {
        IosBillingManager.companion.purchaseDelegate = { completion in
            StoreKitBillingBridge.shared.purchase { success, token, error in
                _ = completion(KotlinBoolean(value: success), token, error)
            }
        }

        IosBillingManager.companion.restoreDelegate = { completion in
            StoreKitBillingBridge.shared.restore { success, token, error in
                _ = completion(KotlinBoolean(value: success), token, error)
            }
        }

        StoreKitBillingBridge.shared.listenForTransactionUpdates()
        StoreKitBillingBridge.shared.checkCurrentEntitlements()
    }

    /// Listens for background transaction updates (renewals, revocations, sandbox purchases).
    private func listenForTransactionUpdates() {
        updateListenerTask = Task.detached {
            for await result in Transaction.updates {
                do {
                    let transaction = try self.checkVerified(result)
                    await self.updateEntitlementState(for: transaction)
                    await transaction.finish()
                } catch {
                    print("Unverified StoreKit transaction update: \(error)")
                }
            }
        }
    }

    /// Queries active StoreKit 2 entitlements and updates Kotlin SSOT state.
    func checkCurrentEntitlements() {
        Task {
            var hasActiveSubscription = false
            for await result in Transaction.currentEntitlements {
                do {
                    let transaction = try checkVerified(result)
                    if transaction.productID == PRODUCT_ID && transaction.revocationDate == nil {
                        if let expirationDate = transaction.expirationDate, expirationDate > Date() {
                            hasActiveSubscription = true
                        } else if transaction.expirationDate == nil {
                            hasActiveSubscription = true
                        }
                    }
                } catch {
                    print("Error checking StoreKit entitlement: \(error)")
                }
            }
            let newState: SubscriptionState = hasActiveSubscription ? SubscriptionState.Active(expirationTimestampMs: 0, autoRenewing: true) : SubscriptionState.Inactive()
            IosBillingManager.companion.statusUpdateListener?(newState)
        }
    }

    /// Executes purchase flow for `com.payslipmax.pro.yearly`.
    func purchase(completion: @escaping (Bool, String?, String?) -> Void) {
        Task {
            do {
                let products = try await Product.products(for: [PRODUCT_ID])
                guard let product = products.first else {
                    completion(false, nil, "Product \(PRODUCT_ID) not found in StoreKit")
                    return
                }

                let result = try await product.purchase()
                switch result {
                case .success(let verification):
                    let transaction = try checkVerified(verification)
                    await transaction.finish()
                    let state = SubscriptionState.Active(expirationTimestampMs: 0, autoRenewing: true)
                    IosBillingManager.companion.statusUpdateListener?(state)
                    completion(true, String(transaction.id), nil)
                case .userCancelled:
                    completion(false, nil, "USER_CANCELLED")
                case .pending:
                    completion(false, nil, "Purchase pending authorization")
                @unknown default:
                    completion(false, nil, "Unknown purchase result")
                }
            } catch {
                completion(false, nil, error.localizedDescription)
            }
        }
    }

    /// Restores past purchases.
    func restore(completion: @escaping (Bool, String?, String?) -> Void) {
        Task {
            do {
                try await AppStore.sync()
                checkCurrentEntitlements()
                completion(true, "restored", nil)
            } catch {
                completion(false, nil, error.localizedDescription)
            }
        }
    }

    private func updateEntitlementState(for transaction: Transaction) async {
        if transaction.productID == PRODUCT_ID && transaction.revocationDate == nil {
            let active = SubscriptionState.Active(expirationTimestampMs: 0, autoRenewing: true)
            IosBillingManager.companion.statusUpdateListener?(active)
        }
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified(_, let error):
            throw error
        case .verified(let safe):
            return safe
        }
    }

    deinit {
        updateListenerTask?.cancel()
    }
}
