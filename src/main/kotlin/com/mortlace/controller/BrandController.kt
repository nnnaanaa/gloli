package com.mortlace.controller

import com.mortlace.dto.BrandRequest
import com.mortlace.dto.BrandResponse
import com.mortlace.service.BrandService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/brands")
@Tag(name = "Brands", description = "Brand management API")
class BrandController(private val service: BrandService) {

    @GetMapping
    @Operation(summary = "List brands")
    fun list(): List<BrandResponse> = service.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID")
    fun get(@PathVariable id: Long): BrandResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add brand")
    fun create(@Valid @RequestBody req: BrandRequest): BrandResponse = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Update brand")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: BrandRequest): BrandResponse =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete brand")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
