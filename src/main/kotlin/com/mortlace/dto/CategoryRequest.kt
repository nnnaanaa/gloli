package com.mortlace.dto

import jakarta.validation.constraints.NotBlank

data class CategoryRequest(
    @field:NotBlank(message = "カテゴリー名は必須です")
    val name: String
)
