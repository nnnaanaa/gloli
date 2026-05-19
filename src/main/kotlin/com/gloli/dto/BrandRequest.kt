package com.gloli.dto

import jakarta.validation.constraints.NotBlank

/** ブランド作成・更新リクエスト */
data class BrandRequest(
    @field:NotBlank(message = "Brand name is required")
    val name: String,
    /** 省略可能。指定するとスクレイパーのドメインマッチングで活用される */
    val url: String? = null
)
