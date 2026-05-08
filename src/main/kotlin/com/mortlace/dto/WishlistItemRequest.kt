package com.mortlace.dto

import com.mortlace.domain.enums.Priority
import com.mortlace.domain.enums.Status
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

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
    val imageUrl: String? = null,
    val status: Status = Status.WANTED
)
