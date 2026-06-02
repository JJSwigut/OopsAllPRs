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
        return IosAppViewControllerFactory().create(developerToolsEnabled: developerToolsEnabled)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
