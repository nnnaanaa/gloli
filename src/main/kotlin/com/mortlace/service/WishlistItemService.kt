package com.mortlace.service

import com.mortlace.domain.WishlistItem
import com.mortlace.domain.enums.Priority
import com.mortlace.dto.BrandResponse
import com.mortlace.dto.CategoryResponse
import com.mortlace.dto.WishlistItemRequest
import com.mortlace.dto.WishlistItemResponse
import com.mortlace.repository.BrandRepository
import com.mortlace.repository.CategoryRepository
import com.mortlace.repository.WishlistItemRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Service
@Transactional
class WishlistItemService(
    private val repo: WishlistItemRepository,
    private val brandRepo: BrandRepository,
    private val categoryRepo: CategoryRepository
) {

    @Transactional(readOnly = true)
    fun findAll(priority: Priority?, brandId: Long?, categoryId: Long?): List<WishlistItemResponse> {
        return repo.findAll()
            .filter { priority == null || it.priority == priority }
            .filter { brandId == null || it.brand?.id == brandId }
            .filter { categoryId == null || it.category?.id == categoryId }
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): WishlistItemResponse =
        repo.findById(id).orElseThrow { notFound(id) }.toResponse()

    fun create(req: WishlistItemRequest): WishlistItemResponse {
        val brand = req.brandId?.let {
            brandRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "ブランドが見つかりません: id=$it")
            }
        }
        val category = req.categoryId?.let {
            categoryRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "カテゴリーが見つかりません: id=$it")
            }
        }
        val item = WishlistItem(
            name = req.name,
            url = req.url,
            price = req.price,
            brand = brand,
            category = category,
            notes = req.notes,
            priority = req.priority
        )
        return repo.save(item).toResponse()
    }

    fun update(id: Long, req: WishlistItemRequest): WishlistItemResponse {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        val brand = req.brandId?.let {
            brandRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "ブランドが見つかりません: id=$it")
            }
        }
        val category = req.categoryId?.let {
            categoryRepo.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "カテゴリーが見つかりません: id=$it")
            }
        }
        item.name = req.name
        item.url = req.url
        item.price = req.price
        item.brand = brand
        item.category = category
        item.notes = req.notes
        item.priority = req.priority
        return repo.save(item).toResponse()
    }

    fun delete(id: Long) {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        item.imagePath?.let { File("./data/images/$it").absoluteFile.delete() }
        repo.deleteById(id)
    }

    fun uploadImage(id: Long, file: MultipartFile): WishlistItemResponse {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        val dir = File("./data/images").absoluteFile.also { it.mkdirs() }
        val ext = file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg"
        item.imagePath?.let { File(dir, it).delete() }
        val filename = "$id.$ext"
        file.inputStream.use { input ->
            Files.copy(input, File(dir, filename).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        item.imagePath = filename
        return repo.save(item).toResponse()
    }

    fun getImage(id: Long): Pair<ByteArray, String> {
        val item = repo.findById(id).orElseThrow { notFound(id) }
        val path = item.imagePath ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "画像がありません")
        val file = File("./data/images/$path").absoluteFile
        if (!file.exists()) throw ResponseStatusException(HttpStatus.NOT_FOUND, "画像ファイルが見つかりません")
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
        imageUrl = if (imagePath != null) "/api/wishlist/$id/image" else null,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun notFound(id: Long) =
        ResponseStatusException(HttpStatus.NOT_FOUND, "アイテムが見つかりません: id=$id")
}
