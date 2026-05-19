package com.gloli.repository

import com.gloli.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** カテゴリの永続化操作 */
@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    /** 名前の重複チェックに使用する */
    fun existsByName(name: String): Boolean
}
