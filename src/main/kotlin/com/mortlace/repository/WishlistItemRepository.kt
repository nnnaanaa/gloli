package com.mortlace.repository

import com.mortlace.domain.WishlistItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WishlistItemRepository : JpaRepository<WishlistItem, Long>
