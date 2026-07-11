package com.wizblock.domain

interface DomainNormalizer {
    fun normalize(input: String): NormalizedDomainResult
}
