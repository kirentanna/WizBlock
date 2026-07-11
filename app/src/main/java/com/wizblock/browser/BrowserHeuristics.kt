package com.wizblock.browser

object BrowserHeuristics {
    private val knownPrefixes = listOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.brave.browser",
        "com.opera.browser",
        "com.duckduckgo.mobile.android",
        "org.chromium.chrome"
    )

    private val packageTokens = listOf("browser", "chrome", "firefox", "edge", "opera", "brave", "internet")

    fun isLikelyBrowser(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (knownPrefixes.any { packageName.startsWith(it) }) return true
        return packageTokens.any { packageName.contains(it, ignoreCase = true) }
    }

    fun viewIdHints(packageName: String): List<String> {
        return when {
            packageName.startsWith("com.android.chrome") -> listOf("com.android.chrome:id/url_bar")
            packageName.startsWith("org.mozilla.firefox") -> listOf(
                "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                "org.mozilla.firefox:id/url_bar_title",
                "org.mozilla.firefox:id/mozac_browser_toolbar_edit_url_view"
            )
            packageName.startsWith("com.microsoft.emmx") -> listOf("com.microsoft.emmx:id/url_bar")
            packageName.startsWith("com.sec.android.app.sbrowser") -> listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text")
            packageName.startsWith("com.brave.browser") -> listOf("com.brave.browser:id/url_bar")
            packageName.startsWith("com.opera.browser") -> listOf("com.opera.browser:id/url_field")
            else -> emptyList()
        }
    }
}
