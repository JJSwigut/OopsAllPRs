import SwiftUI
import UIKit
import shared

struct ContentView: View {
    var body: some View {
        SharedAppView()
            .ignoresSafeArea()
    }
}

struct SharedAppView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let developerToolsEnabled: Bool
        #if DEBUG
        developerToolsEnabled = true
        #else
        developerToolsEnabled = false
        #endif
        let container = UIViewController()
        let sharedController = IosAppViewControllerFactory().create(
            context: container,
            developerToolsEnabled: developerToolsEnabled
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
