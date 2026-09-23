import StoreKit
import StoreKitTest
import XCTest

@available(iOS 15.0, *)
final class FullAccessStoreKitTests: XCTestCase {
    private static let productID = "lifetime_unlock"
    private var session: SKTestSession!

    override func setUpWithError() throws {
        let configurationURL = try XCTUnwrap(
            Bundle(for: Self.self).url(forResource: "FullAccess", withExtension: "storekit")
        )
        session = try SKTestSession(contentsOf: configurationURL)
        session.disableDialogs = true
        session.resetToDefaultState()
        session.clearTransactions()
    }

    override func tearDownWithError() throws {
        session.resetToDefaultState()
        session = nil
    }

    func testLifetimeUnlockPurchaseCreatesCurrentEntitlement() async throws {
        let transaction = try await session.buyProduct(identifier: Self.productID)
        XCTAssertEqual(transaction.productID, Self.productID)
        XCTAssertNil(transaction.revocationDate)

        let testTransaction = try XCTUnwrap(session.allTransactions().first)
        XCTAssertEqual(testTransaction.productIdentifier, Self.productID)

        try await assertCurrentEntitlements([Self.productID])
    }

    func testLifetimeUnlockRemainsEntitledInFreshStoreKitSession() async throws {
        _ = try await session.buyProduct(identifier: Self.productID)
        try await assertCurrentEntitlements([Self.productID])

        let configurationURL = try XCTUnwrap(
            Bundle(for: Self.self).url(forResource: "FullAccess", withExtension: "storekit")
        )
        let freshSession = try SKTestSession(contentsOf: configurationURL)
        freshSession.disableDialogs = true

        try await assertCurrentEntitlements([Self.productID])
    }

    func testRefundedLifetimeUnlockIsNoLongerEntitled() async throws {
        let transaction = try await session.buyProduct(identifier: Self.productID)
        try await assertCurrentEntitlements([Self.productID])

        try session.refundTransaction(identifier: UInt(transaction.id))

        try await assertCurrentEntitlements([])
    }

    private func assertCurrentEntitlements(
        _ expectedProductIDs: [String],
        timeoutNanoseconds: UInt64 = 3_000_000_000
    ) async throws {
        let deadline = DispatchTime.now().uptimeNanoseconds + timeoutNanoseconds
        var actualProductIDs = try await currentEntitlementProductIDs()
        while actualProductIDs != expectedProductIDs && DispatchTime.now().uptimeNanoseconds < deadline {
            try await Task.sleep(nanoseconds: 100_000_000)
            actualProductIDs = try await currentEntitlementProductIDs()
        }
        XCTAssertEqual(actualProductIDs, expectedProductIDs)
    }

    private func currentEntitlementProductIDs() async throws -> [String] {
        var entitlementProductIDs: [String] = []
        for await result in Transaction.currentEntitlements {
            let entitlement = try verifiedTransaction(from: result)
            entitlementProductIDs.append(entitlement.productID)
        }
        return entitlementProductIDs.sorted()
    }

    private func verifiedTransaction(
        from result: VerificationResult<Transaction>
    ) throws -> Transaction {
        switch result {
        case .verified(let transaction):
            return transaction
        case .unverified(_, let error):
            throw error
        }
    }
}
