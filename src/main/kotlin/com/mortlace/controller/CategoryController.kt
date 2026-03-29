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
@Tag(name = "Categories", description = "カテゴリー管理API")
class CategoryController(private val service: CategoryService) {

    @GetMapping
    @Operation(summary = "カテゴリー一覧")
    fun list(): List<CategoryResponse> = service.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "カテゴリー1件取得")
    fun get(@PathVariable id: Long): CategoryResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "カテゴリー追加")
    fun create(@Valid @RequestBody req: CategoryRequest): CategoryResponse = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "カテゴリー更新")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: CategoryRequest): CategoryResponse =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "カテゴリー削除")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
