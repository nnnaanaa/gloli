package com.gloli.service

import com.gloli.domain.Brand
import com.gloli.dto.BrandRequest
import com.gloli.dto.BrandResponse
import com.gloli.repository.BrandRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class BrandService(private val repo: BrandRepository) {

    @Transactional(readOnly = true)
    fun findAll(): List<BrandResponse> = repo.findAll().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findById(id: Long): BrandResponse = repo.findById(id).orElseThrow { notFound(id) }.toResponse()

    fun create(req: BrandRequest): BrandResponse {
        if (repo.existsByName(req.name)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Brand name already exists: ${req.name}")
        }
        return repo.save(Brand(name = req.name, url = req.url)).toResponse()
    }

    fun update(id: Long, req: BrandRequest): BrandResponse {
        val brand = repo.findById(id).orElseThrow { notFound(id) }
        brand.name = req.name
        brand.url = req.url
        return repo.save(brand).toResponse()
    }

    fun delete(id: Long) {
        if (!repo.existsById(id)) throw notFound(id)
        repo.deleteById(id)
    }

    fun Brand.toResponse() = BrandResponse(id = id, name = name, url = url)

    private fun notFound(id: Long) =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found: id=$id")
}
