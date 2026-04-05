package com.mortlace.service

import com.mortlace.repository.WishlistItemRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
class UrlCheckService(private val repo: WishlistItemRepository) {

    private val log = LoggerFactory.getLogger(UrlCheckService::class.java)

    @Scheduled(fixedDelayString = "\${mortlace.url-check.interval-ms:3600000}", initialDelay = 30000)
    fun checkAll() {
        val snapshot = loadSnapshot()
        log.info("URL check start: ${snapshot.size} items")

        snapshot.forEach { (id, url, prevHash, prevSnapshot) ->
            try {
                val (newHash, newText) = fetchContent(url) ?: return@forEach
                val now = LocalDateTime.now()
                if (prevHash != null && prevHash != newHash) {
                    log.info("Update detected: id=$id url=$url")
                    saveResult(id, newHash, now, hasUpdate = true, previousSnapshot = prevSnapshot, contentSnapshot = newText)
                } else {
                    saveResult(id, newHash, now, hasUpdate = false, previousSnapshot = prevSnapshot, contentSnapshot = newText)
                }
            } catch (e: Exception) {
                log.debug("Failed to check url id=$id: ${e.message}")
            }
        }
        log.info("URL check done")
    }

    @Transactional(readOnly = true)
    fun loadSnapshot(): List<Triple4> =
        repo.findAll().map { Triple4(it.id, it.url, it.contentHash, it.contentSnapshot) }

    @Transactional
    fun saveResult(id: Long, hash: String, checkedAt: LocalDateTime, hasUpdate: Boolean, previousSnapshot: String?, contentSnapshot: String?) {
        val item = repo.findById(id).orElse(null) ?: return
        if (hasUpdate) {
            item.hasUpdate = true
            item.previousSnapshot = previousSnapshot
        }
        item.contentHash = hash
        item.contentSnapshot = contentSnapshot
        item.lastCheckedAt = checkedAt
        repo.save(item)
    }

    private fun fetchContent(rawUrl: String): Pair<String, String>? {
        return try {
            val conn = URL(rawUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()
            val stripped = body
                .replace(Regex("<!--[\\s\\S]*?-->"), "")
                .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<noscript[\\s\\S]*?</noscript>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<template[\\s\\S]*?</template>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<svg[\\s\\S]*?</svg>", RegexOption.IGNORE_CASE), "")
                // strip all attributes to avoid token/nonce/session changes
                .replace(Regex("<(\\w+)[^>]*>"), "<$1>")
            val normalized = stripped.replace(Regex("\\s+"), " ").trim()
            val text = extractText(stripped)
            sha256(normalized) to text
        } catch (e: Exception) {
            null
        }
    }

    private fun extractText(html: String): String {
        return html
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#\\d+;")) { m ->
                m.value.removePrefix("&#").removeSuffix(";").toIntOrNull()?.toChar()?.toString() ?: " "
            }
            .split("\n")
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .let { text ->
                // limit snapshot size to 50k chars
                if (text.length > 50_000) text.substring(0, 50_000) else text
            }
    }

    private fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class Triple4(val id: Long, val url: String, val hash: String?, val snapshot: String?)
}
