package com.yjy.feature.startedchallenge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjy.common.core.extensions.restartableStateIn
import com.yjy.common.network.HttpStatusCodes.CONFLICT
import com.yjy.common.network.HttpStatusCodes.FORBIDDEN
import com.yjy.common.network.HttpStatusCodes.NOT_FOUND
import com.yjy.common.network.handleNetworkResult
import com.yjy.common.network.onFailure
import com.yjy.common.network.onSuccess
import com.yjy.data.challenge.api.ChallengePreferencesRepository
import com.yjy.data.challenge.api.StartedChallengeRepository
import com.yjy.domain.GetStartedChallengeDetailUseCase
import com.yjy.domain.ResetStartedChallengeUseCase
import com.yjy.feature.startedchallenge.model.ChallengeDetailUiState
import com.yjy.feature.startedchallenge.model.StartReasonsUiState
import com.yjy.feature.startedchallenge.model.StartedChallengeUiAction
import com.yjy.feature.startedchallenge.model.StartedChallengeUiEvent
import com.yjy.feature.startedchallenge.model.StartedChallengeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class StartedChallengeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getStartedChallengeDetail: GetStartedChallengeDetailUseCase,
    challengePreferencesRepository: ChallengePreferencesRepository,
    private val resetStartedChallengeUseCase: ResetStartedChallengeUseCase,
    private val startedChallengeRepository: StartedChallengeRepository,
) : ViewModel() {

    private val challengeId = savedStateHandle.getStateFlow<Int?>("challengeId", null)
    private val refreshReasonsTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _uiState = MutableStateFlow(StartedChallengeUiState())
    val uiState: StateFlow<StartedChallengeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<StartedChallengeUiEvent>()
    val uiEvent: Flow<StartedChallengeUiEvent> = _uiEvent.receiveAsFlow()

    val challengeDetail = merge(
        challengeId,
        challengePreferencesRepository.timeChangedFlow.map { challengeId.value },
    ).filterNotNull().flatMapLatest { id ->
        getStartedChallengeDetail(id)
    }.map { result ->
        handleNetworkResult(
            result = result,
            onSuccess = { challenge -> ChallengeDetailUiState.Success(challenge) },
            onHttpError = { code ->
                when (code) {
                    NOT_FOUND -> StartedChallengeUiEvent.LoadFailure.NotFound
                    CONFLICT -> StartedChallengeUiEvent.LoadFailure.NotStarted
                    FORBIDDEN -> StartedChallengeUiEvent.LoadFailure.NotParticipant
                    else -> StartedChallengeUiEvent.LoadFailure.Unknown
                }.also { sendEvent(it) }
                ChallengeDetailUiState.Loading
            },
            onNetworkError = {
                sendEvent(StartedChallengeUiEvent.LoadFailure.Network)
                ChallengeDetailUiState.Loading
            },
            onUnknownError = {
                sendEvent(StartedChallengeUiEvent.LoadFailure.Unknown)
                ChallengeDetailUiState.Loading
            },
        )
    }.restartableStateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ChallengeDetailUiState.Loading,
    )

    val startReasonsState = merge(
        challengeId,
        refreshReasonsTrigger.map { challengeId.value },
    ).filterNotNull().flatMapLatest { id ->
        flow {
            startedChallengeRepository.getStartReasons(id)
                .onSuccess {
                    _uiState.update { state -> state.copy(isEditingReasonToStart = false) }
                    emit(StartReasonsUiState.Success(it))
                }
                .onFailure {
                    sendEvent(StartedChallengeUiEvent.LoadFailure.Unknown)
                    emit(StartReasonsUiState.Loading)
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = StartReasonsUiState.Loading,
    )

    private fun refreshReasons() {
        refreshReasonsTrigger.tryEmit(Unit)
    }

    private fun sendEvent(event: StartedChallengeUiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    fun processAction(action: StartedChallengeUiAction) {
        when (action) {
            is StartedChallengeUiAction.OnResetClick ->
                resetRecord(action.challengeId, action.resetDateTime, action.memo)

            is StartedChallengeUiAction.OnAddReasonToStart ->
                addReasonToStart(action.challengeId, action.reasonToStart)

            is StartedChallengeUiAction.OnDeleteReasonToStart -> deleteReasonToStart(action.reasonId)
            is StartedChallengeUiAction.OnDeleteChallengeClick -> deleteChallenge(action.challengeId)
            is StartedChallengeUiAction.OnContinueChallengeClick -> continueChallenge(action.challengeId)
        }
    }

    private fun deleteChallenge(challengeId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isDeleting = true) }

        startedChallengeRepository.deleteStartedChallenge(challengeId)
            .onSuccess { sendEvent(StartedChallengeUiEvent.DeleteSuccess) }
            .onFailure { sendEvent(StartedChallengeUiEvent.DeleteFailure) }
        _uiState.update { it.copy(isDeleting = false) }
    }

    private fun continueChallenge(challengeId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isContinuing = true) }

        startedChallengeRepository.continueStartedChallenge(challengeId)
            .onSuccess {
                sendEvent(StartedChallengeUiEvent.ContinueSuccess)
                challengeDetail.restart()
            }
            .onFailure { sendEvent(StartedChallengeUiEvent.ContinueFailure) }
        _uiState.update { it.copy(isContinuing = false) }
    }

    private fun resetRecord(challengeId: Int, resetDateTime: LocalDateTime, memo: String) = viewModelScope.launch {
        _uiState.update { it.copy(isResetting = true) }

        resetStartedChallengeUseCase(challengeId, resetDateTime, memo)
            .onSuccess {
                sendEvent(StartedChallengeUiEvent.ResetSuccess)
                challengeDetail.restart()
            }
            .onFailure {
                sendEvent(StartedChallengeUiEvent.ResetFailure)
            }
        _uiState.update { it.copy(isResetting = false) }
    }

    private fun addReasonToStart(challengeId: Int, reasonToStart: String) = viewModelScope.launch {
        _uiState.update { it.copy(isEditingReasonToStart = true) }

        startedChallengeRepository.addReasonToStartChallenge(challengeId = challengeId, reason = reasonToStart)
            .onSuccess { refreshReasons() }
            .onFailure {
                sendEvent(StartedChallengeUiEvent.EditReasonToStartFailure)
                _uiState.update { it.copy(isEditingReasonToStart = false) }
            }
    }

    private fun deleteReasonToStart(reasonId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isEditingReasonToStart = true) }

        startedChallengeRepository.deleteReasonToStartChallenge(reasonId = reasonId)
            .onSuccess { refreshReasons() }
            .onFailure {
                sendEvent(StartedChallengeUiEvent.EditReasonToStartFailure)
                _uiState.update { it.copy(isEditingReasonToStart = false) }
            }
    }
}
