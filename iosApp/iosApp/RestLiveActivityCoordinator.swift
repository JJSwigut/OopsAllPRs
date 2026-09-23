import ActivityKit
import Foundation
import shared

final class RestLiveActivityCoordinator: NSObject, RestAlertScheduler {
    private let operationLock = NSLock()
    private var operationTail: Task<Void, Never>?
    private var expiryTask: Task<Void, Never>?

    func schedule(
        restEndsAt: Kotlinx_datetimeInstant,
        soundEnabled: Bool,
        persistentSurfaceEnabled: Bool
    ) -> RestAlertScheduleResult {
        _ = soundEnabled
        let endsAt = Date(timeIntervalSince1970: Double(restEndsAt.toEpochMilliseconds()) / 1_000)
        guard persistentSurfaceEnabled, endsAt > Date() else {
            cancel()
            return .scheduled
        }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            cancel()
            return .permissionDenied
        }

        let state = RestActivityAttributes.ContentState(startedAt: Date(), endsAt: endsAt)
        let content = ActivityContent(state: state, staleDate: endsAt, relevanceScore: 100)

        cancelExpiryTask()
        enqueueOperation {
            await Self.reconcileActivity(content: content)
        }
        scheduleExpiry(at: endsAt, content: content)
        return .scheduled
    }

    func cancel() {
        cancelExpiryTask()
        enqueueOperation {
            await Self.endAllActivities(content: nil)
        }
    }

    private func enqueueOperation(_ operation: @escaping () async -> Void) {
        operationLock.lock()
        let previous = operationTail
        let next = Task {
            await previous?.value
            await operation()
        }
        operationTail = next
        operationLock.unlock()
    }

    private func cancelExpiryTask() {
        operationLock.lock()
        let task = expiryTask
        expiryTask = nil
        operationLock.unlock()
        task?.cancel()
    }

    private func scheduleExpiry(
        at endsAt: Date,
        content: ActivityContent<RestActivityAttributes.ContentState>
    ) {
        let delay = max(0, endsAt.timeIntervalSinceNow)
        let task = Task { [weak self] in
            do {
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            } catch {
                return
            }
            guard !Task.isCancelled else { return }
            self?.enqueueOperation {
                await Self.endAllActivities(content: content)
            }
        }
        operationLock.lock()
        expiryTask = task
        operationLock.unlock()
    }

    private static func reconcileActivity(
        content: ActivityContent<RestActivityAttributes.ContentState>
    ) async {
        let activities = Activity<RestActivityAttributes>.activities
        if let current = activities.first {
            await current.update(content)
            for duplicate in activities.dropFirst() {
                await duplicate.end(content, dismissalPolicy: .immediate)
            }
            return
        }

        _ = try? Activity.request(
            attributes: RestActivityAttributes(),
            content: content,
            pushType: nil
        )
    }

    private static func endAllActivities(
        content: ActivityContent<RestActivityAttributes.ContentState>?
    ) async {
        for activity in Activity<RestActivityAttributes>.activities {
            await activity.end(content, dismissalPolicy: .immediate)
        }
    }
}
