package com.gloli.dto

import jakarta.validation.constraints.NotBlank

data class BrandRequest(
    @field:NotBlank(message = "Brand name is required")
    val name: String,
    val url: String? = null
)
