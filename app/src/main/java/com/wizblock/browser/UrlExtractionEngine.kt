package com.wizblock.browser

import android.view.accessibility.AccessibilityNodeInfo

interface UrlExtractionEngine {
    fun extractHost(rootNode: AccessibilityNodeInfo?, packageName: String): ExtractionStatus
}
