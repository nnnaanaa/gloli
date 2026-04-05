package com.mortlace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class WishlistApplication

fun main(args: Array<String>) {
    runApplication<WishlistApplication>(*args)
}
