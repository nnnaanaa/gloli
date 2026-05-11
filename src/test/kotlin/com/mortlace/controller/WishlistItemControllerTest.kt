package com.mortlace.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mortlace.repository.BrandRepository
import com.mortlace.repository.CategoryRepository
import com.mortlace.repository.WishlistItemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistItemControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var wishlistItemRepository: WishlistItemRepository
    @Autowired lateinit var brandRepository: BrandRepository
    @Autowired lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setUp() {
        wishlistItemRepository.deleteAll()
        brandRepository.deleteAll()
        categoryRepository.deleteAll()
    }

    private fun json(vararg pairs: Pair<String, Any?>) =
        objectMapper.writeValueAsString(pairs.toMap())

    private fun createItem(
        url: String = "https://example.com/item",
        name: String? = "Test Item",
        priority: String = "MEDIUM"
    ): Long {
        val result = mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to url, "name" to name, "priority" to priority))
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
    }

    // --- Create ---

    @Test
    fun `create item returns 201 with body`() {
        mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to "https://example.com/shoes", "name" to "Running Shoes", "priority" to "HIGH"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Running Shoes"))
            .andExpect(jsonPath("$.url").value("https://example.com/shoes"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.status").value("WANTED"))
            .andExpect(jsonPath("$.id").isNumber)
    }

    @Test
    fun `create item without url returns 400`() {
        mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "No URL Item", "url" to ""))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create item with url exceeding 2048 chars returns 400`() {
        val longUrl = "https://example.com/" + "a".repeat(2048)
        mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to longUrl))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create item with brand and category links them`() {
        val brandId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/brands").contentType(MediaType.APPLICATION_JSON).content(json("name" to "Nike"))
            ).andReturn().response.contentAsString
        )["id"].asLong()

        val catId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(json("name" to "Shoes"))
            ).andReturn().response.contentAsString
        )["id"].asLong()

        mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to "https://nike.com/shoe", "brandId" to brandId, "categoryId" to catId))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.brand.id").value(brandId))
            .andExpect(jsonPath("$.brand.name").value("Nike"))
            .andExpect(jsonPath("$.category.id").value(catId))
            .andExpect(jsonPath("$.category.name").value("Shoes"))
    }

    @Test
    fun `create item with nonexistent brand returns 404`() {
        mockMvc.perform(
            post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to "https://example.com", "brandId" to 99999L))
        )
            .andExpect(status().isNotFound)
    }

    // --- Get ---

    @Test
    fun `get item by id returns item`() {
        val id = createItem(url = "https://example.com/get-test")

        mockMvc.perform(get("/api/wishlist/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
    }

    @Test
    fun `get item by nonexistent id returns 404`() {
        mockMvc.perform(get("/api/wishlist/99999"))
            .andExpect(status().isNotFound)
    }

    // --- List ---

    @Test
    fun `list excludes owned items`() {
        val id = createItem()
        mockMvc.perform(
            patch("/api/wishlist/$id/status").param("status", "OWNED")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/wishlist"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `list excludes soft-deleted items`() {
        val id = createItem()
        mockMvc.perform(delete("/api/wishlist/$id")).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/wishlist"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `list filters by priority`() {
        createItem(url = "https://a.com", priority = "HIGH")
        createItem(url = "https://b.com", priority = "LOW")
        createItem(url = "https://c.com", priority = "HIGH")

        mockMvc.perform(get("/api/wishlist").param("priority", "HIGH"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        mockMvc.perform(get("/api/wishlist").param("priority", "LOW"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `list filters by brandId`() {
        val brandId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/brands").contentType(MediaType.APPLICATION_JSON).content(json("name" to "Adidas"))
            ).andReturn().response.contentAsString
        )["id"].asLong()

        mockMvc.perform(
            post("/api/wishlist").contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to "https://adidas.com/a", "brandId" to brandId))
        ).andExpect(status().isCreated)
        createItem(url = "https://other.com")

        mockMvc.perform(get("/api/wishlist").param("brandId", brandId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].brand.name").value("Adidas"))
    }

    // --- Update ---

    @Test
    fun `update item changes fields`() {
        val id = createItem()

        mockMvc.perform(
            put("/api/wishlist/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to "https://updated.com", "name" to "Updated Name", "priority" to "HIGH", "status" to "WANTED"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.url").value("https://updated.com"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
    }

    @Test
    fun `update nonexistent item returns 404`() {
        mockMvc.perform(
            put("/api/wishlist/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("url" to "https://example.com", "status" to "WANTED"))
        )
            .andExpect(status().isNotFound)
    }

    // --- Status ---

    @Test
    fun `update status to owned moves item to owned list`() {
        val id = createItem()

        mockMvc.perform(patch("/api/wishlist/$id/status").param("status", "OWNED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OWNED"))

        mockMvc.perform(get("/api/wishlist/owned"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id))
    }

    @Test
    fun `update status on nonexistent item returns 404`() {
        mockMvc.perform(patch("/api/wishlist/99999/status").param("status", "OWNED"))
            .andExpect(status().isNotFound)
    }

    // --- Soft delete and restore ---

    @Test
    fun `soft delete moves item to deleted list`() {
        val id = createItem()

        mockMvc.perform(delete("/api/wishlist/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/wishlist/deleted"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id))
    }

    @Test
    fun `restore brings item back to wishlist`() {
        val id = createItem()
        mockMvc.perform(delete("/api/wishlist/$id")).andExpect(status().isNoContent)

        mockMvc.perform(post("/api/wishlist/$id/restore"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.deletedAt").doesNotExist())

        mockMvc.perform(get("/api/wishlist/deleted"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        mockMvc.perform(get("/api/wishlist"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `soft delete nonexistent item returns 404`() {
        mockMvc.perform(delete("/api/wishlist/99999"))
            .andExpect(status().isNotFound)
    }

    // --- Permanent delete ---

    @Test
    fun `permanent delete removes item entirely`() {
        val id = createItem()

        mockMvc.perform(delete("/api/wishlist/$id/permanent"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/wishlist/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `permanent delete nonexistent item returns 404`() {
        mockMvc.perform(delete("/api/wishlist/99999/permanent"))
            .andExpect(status().isNotFound)
    }

    // --- Owned list ---

    @Test
    fun `owned list excludes soft-deleted owned items`() {
        val id = createItem()
        mockMvc.perform(patch("/api/wishlist/$id/status").param("status", "OWNED"))
        mockMvc.perform(delete("/api/wishlist/$id"))

        mockMvc.perform(get("/api/wishlist/owned"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // --- Image endpoint: no file ---

    @Test
    fun `get image for item without image returns 404`() {
        val id = createItem()

        mockMvc.perform(get("/api/wishlist/$id/image"))
            .andExpect(status().isNotFound)
    }
}
