package com.mortlace.controller

import com.mortlace.dto.ScrapedProductInfo
import com.mortlace.service.ProductScraperService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/scrape")
@Tag(name = "Scraper", description = "Fetch product info from URL")
class ScraperController(private val service: ProductScraperService) {

    @GetMapping
    @Operation(
        summary = "Scrape product info",
        description = "Fetches product name, image, brand, and price from OGP metadata of the given URL"
    )
    fun scrape(@RequestParam url: String): ScrapedProductInfo = service.scrape(url)
}
