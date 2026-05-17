package com.gloli.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gloli.repository.CategoryRepository
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
class CategoryControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setUp() {
        categoryRepository.deleteAll()
    }

    private fun json(vararg pairs: Pair<String, Any?>) =
        objectMapper.writeValueAsString(pairs.toMap())

    @Test
    fun `list categories returns empty list initially`() {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `create category returns 201 with body`() {
        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "Shoes"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Shoes"))
            .andExpect(jsonPath("$.id").isNumber)
    }

    @Test
    fun `create category with blank name returns 400`() {
        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to ""))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create duplicate category name returns 409`() {
        val body = json("name" to "Clothing")
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
    }

    @Test
    fun `get category by id returns category`() {
        val created = mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "Electronics"))
        )
            .andReturn()
        val id = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        mockMvc.perform(get("/api/categories/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Electronics"))
    }

    @Test
    fun `get category by nonexistent id returns 404`() {
        mockMvc.perform(get("/api/categories/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update category changes name`() {
        val created = mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "Old Category"))
        )
            .andReturn()
        val id = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        mockMvc.perform(
            put("/api/categories/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "New Category"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Category"))
    }

    @Test
    fun `delete category returns 204 and category is gone`() {
        val created = mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("name" to "ToDelete"))
        )
            .andReturn()
        val id = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        mockMvc.perform(delete("/api/categories/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/categories/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete nonexistent category returns 404`() {
        mockMvc.perform(delete("/api/categories/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `list categories returns all created categories`() {
        listOf("Bags", "Watches", "Accessories").forEach { name ->
            mockMvc.perform(
                post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json("name" to name))
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }
}
