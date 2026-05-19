package com.gloli.dto

import com.gloli.domain.enums.Priority
import com.gloli.domain.enums.Status
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/** ウィッシュリストアイテム作成・更新リクエスト */
data class WishlistItemRequest(
    val name: String? = null,

    @field:NotBlank(message = "URL is required")
    @field:Size(max = 2048, message = "URL is too long")
    val url: String,

    val price: BigDecimal? = null,
    val brandId: Long? = null,
    val categoryId: Long? = null,
    val notes: String? = null,
    val priority: Priority = Priority.MEDIUM,
    /** 外部画像URL。指定するとローカル保存画像は削除される */
    val imageUrl: String? = null,
    val status: Status = Status.WANTED
)
