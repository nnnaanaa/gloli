package com.gloli.repository

import com.gloli.domain.WishlistItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WishlistItemRepository : JpaRepository<WishlistItem, Long> {
    fun findAllByDeletedAtIsNull(): List<WishlistItem>
    fun findAllByDeletedAtIsNotNull(): List<WishlistItem>
}
