package com.mortlace.dto

import com.mortlace.domain.enums.Priority
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class WishlistItemRequest(
    val name: String? = null,

    @field:NotBlank(message = "URLは必須です")
    @field:Size(max = 2048, message = "URLが長すぎます")
    val url: String,

    val price: BigDecimal? = null,
    val brandId: Long? = null,
    val categoryId: Long? = null,
    val notes: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val imageUrl: String? = null
)
