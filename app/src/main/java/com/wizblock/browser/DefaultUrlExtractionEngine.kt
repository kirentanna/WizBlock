package com.wizblock.browser

import android.view.accessibility.AccessibilityNodeInfo
import com.wizblock.domain.DomainNormalizer
import com.wizblock.domain.NormalizedDomainResult
import java.util.ArrayDeque

class DefaultUrlExtractionEngine(
    private val domainNormalizer: DomainNormalizer
) : UrlExtractionEngine {

    override fun extractHost(rootNode: AccessibilityNodeInfo?, packageName: String): ExtractionStatus {
        if (!BrowserHeuristics.isLikelyBrowser(packageName)) {
            return ExtractionStatus.UnsupportedBrowser
        }

        if (rootNode == null) {
            return ExtractionStatus.UrlNotFound
        }

        BrowserHeuristics.viewIdHints(packageName).forEach { viewId ->
            runCatching {
                rootNode.findAccessibilityNodeInfosByViewId(viewId)
            }.getOrNull()?.forEach { node ->
                candidateText(node)?.let { candidate ->
                    normalize(candidate)?.let { return ExtractionStatus.Success(it, candidate) }
                }
            }
        }

        collectAddressBarCandidates(rootNode).forEach { text ->
            normalize(text)?.let { return ExtractionStatus.Success(it, text) }
        }

        return ExtractionStatus.UrlNotFound
    }

    private fun collectAddressBarCandidates(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 280) {
            val node = queue.removeFirst()
            visited++

            if (isAddressBarLikeNode(node)) {
                candidateText(node)?.let { texts.add(it) }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return texts
    }

    private fun isAddressBarLikeNode(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName.orEmpty().lowercase()
        val className = node.className?.toString().orEmpty().lowercase()
        val hint = node.hintText?.toString().orEmpty().lowercase()
        val looksLikeAddressBar = viewId.contains("url") ||
            viewId.contains("address") ||
            viewId.contains("omnibox") ||
            viewId.contains("location") ||
            hint.contains("search or type web address")

        val inputLike = node.isEditable || className.contains("edittext")
        return looksLikeAddressBar || (inputLike && viewId.isNotBlank())
    }

    private fun candidateText(node: AccessibilityNodeInfo): String? {
        val fromText = node.text?.toString()
        if (!fromText.isNullOrBlank()) return fromText

        val fromDesc = node.contentDescription?.toString()
        if (!fromDesc.isNullOrBlank()) return fromDesc

        val fromHint = node.hintText?.toString()
        return fromHint
    }

    private fun normalize(input: String): String? {
        val host = UrlTextParser.extractHost(input) ?: return null
        return when (val normalized = domainNormalizer.normalize(host)) {
            is NormalizedDomainResult.Success -> normalized.domain
            is NormalizedDomainResult.Error -> null
        }
    }
}
