package com.gloli.dto

import jakarta.validation.constraints.NotBlank

/** カテゴリ作成・更新リクエスト */
data class CategoryRequest(
    @field:NotBlank(message = "Category name is required")
    val name: String
)
