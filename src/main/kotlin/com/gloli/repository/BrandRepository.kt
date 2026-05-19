package com.gloli.repository

import com.gloli.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** ブランドの永続化操作 */
@Repository
interface BrandRepository : JpaRepository<Brand, Long> {
    /** 名前の重複チェックに使用する */
    fun existsByName(name: String): Boolean
}
