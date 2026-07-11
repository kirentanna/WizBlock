package com.wizblock.domain

import com.google.common.truth.Truth.assertThat
import com.wizblock.browser.UrlTextParser
import org.junit.Test

class UrlTextParserTest {

    @Test
    fun extractHost_url_success() {
        val host = UrlTextParser.extractHost("https://www.chess.com/play")

        assertThat(host).isEqualTo("chess.com")
    }

    @Test
    fun extractHost_plainDomain_success() {
        val host = UrlTextParser.extractHost("lichess.org")

        assertThat(host).isEqualTo("lichess.org")
    }

    @Test
    fun extractHost_invalid_null() {
        val host = UrlTextParser.extractHost("hello world")

        assertThat(host).isNull()
    }
}
