package com.mortlace.dto

import jakarta.validation.constraints.NotBlank

data class BrandRequest(
    @field:NotBlank(message = "ブランド名は必須です")
    val name: String,
    val url: String? = null
)
