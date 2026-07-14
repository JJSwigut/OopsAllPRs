package com.jjswigut.oopsallprs.di

import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.UserExerciseConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.usecase.ExerciseLoggingConfigurationUseCases

internal data class ExerciseLoggingComposition(
    val management: ExerciseLoggingConfigurationUseCases?,
    val configurations: LoggingConfigurationRepository?
)

internal fun createExerciseLoggingComposition(
    repositoryCapabilities: Any,
    exercises: ExerciseRepository,
    workouts: WorkoutRepository,
    activeUx: ActiveWorkoutUxRepository
): ExerciseLoggingComposition {
    val configurations = repositoryCapabilities as? LoggingConfigurationRepository
    val userConfigurations = repositoryCapabilities as? UserExerciseConfigurationRepository
    val management = if (configurations != null && userConfigurations != null) {
        ExerciseLoggingConfigurationUseCases(
            exercises = exercises,
            configurations = configurations,
            userConfigurations = userConfigurations,
            workouts = workouts,
            activeUx = activeUx
        )
    } else {
        null
    }
    return ExerciseLoggingComposition(management, configurations)
}
