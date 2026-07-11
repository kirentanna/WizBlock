package com.wizblock.browser

sealed interface ExtractionStatus {
    data class Success(
        val host: String,
        val addressText: String
    ) : ExtractionStatus
    data object UnsupportedBrowser : ExtractionStatus
    data object UrlNotFound : ExtractionStatus
}
