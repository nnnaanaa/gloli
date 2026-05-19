package com.gloli

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** アプリケーションエントリーポイント */
@SpringBootApplication
class WishlistApplication

fun main(args: Array<String>) {
    runApplication<WishlistApplication>(*args)
}
