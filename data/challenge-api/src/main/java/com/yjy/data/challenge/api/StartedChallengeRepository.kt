package com.yjy.data.challenge.api

import com.yjy.common.network.NetworkResult
import com.yjy.model.challenge.ChallengeRank
import com.yjy.model.challenge.DetailedStartedChallenge
import com.yjy.model.challenge.RecoveryProgress
import com.yjy.model.challenge.ResetInfo
import com.yjy.model.challenge.SimpleStartedChallenge
import com.yjy.model.challenge.StartReason
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface StartedChallengeRepository {
    val startedChallenges: Flow<List<SimpleStartedChallenge>>
    val completedChallenges: Flow<List<SimpleStartedChallenge>>
    suspend fun resetStartedChallenge(challengeId: Int, resetDateTime: LocalDateTime, memo: String): NetworkResult<Unit>
    suspend fun addReasonToStartChallenge(challengeId: Int, reason: String): NetworkResult<Unit>
    suspend fun deleteReasonToStartChallenge(reasonId: Int): NetworkResult<Unit>
    suspend fun deleteStartedChallenge(challengeId: Int): NetworkResult<Unit>
    suspend fun continueStartedChallenge(challengeId: Int): NetworkResult<Unit>
    suspend fun forceRemoveStartedChallengeMember(memberId: Int): NetworkResult<Unit>
    suspend fun getStartedChallengeDetail(challengeId: Int): Flow<NetworkResult<DetailedStartedChallenge>>
    suspend fun getResetInfo(challengeId: Int): Flow<NetworkResult<ResetInfo>>
    suspend fun getStartReasons(challengeId: Int): NetworkResult<List<StartReason>>
    suspend fun getChallengeProgress(challengeId: Int): NetworkResult<RecoveryProgress>
    suspend fun getChallengeRanking(challengeId: Int): Flow<NetworkResult<List<ChallengeRank>>>
}
