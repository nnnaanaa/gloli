package com.mortlace.controller

import com.mortlace.domain.enums.Priority
import com.mortlace.dto.WishlistItemRequest
import com.mortlace.dto.WishlistItemResponse
import com.mortlace.service.WishlistItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "ロリィタ服ウィッシュリスト管理API")
class WishlistItemController(private val service: WishlistItemService) {

    @GetMapping
    @Operation(summary = "一覧取得", description = "優先度・ブランドIDでフィルタリング可能")
    fun list(
        @RequestParam priority: Priority? = null,
        @RequestParam brandId: Long? = null,
        @RequestParam categoryId: Long? = null
    ): List<WishlistItemResponse> = service.findAll(priority, brandId, categoryId)

    @GetMapping("/{id}")
    @Operation(summary = "1件取得")
    fun get(@PathVariable id: Long): WishlistItemResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "アイテム追加")
    fun create(@Valid @RequestBody req: WishlistItemRequest): WishlistItemResponse = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "アイテム更新")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: WishlistItemRequest
    ): WishlistItemResponse = service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "アイテム削除")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
