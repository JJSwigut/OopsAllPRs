# Workout Tracking

This context describes exercises, workout plans, and the facts recorded while a person trains.

## Language

**Exercise Definition**:
The durable description of a movement available for selection. Seeded and user-created definitions have the same capabilities.
_Avoid_: Exercise type, catalog row, premade exercise

**Logging Configuration**:
An immutable description of which measures and observed effort a set may capture. Its identity preserves the meaning of historical performances.
_Avoid_: Logging mode, exercise mode, input layout

**Definition Default**:
The logging configuration supplied by an exercise definition before personal or workout-specific choices are applied.
_Avoid_: Global default, hard-coded mode

**User Exercise Configuration**:
A person's preferred full logging configuration for one exercise definition.
_Avoid_: Exercise override, preference patch

**Workout Override**:
A logging configuration selected for one exercise in one workout without changing its definition, saved routines, or history.
_Avoid_: Temporary exercise type, session default

**Resolved Logging Configuration**:
The configuration selected for an exercise after applying workout override, user exercise configuration, and definition default precedence.
_Avoid_: Effective mode, merged settings

**Exercise Snapshot**:
The exercise identity, revision, display facts, and resolved logging configuration preserved by a routine or workout.
_Avoid_: Exercise copy, cached exercise

**Set Performance**:
The observed measures and effort recorded for one performed set, together with the logging configuration that defined their meaning.
_Avoid_: Set input, result row

**Observed Effort**:
Optional evidence recorded after a set, such as RPE, reps in reserve, or whether failure was reached.
_Avoid_: Effort target, intensity setting

**Effort Target**:
The intended effort prescribed by a routine for a future set.
_Avoid_: Observed effort, achieved effort

**Load Role**:
The meaning of a recorded load relative to the movement, such as external resistance, assistance, or load added to bodyweight.
_Avoid_: Weight type, positive or negative weight

**Added Load**:
Positive external load added to a bodyweight movement. Zero or no value means the set was unloaded.
_Avoid_: Weighted bodyweight mode, negative assistance

**Assistance Load**:
Positive assistance applied to make a movement easier. It is distinct from added load and is never represented as a negative value.
_Avoid_: Negative weight, subtracted load

**Exercise Definition Revision**:
The identity of a specific definition state used to detect and control later updates.
_Avoid_: Schema version, edit count

**Record Derivation Version**:
The rule-set version that produced a progress point or personal record from set performances.
_Avoid_: Database version, record revision

**Progress Evidence**:
The recorded sets or completed sessions that support a progress reading. An estimated strength value, work performed, and a completed session are distinct evidence, not interchangeable measures.
_Avoid_: Overall score, proof of muscle growth

**Work Performed**:
The sum of external load multiplied by repetitions for an exercise in a session. It describes recorded training work, not a direct measure of strength or physiological adaptation.
_Avoid_: Strength gained, calories burned
