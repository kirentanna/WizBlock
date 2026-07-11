package com.wizblock.domain

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.CategoryPack
import org.junit.Test

class CategoryMatcherTest {
    private val matcher = CategoryMatcher(
        listOf(
            CategoryPack(
                id = "games",
                name = "Games",
                description = "Game sites",
                domains = listOf("lichess.org")
            )
        )
    )

    @Test
    fun match_enabledCategoryBlocksSubdomain() {
        val result = matcher.match("play.lichess.org", "https://play.lichess.org", setOf("games"))

        assertThat(result?.categoryId).isEqualTo("games")
        assertThat(result?.targetValue).isEqualTo("play.lichess.org")
    }

    @Test
    fun match_disabledCategoryDoesNotBlock() {
        val result = matcher.match("lichess.org", "https://lichess.org", emptySet())

        assertThat(result).isNull()
    }
}
