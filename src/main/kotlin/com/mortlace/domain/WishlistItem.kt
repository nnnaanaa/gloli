package com.mortlace.domain

import com.mortlace.domain.enums.Priority
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "wishlist_items")
class WishlistItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(columnDefinition = "TEXT")
    var name: String? = null,

    @Column(nullable = false, length = 2048)
    var url: String,

    var price: BigDecimal? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    var brand: Brand? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Enumerated(EnumType.STRING)
    var priority: Priority = Priority.MEDIUM,

    var imagePath: String? = null,

    @Column(length = 2048)
    var imageUrl: String? = null,

    @CreationTimestamp
    @Column(updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
