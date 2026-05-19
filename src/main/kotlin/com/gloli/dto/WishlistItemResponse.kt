package com.gloli.dto

import com.gloli.domain.enums.Priority
import com.gloli.domain.enums.Status
import java.math.BigDecimal
import java.time.LocalDateTime

/** ウィッシュリストアイテム取得レスポンス */
data class WishlistItemResponse(
    val id: Long,
    val name: String?,
    val url: String,
    val price: BigDecimal?,
    val brand: BrandResponse?,
    val category: CategoryResponse?,
    val notes: String?,
    val priority: Priority,
    val status: Status,
    /** 外部URLまたはローカル保存画像への APIパス。どちらもない場合は null */
    val imageUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    /** null = アクティブ、non-null = アーカイブ済み */
    val deletedAt: LocalDateTime?
)
