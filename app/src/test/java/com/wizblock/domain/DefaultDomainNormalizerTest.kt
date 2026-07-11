package com.wizblock.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultDomainNormalizerTest {

    private val normalizer = DefaultDomainNormalizer()

    @Test
    fun normalize_plainDomain_success() {
        val result = normalizer.normalize("Chess.com")

        assertThat(result).isEqualTo(NormalizedDomainResult.Success("chess.com"))
    }

    @Test
    fun normalize_fullUrl_success() {
        val result = normalizer.normalize("https://www.lichess.org/play")

        assertThat(result).isEqualTo(NormalizedDomainResult.Success("lichess.org"))
    }

    @Test
    fun normalize_idn_success() {
        val result = normalizer.normalize("https://münich.com")

        assertThat(result).isEqualTo(NormalizedDomainResult.Success("xn--mnich-kva.com"))
    }

    @Test
    fun normalize_invalidHost_error() {
        val result = normalizer.normalize("not a domain")

        assertThat(result).isEqualTo(NormalizedDomainResult.Error(ErrorReason.InvalidHost))
    }
}
