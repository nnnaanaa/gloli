package com.gloli.dto

data class BulkRefreshResult(
    val total: Int,
    val updated: Int,
    val failed: Int,
    val changes: List<ItemChangeDetail> = emptyList()
)

data class ItemChangeDetail(
    val id: Long,
    val name: String?,
    val fields: List<FieldChange>
)

data class FieldChange(
    val field: String,
    val from: String?,
    val to: String?
)
