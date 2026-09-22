import Foundation
import StoreKit
import UIKit
import shared

@available(iOS 15.0, *)
final class StoreKitFullAccessBillingAdapter: NSObject, FullAccessBillingAdapter {
    private enum ProductIds {
        static let lifetime = FullAccessBillingProductIds.shared.LIFETIME
    }

    private var transactionUpdates: Task<Void, Never>?
    private var foregroundObserver: NSObjectProtocol?
    private let stateLock = NSLock()
    private var entitlementObserver: FullAccessBillingObserver?
    private var undeliveredTransactions: [UInt64: Transaction] = [:]
    private var deliveryBatches: [String: [Transaction]] = [:]
    private var deliveryOrder: [String] = []

    override init() {
        super.init()
        transactionUpdates = Task.detached { [weak self] in
            for await update in Transaction.updates {
                guard case .verified(let transaction) = update,
                      transaction.productID == ProductIds.lifetime else { continue }
                self?.stageDelivery(transaction)
                self?.notifyEntitlementsChanged()
            }
        }
        foregroundObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main
        ) { [weak self] _ in
            self?.notifyEntitlementsChanged()
        }
    }

    deinit {
        transactionUpdates?.cancel()
        if let foregroundObserver {
            NotificationCenter.default.removeObserver(foregroundObserver)
        }
    }

    func setEntitlementObserver(observer: FullAccessBillingObserver?) {
        stateLock.lock()
        entitlementObserver = observer
        stateLock.unlock()
    }

    func completeEntitlementDelivery(deliveryToken: String?, completionHandler: @escaping (FoundationResult?, Error?) -> Void) {
        Task {
            // Shared code invokes this only after access has been saved successfully.
            do {
                for transaction in try deliveryBatch(deliveryToken) {
                    await transaction.finish()
                    removeDelivery(transaction)
                }
                completionHandler(success(KotlinUnit.shared), nil)
            } catch {
                completionHandler(failure(error.localizedDescription), nil)
            }
        }
    }

    private func notifyEntitlementsChanged() {
        stateLock.lock()
        let observer = entitlementObserver
        stateLock.unlock()
        observer?.onEntitlementsChanged()
    }

    private func stageDelivery(_ transaction: Transaction) {
        stateLock.lock()
        undeliveredTransactions[transaction.id] = transaction
        stateLock.unlock()
    }

    private func stagedDeliveries() -> [Transaction] {
        stateLock.lock()
        defer { stateLock.unlock() }
        return Array(undeliveredTransactions.values)
    }

    private func prepareDelivery(_ transactions: [Transaction]) -> String? {
        guard !transactions.isEmpty else { return nil }
        stateLock.lock()
        defer { stateLock.unlock() }
        let token = UUID().uuidString
        deliveryBatches[token] = transactions
        deliveryOrder.append(token)
        // Abandoned native requests can outlive a canceled shared caller. StoreKit
        // retains unfinished transactions, so evicting old batches is retry-safe.
        while deliveryOrder.count > 16 {
            deliveryBatches.removeValue(forKey: deliveryOrder.removeFirst())
        }
        return token
    }

    private func deliveryBatch(_ token: String?) throws -> [Transaction] {
        guard let token else { return [] }
        stateLock.lock()
        defer { stateLock.unlock() }
        deliveryOrder.removeAll { $0 == token }
        guard let transactions = deliveryBatches.removeValue(forKey: token) else {
            throw StoreKitFullAccessError.deliveryExpired
        }
        return transactions
    }

    private func removeDelivery(_ transaction: Transaction) {
        stateLock.lock()
        defer { stateLock.unlock() }
        // A newer update for the same transaction must survive the older finish.
        if undeliveredTransactions[transaction.id]?.jsonRepresentation == transaction.jsonRepresentation {
            undeliveredTransactions.removeValue(forKey: transaction.id)
        }
    }

    func loadOffers(completionHandler: @escaping (FoundationResult?, Error?) -> Void) {
        Task {
            do {
                let products = try await Product.products(for: [ProductIds.lifetime])
                let offers = products.compactMap { offer(for: $0) }
                guard !offers.isEmpty else {
                    completionHandler(failure("App Store products were not found. Check Full Access product IDs."), nil)
                    return
                }
                completionHandler(success(offers), nil)
            } catch {
                completionHandler(failure("Could not load App Store products. \(error.localizedDescription)"), nil)
            }
        }
    }

    func purchaseLifetimeUnlock(completionHandler: @escaping (FoundationResult?, Error?) -> Void) {
        Task {
            do {
                let storeProduct = try await productDetails()
                let result = try await storeProduct.purchase()
                switch result {
                case .success(let verification):
                    let transaction = try checkVerified(verification)
                    stageDelivery(transaction)
                    notifyEntitlementsChanged()
                    completionHandler(success(try await entitlementSnapshot()), nil)
                case .userCancelled:
                    completionHandler(failure("Purchase cancelled."), nil)
                case .pending:
                    completionHandler(failure("Purchase is pending. Full Access unlocks after the App Store confirms payment."), nil)
                @unknown default:
                    completionHandler(failure("App Store purchase did not complete."), nil)
                }
            } catch {
                completionHandler(failure("App Store purchase failed. \(error.localizedDescription)"), nil)
            }
        }
    }

    func refreshEntitlements(completionHandler: @escaping (FoundationResult?, Error?) -> Void) {
        Task {
            do {
                completionHandler(success(try await entitlementSnapshot()), nil)
            } catch {
                completionHandler(failure("Could not verify App Store access. \(error.localizedDescription)"), nil)
            }
        }
    }

    func restorePurchases(completionHandler: @escaping (FoundationResult?, Error?) -> Void) {
        Task {
            do {
                try await AppStore.sync()
                completionHandler(success(try await entitlementSnapshot()), nil)
            } catch {
                completionHandler(failure("Could not restore App Store purchases. \(error.localizedDescription)"), nil)
            }
        }
    }

    private func productDetails() async throws -> Product {
        let products = try await Product.products(for: [ProductIds.lifetime])
        guard let product = products.first else {
            throw StoreKitFullAccessError.productNotFound(ProductIds.lifetime)
        }
        return product
    }

    private func offer(for product: Product) -> FullAccessStoreOffer? {
        switch product.id {
        case ProductIds.lifetime:
            return FullAccessStoreOffer(
                title: "Lifetime",
                priceLabel: product.displayPrice,
                termsLabel: "One-time App Store purchase."
            )
        default:
            return nil
        }
    }

    private func entitlementSnapshot() async throws -> FullAccessEntitlementSnapshot {
        var lifetimeUnlocked = false
        var transactions = Dictionary(uniqueKeysWithValues: stagedDeliveries().map { ($0.id, $0) })
        for await entitlement in Transaction.currentEntitlements {
            let transaction = try checkVerified(entitlement)
            guard transaction.productID == ProductIds.lifetime else { continue }
            stageDelivery(transaction)
            transactions[transaction.id] = transaction
            if transaction.revocationDate == nil && !transaction.isUpgraded {
                lifetimeUnlocked = true
            }
        }
        return FullAccessEntitlementSnapshot(
            lifetimeUnlocked: lifetimeUnlocked,
            storeStatus: FullAccessStoreStatus.available,
            message: nil,
            deliveryToken: prepareDelivery(transactions.values.filter {
                lifetimeUnlocked || $0.revocationDate != nil
            })
        )
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .verified(let value):
            return value
        case .unverified(_, let error):
            throw error
        }
    }

    private func success(_ value: Any?) -> FoundationResult {
        FoundationPrimitivesKt.foundationSuccess(value: value)
    }

    private func failure(_ message: String) -> FoundationResult {
        FoundationPrimitivesKt.foundationFailure(error: FoundationErrorPlatform(message: message))
    }
}

private enum StoreKitFullAccessError: LocalizedError {
    case productNotFound(String)
    case deliveryExpired

    var errorDescription: String? {
        switch self {
        case .productNotFound(let productId):
            return "App Store product was not found: \(productId)"
        case .deliveryExpired:
            return "App Store delivery needs a fresh access check. Restore purchases to retry."
        }
    }
}
