package com.wizblock.model

data class DailyBlockSummary(
    val total: Int,
    val appBlocks: Int,
    val domainBlocks: Int,
    val keywordBlocks: Int
)
