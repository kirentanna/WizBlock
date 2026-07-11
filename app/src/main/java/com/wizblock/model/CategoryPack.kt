package com.wizblock.model

data class CategoryPack(
    val id: String,
    val name: String,
    val description: String,
    val domains: List<String>
)
