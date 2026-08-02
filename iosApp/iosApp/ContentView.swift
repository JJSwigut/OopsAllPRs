import SwiftUI
import UIKit
import shared

struct ContentView: View {
    private let activeWorkoutOpenRequest = ActiveWorkoutOpenRequest()

    var body: some View {
        SharedAppView(activeWorkoutOpenRequest: activeWorkoutOpenRequest)
            .ignoresSafeArea()
            .onOpenURL { url in
                guard url.scheme == "oopsallprs", url.host == "active-workout" else { return }
                activeWorkoutOpenRequest.request()
            }
    }
}

struct SharedAppView: UIViewControllerRepresentable {
    let activeWorkoutOpenRequest: ActiveWorkoutOpenRequest

    func makeUIViewController(context: Context) -> UIViewController {
        let developerToolsEnabled: Bool
        #if DEBUG
        developerToolsEnabled = true
        #else
        developerToolsEnabled = false
        #endif
        let container = UIViewController()
        let billingAdapter: FullAccessBillingAdapter?
        if #available(iOS 15.0, *) {
            billingAdapter = StoreKitFullAccessBillingAdapter()
        } else {
            billingAdapter = nil
        }
        let restAlertScheduler = RestLiveActivityCoordinator()
        let sharedController = IosAppViewControllerFactory().create(
            context: container,
            developerToolsEnabled: developerToolsEnabled,
            fullAccessBilling: billingAdapter,
            restAlertScheduler: restAlertScheduler,
            activeWorkoutOpenRequest: activeWorkoutOpenRequest
        )
        container.addChild(sharedController)
        container.view.addSubview(sharedController.view)
        sharedController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            sharedController.view.leadingAnchor.constraint(equalTo: container.view.leadingAnchor),
            sharedController.view.trailingAnchor.constraint(equalTo: container.view.trailingAnchor),
            sharedController.view.topAnchor.constraint(equalTo: container.view.topAnchor),
            sharedController.view.bottomAnchor.constraint(equalTo: container.view.bottomAnchor)
        ])
        sharedController.didMove(toParent: container)
        return container
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
