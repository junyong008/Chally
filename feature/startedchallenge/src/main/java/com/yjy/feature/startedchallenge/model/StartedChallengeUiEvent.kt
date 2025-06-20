package com.yjy.feature.startedchallenge.model

sealed interface StartedChallengeUiEvent {
    data object ResetSuccess : StartedChallengeUiEvent
    data object ResetFailure : StartedChallengeUiEvent
    data object DeleteSuccess : StartedChallengeUiEvent
    data object DeleteFailure : StartedChallengeUiEvent
    data object ContinueSuccess : StartedChallengeUiEvent
    data object ContinueFailure : StartedChallengeUiEvent
    data object EditReasonToStartFailure : StartedChallengeUiEvent

    sealed class LoadFailure : StartedChallengeUiEvent {
        data object NotFound : LoadFailure()
        data object NotStarted : LoadFailure()
        data object NotParticipant : LoadFailure()
        data object Network : LoadFailure()
        data object Unknown : LoadFailure()
    }
}
