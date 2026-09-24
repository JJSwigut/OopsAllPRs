import ActivityKit
import Foundation
import UserNotifications
import shared

final class RestLiveActivityCoordinator: NSObject, RestAlertScheduler, UNUserNotificationCenterDelegate {
    private let activeWorkoutOpenRequest: ActiveWorkoutOpenRequest
    private let operationLock = NSLock()
    private var operationTail: Task<Void, Never>?
    private var expiryTask: Task<Void, Never>?
    private var notificationVersion = 0

    init(activeWorkoutOpenRequest: ActiveWorkoutOpenRequest) {
        self.activeWorkoutOpenRequest = activeWorkoutOpenRequest
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    func schedule(
        restEndsAt: Kotlinx_datetimeInstant,
        soundEnabled: Bool,
        persistentSurfaceEnabled: Bool
    ) -> RestAlertScheduleResult {
        let endsAt = Date(timeIntervalSince1970: Double(restEndsAt.toEpochMilliseconds()) / 1_000)
        scheduleCompletionNotification(at: endsAt, soundEnabled: soundEnabled)
        guard persistentSurfaceEnabled, endsAt > Date() else {
            endLiveActivity()
            return .scheduled
        }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            endLiveActivity()
            return .scheduled
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
        operationLock.lock()
        notificationVersion += 1
        operationLock.unlock()
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [Self.completionNotificationId])
        endLiveActivity()
    }

    private func endLiveActivity() {
        cancelExpiryTask()
        enqueueOperation {
            await Self.endAllActivities(content: nil)
        }
    }

    private func scheduleCompletionNotification(at endsAt: Date, soundEnabled: Bool) {
        let center = UNUserNotificationCenter.current()
        operationLock.lock()
        notificationVersion += 1
        let version = notificationVersion
        operationLock.unlock()
        center.removePendingNotificationRequests(withIdentifiers: [Self.completionNotificationId])
        guard endsAt > Date() else { return }
        center.requestAuthorization(options: [.alert, .sound]) { [weak self] granted, _ in
            guard let self, granted else { return }
            self.operationLock.lock()
            let isCurrent = self.notificationVersion == version
            self.operationLock.unlock()
            guard isCurrent else { return }
            let content = UNMutableNotificationContent()
            content.title = "Rest complete"
            content.body = "Time for the next set."
            if soundEnabled { content.sound = .default }
            let delay = max(1, endsAt.timeIntervalSinceNow)
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)
            center.add(UNNotificationRequest(identifier: Self.completionNotificationId, content: content, trigger: trigger))
        }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if response.notification.request.identifier == Self.completionNotificationId {
            activeWorkoutOpenRequest.request()
        }
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    private static let completionNotificationId = "rest-complete"

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
