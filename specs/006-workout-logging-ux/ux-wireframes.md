# Workout Logging UX V1 Wireframes

Purpose: capture the intended product shape before implementation planning. These are behavioral wireframes, not final visual polish.

## UX Principles

- The active workout is the app's primary mode while a workout is open.
- Every screen should answer "what do I do next?" at a glance.
- Use density for repeated logging surfaces and save visual emphasis for the next action.
- Do not stack cards inside cards in the logging loop.
- Bodyweight starts as reps-only; added load is optional and hidden from the default path.
- PR feedback should feel like a reward, not a modal interruption.

## Train

```text
+------------------------------+
|                              |
| [ Start workout            ] |
|                              |
| Active workout               |  only if one exists
| Bench + 2 more     24m       |
| [ Resume                  ]  |
|                              |
| Recent templates             |  later slice can populate this
| Push day                     |
| Legs                         |
|                              |
| bottom nav                   |
+------------------------------+
```

Notes:
- No app title on this screen.
- If no active workout exists, Start workout is the first meaningful object.
- Routine/template entry is secondary until templates are implemented.

## Active Workout Empty

```text
+------------------------------+
|                     Close    |
| Workout           0:00       |
|                              |
| [ Add exercise             ] |
|                              |
| No exercises yet             |
| Add an exercise to start.    |
+------------------------------+
```

Notes:
- Full-screen focused mode.
- Top-level navigation is hidden while logging.
- Close is a compact text action, not a large glass button.

## Active Workout With Exercise

```text
+------------------------------+
|                     Close    |
| Workout           18:42      |
| [ Add exercise             ] |
|                              |
| Bench Press                  |
| Weighted                     |
| 1  10 reps @ 60 kg           |
| 2  10 reps @ 60 kg      PR   |
|                              |
| Next set                     |
| Reps        Weight           |
| [-] 10 [+]  [-] 60 kg [+]    |
| [ Log set                  ] |
|                              |
| 3/4 Sit-Up                   |
| Bodyweight                   |
| Next set                     |
| Reps                         |
| [-] 12 [+]                   |
| [ Log set                  ] |
+------------------------------+
```

Notes:
- Completed sets are rows, not nested cards.
- Next set stays inside the exercise block and uses the last logged set as the default.
- Bodyweight next set omits weight by default.

## Add Exercise Picker

```text
+------------------------------+
|                    Cancel    |
| [ Search exercises         ] |
|                              |
| Bench Press       Chest      |
| Weighted             [ Add ] |
|                              |
| Incline Bench     Chest      |
| Weighted             [ Add ] |
|                              |
| Push-Up           Chest      |
| Bodyweight          [ Add ]  |
|                              |
| [ Create custom exercise   ] |
+------------------------------+
```

Notes:
- Search is the main header; no separate title needed.
- Rows are dense and one-tap.
- Keyboard must not hide the search field or the currently visible result action.

## Set Logging States

```text
Ready:
[-] 10 [+]  [-] 60 kg [+]  [ Log set ]

Pending:
[-] 10 [+]  [-] 60 kg [+]  [ Logging... ]

Failure:
[-] 10 [+]  [-] 60 kg [+]  [ Retry ]
Could not log. Nothing was saved.

Success:
2  10 reps @ 60 kg  PR
Next set defaults to 10 reps @ 60 kg
```

Notes:
- A set is only shown in the completed list after successful confirmation.
- Failure keeps the draft intact.
- PR marker attaches to the completed row.

## Manual Review Targets

- Pixel-class phone portrait.
- Keyboard visible in exercise search.
- Weighted exercise with multiple completed sets.
- Bodyweight exercise with reps-only sets.
- Long exercise names.
- Restart while active workout has at least one logged set and one draft.
