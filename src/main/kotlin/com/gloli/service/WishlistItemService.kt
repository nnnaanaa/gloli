package com.gloli.service

import com.gloli.domain.WishlistItem
import com.gloli.domain.enums.Priority
import com.gloli.domain.enums.Status
import com.gloli.dto.BrandResponse
import com.gloli.dto.CategoryResponse
import com.gloli.dto.WishlistItemRequest
import com.gloli.dto.WishlistItemResponse
import com.gloli.repository.BrandRepository
import com.gloli.repository.CategoryRepository
import com.gloli.repository.WishlistItemRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.File

@Service
@Transactional
class WishlistItemService(
    private val repo: WishlistItemRepository,
    private val brandRepo: BrandRepository,
    private val categoryRepo: CategoryRepository
) {

    @Transactional(readOnly = true)
    fun findAll(priority: Priority?, brandId: Long?, categoryId: Long?): List<WishlistItemResponse> {
        return repo.findAllByDeletedAtIsNull()
            .filter { it.status != Status.OWNED }
            .filter { priority == null || it.priority == priority }
            .filter { brandId == null || it.brand?.id == brandId }
            .filter { categoryId == null || it.category?.id == categoryId }
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findOwned(): List<WishlistItemResponse> =
        repo.findAllByDeletedAtIsNull()
            .filter { it.status == Status.OWNED }
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findDeleted(): List<WishlistItemResponse> =
        repo.findAllByDeletedAtIsNotNull().map { it.toResponse() }

    fun updateStatus(id: Long, status: Status): WishlistItemResponse {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        item.status = status
        return repo.save(item).toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): WishlistItemResponse =
        repo.findById(id).orElseThrow { notFound(id) }.toResponse()

    fun create(req: WishlistItemRequest): WishlistItemResponse {
        if (!req.url.isNullOrBlank() && repo.existsByUrlAndDeletedAtIsNull(req.url)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "This URL is already in your list")
        }
        val brand = req.brandId?.let {
            brandRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found: id=$it")
            }
        }
        val category = req.categoryId?.let {
            categoryRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: id=$it")
            }
        }
        val item = WishlistItem(
            name = req.name,
            url = req.url,
            price = req.price,
            brand = brand,
            category = category,
            notes = req.notes,
            priority = req.priority,
            imageUrl = req.imageUrl
        )
        return repo.save(item).toResponse()
    }

    fun update(id: Long, req: WishlistItemRequest): WishlistItemResponse {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        if (!req.url.isNullOrBlank() && req.url != item.url && repo.existsByUrlAndDeletedAtIsNullAndIdNot(req.url, id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "This URL is already in your list")
        }
        val brand = req.brandId?.let {
            brandRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found: id=$it")
            }
        }
        val category = req.categoryId?.let {
            categoryRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: id=$it")
            }
        }
        item.name = req.name
        item.url = req.url
        item.price = req.price
        item.brand = brand
        item.category = category
        item.notes = req.notes
        item.priority = req.priority
        item.status = req.status
        if (req.imageUrl != null) {
            item.imagePath?.let { File("./data/images/$it").absoluteFile.delete() }
            item.imagePath = null
        }
        item.imageUrl = req.imageUrl
        return repo.save(item).toResponse()
    }

    fun softDelete(id: Long) {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        item.deletedAt = java.time.LocalDateTime.now()
        repo.save(item)
    }

    fun restore(id: Long): WishlistItemResponse {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        item.deletedAt = null
        return repo.save(item).toResponse()
    }

    fun delete(id: Long) {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        item.imagePath?.let { File("./data/images/$it").absoluteFile.delete() }
        repo.deleteById(id)
    }

    fun uploadImage(id: Long, file: MultipartFile): WishlistItemResponse {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        val bytes = file.bytes
        val ext = detectImageExtension(bytes)
        val dir = File("./data/images").absoluteFile.also { it.mkdirs() }
        item.imagePath?.let { File(dir, it).delete() }
        val filename = "$id.$ext"
        File(dir, filename).writeBytes(bytes)
        item.imagePath = filename
        item.imageUrl = null
        return repo.save(item).toResponse()
    }

    private fun detectImageExtension(bytes: ByteArray): String {
        val b = bytes
        val isJpeg  = b.size >= 3  && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte()
        val isPng   = b.size >= 4  && b[0] == 0x89.toByte() && b[1] == 0x50.toByte() && b[2] == 0x4E.toByte() && b[3] == 0x47.toByte()
        val isGif   = b.size >= 4  && b[0] == 0x47.toByte() && b[1] == 0x49.toByte() && b[2] == 0x46.toByte() && b[3] == 0x38.toByte()
        val isWebp  = b.size >= 12 && b[0] == 0x52.toByte() && b[1] == 0x49.toByte() && b[2] == 0x46.toByte() && b[3] == 0x46.toByte()
                                   && b[8] == 0x57.toByte() && b[9] == 0x45.toByte() && b[10] == 0x42.toByte() && b[11] == 0x50.toByte()
        return when {
            isJpeg -> "jpg"
            isPng  -> "png"
            isGif  -> "gif"
            isWebp -> "webp"
            else   -> throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, GIF, and WebP images are allowed")
        }
    }

    fun getImage(id: Long): Pair<ByteArray, String> {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        val path = item.imagePath ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No image")
        val file = File("./data/images/$path").absoluteFile
        if (!file.exists()) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image file not found")
        val contentType = when (path.substringAfterLast('.').lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        return file.readBytes() to contentType
    }

    private fun WishlistItem.toResponse() = WishlistItemResponse(
        id = id,
        name = name,
        url = url,
        price = price,
        brand = brand?.let { BrandResponse(it.id, it.name, it.url) },
        category = category?.let { CategoryResponse(it.id, it.name) },
        notes = notes,
        priority = priority,
        status = status,
        imageUrl = imageUrl ?: if (imagePath != null) "/api/wishlist/$id/image" else null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun notFound(id: Long) =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: id=$id")
}
