package com.yjy.data.network.response.challenge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetStartedChallengeDetailResponse(
    @SerialName("ROOM_IDX")
    val challengeId: Int,
    @SerialName("TITLE")
    val title: String,
    @SerialName("CONTENT")
    val description: String,
    @SerialName("CHALLENGETYPE")
    val category: String,
    @SerialName("ENDTIME")
    val targetDays: Int,
    @SerialName("CURRENTUSERNUM")
    val currentParticipantCount: Int,
    @SerialName("MAXUSERNUM")
    val maxParticipantCount: Int,
    @SerialName("RECENTSTARTTIME")
    val recentResetDateTime: String,
    @SerialName("STARTTIME")
    val startDateTime: String,
    @SerialName("ISFREEMODE")
    val isFreeMode: Boolean,
    @SerialName("ISCOMPLETE")
    val isCompleted: Boolean,
    @SerialName("ISPRIVATE")
    val isPrivate: Boolean,
    @SerialName("RANK")
    val rank: Int,
    @SerialName("SCORERANK")
    val scoreRank: Int,
)
