import ActivityKit
import SwiftUI
import WidgetKit

@main
struct RestTimerActivityBundle: WidgetBundle {
    var body: some Widget {
        RestTimerLiveActivity()
    }
}

struct RestTimerLiveActivity: Widget {
    private let workoutURL = URL(string: "oopsallprs://active-workout")

    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RestActivityAttributes.self) { context in
            HStack(spacing: 12) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.title2)
                    .foregroundStyle(.mint)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Rest")
                        .font(.headline)
                    Text("Next set")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                CountdownText(state: context.state, isStale: context.isStale)
                    .font(.title2.weight(.semibold))
            }
            .padding(.horizontal, 16)
            .activityBackgroundTint(Color.black.opacity(0.88))
            .activitySystemActionForegroundColor(.white)
            .widgetURL(workoutURL)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Rest timer, next set")
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label("Rest", systemImage: "figure.strengthtraining.traditional")
                        .font(.headline)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    CountdownText(state: context.state, isStale: context.isStale)
                        .font(.headline)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("Next set")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            } compactLeading: {
                Image(systemName: "figure.strengthtraining.traditional")
                    .foregroundStyle(.mint)
                    .accessibilityLabel("Rest timer")
            } compactTrailing: {
                CountdownText(state: context.state, isStale: context.isStale)
                    .font(.caption.weight(.semibold))
            } minimal: {
                Image(systemName: "timer")
                    .foregroundStyle(.mint)
                    .accessibilityLabel("Rest timer")
            }
            .widgetURL(workoutURL)
            .keylineTint(.mint)
        }
    }
}

private struct CountdownText: View {
    let state: RestActivityAttributes.ContentState
    let isStale: Bool

    @ViewBuilder
    var body: some View {
        if isStale {
            Text("Ready")
                .accessibilityLabel("Rest complete")
        } else {
            Text(timerInterval: state.startedAt...state.endsAt, countsDown: true)
                .monospacedDigit()
                .multilineTextAlignment(.trailing)
                .accessibilityLabel("Rest time remaining")
        }
    }
}
