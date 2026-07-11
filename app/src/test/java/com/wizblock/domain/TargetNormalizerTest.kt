package com.wizblock.domain

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType
import org.junit.Test

class TargetNormalizerTest {
    private val normalizer = TargetNormalizer(DefaultDomainNormalizer())

    @Test
    fun normalize_domainAcceptsUrlAndLowercasesHost() {
        val result = normalizer.normalize(TargetType.DOMAIN, " https://WWW.Example.COM/path?q=1 ")

        assertThat(result).isEqualTo(TargetKey(TargetType.DOMAIN, "example.com"))
    }

    @Test
    fun normalize_appPackagePreservesCaseAndTrims() {
        val result = normalizer.normalize(TargetType.APP_PACKAGE, " COM.Instagram.Android ")

        assertThat(result).isEqualTo(TargetKey(TargetType.APP_PACKAGE, "COM.Instagram.Android"))
        assertThat(result?.id).isEqualTo("APP_PACKAGE|com.instagram.android")
    }

    @Test
    fun normalize_keywordLowercasesAndTrims() {
        val result = normalizer.normalize(TargetType.KEYWORD, " Sports News ")

        assertThat(result).isEqualTo(TargetKey(TargetType.KEYWORD, "sports news"))
    }

    @Test
    fun normalize_blankValueReturnsNull() {
        val result = normalizer.normalize(TargetType.KEYWORD, "   ")

        assertThat(result).isNull()
    }
}
