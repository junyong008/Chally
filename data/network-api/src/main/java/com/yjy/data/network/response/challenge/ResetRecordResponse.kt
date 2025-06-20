package com.yjy.data.network.response.challenge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResetRecordResponse(
    @SerialName("ROOMMEMRESET_IDX")
    val resetRecordId: Int,
    @SerialName("RESETDATE")
    val resetDateTime: String,
    @SerialName("ABSTINENCETIME")
    val recordInSeconds: Long,
    @SerialName("RESETMEMO")
    val content: String,
)
