# Competitor Gap Backlog

Research sources:

- Hevy rest timers and feature list: https://help.hevyapp.com/hc/en-us/articles/35385404949143-Rest-Timer-Default-Rest-Timer-How-to-Add-Adjust-Volume-and-Sound and https://www.hevyapp.com/features/
- Strong rest timer and app listing: https://help.strongapp.io/article/231-rest-timer and https://apps.apple.com/us/app/strong-workout-tracker-gym-log/id464254577
- FitNotes workout tools, tracking, routines, and settings: https://www.fitnotesapp.com/workout_tools/, https://www.fitnotesapp.com/workout_tracking/, https://www.fitnotesapp.com/routines/, and https://www.getfitnotes.com/docs/settings.html

## Recommended Sequence

1. **Routine-Aware Rest Timers**: Current spec. Per-exercise rest in routines and active workouts, auto-start after logging, adjust/skip, restart recovery, sound/haptic alert.
2. **Previous Workout Values**: Pre-fill next workout/routine launch from last performed values while preserving current planned templates.
3. **Set Types And RPE/RIR**: Warm-up, working, drop, failure, and optional effort tracking; unlock warm-up-specific rest later.
4. **Supersets And Circuits**: Group exercises and advance focus through the group quickly after each logged set.
5. **Set Notes**: Add short notes per set and exercise notes for durable cues like machine setup, form reminders, spotter help, or rest observations.
6. **Plate Calculator**: Fast loading math for barbell movements, with remembered bar weight and unit handling.
7. **Warm-Up Calculator**: Suggest warm-up sets based on working weight and target reps.
8. **Body Measurements And Progress Photos**: Local bodyweight/body measurements first; photos only after privacy/export decisions are explicit.
9. **Exercise Library Richness**: Better filters, recently used, instructions, aliases, and user-created cleanup/merge behavior.

## Deferred Deliberately

- Social feed, leaderboards, coach assignment, public sharing, and Strava-style integrations are not aligned with the current local-only/offline-first product direction.
- Cloud sync remains future architecture work and should not block local rest timers.
- Apple Watch and Android wearable logging are useful later but should follow the stable active workout loop.
