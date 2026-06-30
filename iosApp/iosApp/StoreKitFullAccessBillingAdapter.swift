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

    override init() {
        super.init()
        transactionUpdates = Task.detached {
            for await update in Transaction.updates {
                if case .verified(let transaction) = update {
                    await transaction.finish()
                }
            }
        }
    }

    deinit {
        transactionUpdates?.cancel()
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
                    await transaction.finish()
                    completionHandler(success(await entitlementSnapshot()), nil)
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
            completionHandler(success(await entitlementSnapshot()), nil)
        }
    }

    func restorePurchases(completionHandler: @escaping (FoundationResult?, Error?) -> Void) {
        Task {
            do {
                try await AppStore.sync()
                completionHandler(success(await entitlementSnapshot()), nil)
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

    private func entitlementSnapshot() async -> FullAccessEntitlementSnapshot {
        var lifetimeUnlocked = false
        for await entitlement in Transaction.currentEntitlements {
            guard case .verified(let transaction) = entitlement else {
                continue
            }
            switch transaction.productID {
            case ProductIds.lifetime:
                lifetimeUnlocked = true
            default:
                break
            }
        }
        return FullAccessEntitlementSnapshot(
            lifetimeUnlocked: lifetimeUnlocked,
            storeStatus: FullAccessStoreStatus.available,
            message: nil
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

    var errorDescription: String? {
        switch self {
        case .productNotFound(let productId):
            return "App Store product was not found: \(productId)"
        }
    }
}
