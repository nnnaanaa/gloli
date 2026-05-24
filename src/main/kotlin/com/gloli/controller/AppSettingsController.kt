package com.gloli.controller

import com.gloli.service.AppSettingsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "App settings API")
class AppSettingsController(private val service: AppSettingsService) {

    @GetMapping("/{key}")
    @Operation(summary = "Get setting value")
    fun get(@PathVariable key: String): ResponseEntity<String> {
        val value = service.get(key) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(value)
    }

    @PutMapping("/{key}")
    @Operation(summary = "Save setting value")
    fun put(@PathVariable key: String, @RequestBody value: String) = service.set(key, value)
}
