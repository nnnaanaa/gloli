package com.mortlace.controller

import com.mortlace.dto.CategoryRequest
import com.mortlace.dto.CategoryResponse
import com.mortlace.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Category management API")
class CategoryController(private val service: CategoryService) {

    @GetMapping
    @Operation(summary = "List categories")
    fun list(): List<CategoryResponse> = service.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    fun get(@PathVariable id: Long): CategoryResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add category")
    fun create(@Valid @RequestBody req: CategoryRequest): CategoryResponse = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: CategoryRequest): CategoryResponse =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete category")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
