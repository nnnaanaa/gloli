package com.gloli.controller

import com.gloli.domain.enums.Priority
import com.gloli.domain.enums.Status
import com.gloli.dto.BulkRefreshResult
import com.gloli.dto.WishlistItemRequest
import com.gloli.dto.WishlistItemResponse
import com.gloli.service.WishlistItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/** ウィッシュリストアイテムの CRUD・画像管理エンドポイント */
@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "Wishlist management API")
class WishlistItemController(private val service: WishlistItemService) {

    /** OWNED 以外のアクティブなアイテム一覧。priority / brandId / categoryId でフィルタ可能 */
    @GetMapping
    @Operation(summary = "List items", description = "Filterable by priority, brand ID, and category ID")
    fun list(
        @RequestParam priority: Priority? = null,
        @RequestParam brandId: Long? = null,
        @RequestParam categoryId: Long? = null
    ): List<WishlistItemResponse> = service.findAll(priority, brandId, categoryId)

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    fun get(@PathVariable id: Long): WishlistItemResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add item")
    fun create(@Valid @RequestBody req: WishlistItemRequest): WishlistItemResponse = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Update item")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: WishlistItemRequest
    ): WishlistItemResponse = service.update(id, req)

    /** OWNED なアイテム一覧（コレクション画面で使用）*/
    @GetMapping("/owned")
    @Operation(summary = "List owned items")
    fun listOwned(): List<WishlistItemResponse> = service.findOwned()

    /** ソフトデリート済みアイテム一覧（アーカイブ画面で使用）*/
    @GetMapping("/deleted")
    @Operation(summary = "List soft-deleted items")
    fun listDeleted(): List<WishlistItemResponse> = service.findDeleted()

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update item status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestParam status: Status
    ): WishlistItemResponse = service.updateStatus(id, status)

    @PatchMapping("/{id}/priority")
    @Operation(summary = "Update item priority")
    fun updatePriority(
        @PathVariable id: Long,
        @RequestParam priority: Priority
    ): WishlistItemResponse = service.updatePriority(id, priority)

    /** アーカイブへ移動（物理削除ではなく deletedAt をセットする）*/
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete item")
    fun softDelete(@PathVariable id: Long) = service.softDelete(id)

    /** DB から完全に削除する。ローカル画像ファイルも合わせて削除される */
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Permanently delete item")
    fun delete(@PathVariable id: Long) = service.delete(id)

    /** アーカイブから復元する（deletedAt を null に戻す）*/
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore soft-deleted item")
    fun restore(@PathVariable id: Long): WishlistItemResponse = service.restore(id)

    @PostMapping("/{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload image")
    fun uploadImage(
        @PathVariable id: Long,
        @RequestParam file: MultipartFile
    ): WishlistItemResponse = service.uploadImage(id, file)

    /** 全アクティブアイテムを再スクレイプして price・name（空欄時）・imageUrl（未設定時）を更新する */
    @PostMapping("/refresh-all")
    @Operation(summary = "Bulk refresh all items", description = "Re-scrapes each item's URL and updates price, name (if blank), and imageUrl (if not set)")
    fun refreshAll(): BulkRefreshResult = service.refreshAll()

    @GetMapping("/{id}/image")
    @Operation(summary = "Get image")
    fun getImage(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val (bytes, contentType) = service.getImage(id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(bytes)
    }
}
