package com.gloli

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/** 起動時に実行するデータ補完処理 */
@Component
class DataMigrationRunner(private val jdbc: JdbcTemplate) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        // status カラムが NOT NULL になる前に登録されたレコードの補完
        jdbc.update("UPDATE wishlist_items SET status = 'WANTED' WHERE status IS NULL")
    }
}
