package com.gloli.repository

import com.gloli.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BrandRepository : JpaRepository<Brand, Long> {
    fun existsByName(name: String): Boolean
}
