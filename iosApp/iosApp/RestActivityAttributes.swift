import ActivityKit
import Foundation

struct RestActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        let startedAt: Date
        let endsAt: Date
    }

}
