package com.mortlace.service

import com.mortlace.domain.Category
import com.mortlace.dto.CategoryRequest
import com.mortlace.dto.CategoryResponse
import com.mortlace.repository.CategoryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class CategoryService(private val repo: CategoryRepository) {

    @Transactional(readOnly = true)
    fun findAll(): List<CategoryResponse> = repo.findAll().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findById(id: Long): CategoryResponse = repo.findById(id).orElseThrow { notFound(id) }.toResponse()

    fun create(req: CategoryRequest): CategoryResponse {
        if (repo.existsByName(req.name)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists: ${req.name}")
        }
        return repo.save(Category(name = req.name)).toResponse()
    }

    fun update(id: Long, req: CategoryRequest): CategoryResponse {
        val category = repo.findById(id).orElseThrow { notFound(id) }
        category.name = req.name
        return repo.save(category).toResponse()
    }

    fun delete(id: Long) {
        if (!repo.existsById(id)) throw notFound(id)
        repo.deleteById(id)
    }

    private fun Category.toResponse() = CategoryResponse(id = id, name = name)

    private fun notFound(id: Long) =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: id=$id")
}
