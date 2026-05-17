package com.gloli

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DataMigrationRunner(private val jdbc: JdbcTemplate) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        jdbc.update("UPDATE wishlist_items SET status = 'WANTED' WHERE status IS NULL")
    }
}
