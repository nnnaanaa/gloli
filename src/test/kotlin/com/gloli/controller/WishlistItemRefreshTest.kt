package com.gloli.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gloli.dto.ScrapedProductInfo
import com.gloli.repository.BrandRepository
import com.gloli.repository.CategoryRepository
import com.gloli.repository.WishlistItemRepository
import com.gloli.service.ProductScraperService
import org.mockito.BDDMockito.given
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistItemRefreshTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var wishlistItemRepository: WishlistItemRepository
    @Autowired lateinit var brandRepository: BrandRepository
    @Autowired lateinit var categoryRepository: CategoryRepository

    @MockitoBean
    lateinit var productScraperService: ProductScraperService

    @BeforeEach
    fun setUp() {
        wishlistItemRepository.deleteAll()
        brandRepository.deleteAll()
        categoryRepository.deleteAll()
    }

    private fun scraped(
        name: String? = null,
        price: BigDecimal? = null,
        imageUrl: String? = null
    ) = ScrapedProductInfo(name = name, price = price, imageUrl = imageUrl,
        brand = null, brandId = null, description = null, category = null)

    private fun createItem(url: String, name: String? = null, price: BigDecimal? = null): Long {
        val body = mutableMapOf<String, Any?>("url" to url)
        if (name  != null) body["name"]  = name
        if (price != null) body["price"] = price
        val result = mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
    }

    // --- Basic counts ---

    @Test
    fun `refresh-all with no items returns zero counts and empty changes`() {
        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.updated").value(0))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.changes").isEmpty)
    }

    @Test
    fun `refresh-all counts failed when scrape throws`() {
        createItem(url = "https://example.com/bad")
        given(productScraperService.scrape("https://example.com/bad"))
            .willThrow(ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed"))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.updated").value(0))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.changes").isEmpty)
    }

    @Test
    fun `refresh-all excludes soft-deleted items`() {
        val id = createItem(url = "https://example.com/deleted")
        mockMvc.perform(delete("/api/wishlist/$id")).andExpect(status().isNoContent)

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    // --- Change detection ---

    @Test
    fun `refresh-all records name change`() {
        createItem(url = "https://example.com/item", name = "Old Name")
        given(productScraperService.scrape("https://example.com/item"))
            .willReturn(scraped(name = "New Name"))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.updated").value(1))
            .andExpect(jsonPath("$.changes.length()").value(1))
            .andExpect(jsonPath("$.changes[0].fields[0].field").value("name"))
            .andExpect(jsonPath("$.changes[0].fields[0].from").value("Old Name"))
            .andExpect(jsonPath("$.changes[0].fields[0].to").value("New Name"))
    }

    @Test
    fun `refresh-all records name set when previously null`() {
        createItem(url = "https://example.com/noname")
        given(productScraperService.scrape("https://example.com/noname"))
            .willReturn(scraped(name = "Fetched Name"))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.changes.length()").value(1))
            .andExpect(jsonPath("$.changes[0].fields[0].field").value("name"))
            .andExpect(jsonPath("$.changes[0].fields[0].from").doesNotExist())
            .andExpect(jsonPath("$.changes[0].fields[0].to").value("Fetched Name"))
    }

    @Test
    fun `refresh-all records price change`() {
        createItem(url = "https://example.com/priced", price = BigDecimal("1000"))
        given(productScraperService.scrape("https://example.com/priced"))
            .willReturn(scraped(price = BigDecimal("1200")))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.changes.length()").value(1))
            .andExpect(jsonPath("$.changes[0].fields[0].field").value("price"))
            .andExpect(jsonPath("$.changes[0].fields[0].from").value("1000"))
            .andExpect(jsonPath("$.changes[0].fields[0].to").value("1200"))
    }

    @Test
    fun `refresh-all records price set when previously null`() {
        createItem(url = "https://example.com/noprice")
        given(productScraperService.scrape("https://example.com/noprice"))
            .willReturn(scraped(price = BigDecimal("5000")))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.changes[0].fields[0].field").value("price"))
            .andExpect(jsonPath("$.changes[0].fields[0].from").doesNotExist())
            .andExpect(jsonPath("$.changes[0].fields[0].to").value("5000"))
    }

    @Test
    fun `refresh-all records imageUrl set when no image exists`() {
        createItem(url = "https://example.com/img")
        given(productScraperService.scrape("https://example.com/img"))
            .willReturn(scraped(imageUrl = "https://cdn.example.com/photo.jpg"))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.changes.length()").value(1))
            .andExpect(jsonPath("$.changes[0].fields[0].field").value("imageUrl"))
            .andExpect(jsonPath("$.changes[0].fields[0].from").doesNotExist())
            .andExpect(jsonPath("$.changes[0].fields[0].to").value("https://cdn.example.com/photo.jpg"))
    }

    @Test
    fun `refresh-all does not record change when values are unchanged`() {
        createItem(url = "https://example.com/same", name = "Same Name", price = BigDecimal("1000"))
        given(productScraperService.scrape("https://example.com/same"))
            .willReturn(scraped(name = "Same Name", price = BigDecimal("1000")))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.updated").value(1))
            .andExpect(jsonPath("$.changes").isEmpty)
    }

    @Test
    fun `refresh-all handles multiple items with mixed results`() {
        createItem(url = "https://example.com/a", name = "Item A", price = BigDecimal("1000"))
        createItem(url = "https://example.com/b", name = "Item B")
        createItem(url = "https://example.com/c")
        given(productScraperService.scrape("https://example.com/a"))
            .willReturn(scraped(name = "Item A", price = BigDecimal("1200")))
        given(productScraperService.scrape("https://example.com/b"))
            .willThrow(ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed"))
        given(productScraperService.scrape("https://example.com/c"))
            .willReturn(scraped(name = "Item C"))

        mockMvc.perform(post("/api/wishlist/refresh-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.updated").value(2))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.changes.length()").value(2))
    }
}
