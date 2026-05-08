package com.mortlace.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mortlace.dto.ScrapedProductInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@Service
class ProductScraperService {

    private val objectMapper = ObjectMapper()
    private val breadcrumbSkipNames = setOf("home", "ホーム", "top", "トップ", "トップページ")

    fun scrape(url: String): ScrapedProductInfo {
        val doc = try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                .referrer("https://www.google.com/")
                .timeout(10_000)
                .followRedirects(true)
                .get()
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch URL: ${e.message}")
        }

        return ScrapedProductInfo(
            name = doc.og("og:title") ?: doc.title().ifBlank { null },
            imageUrl = doc.og("og:image")?.let { resolveUrl(url, it) },
            brand = doc.og("og:site_name"),
            price = parsePrice(doc),
            description = doc.og("og:description"),
            category = parseCategory(doc)
        )
    }

    private fun Document.og(property: String): String? =
        select("meta[property=$property]").attr("content").ifBlank { null }

    private fun parsePrice(doc: Document): BigDecimal? {
        val raw = listOf(
            "og:price:amount",
            "product:price:amount",
            "og:price",
        ).firstNotNullOfOrNull { doc.og(it) } ?: return null

        return raw.replace(Regex("[^0-9.]"), "").toBigDecimalOrNull()
    }

    private fun parseCategory(doc: Document): String? {
        // 非標準OGPタグ（一部のサイトで使用）
        doc.og("og:category")?.let { return it }

        // Schema.org BreadcrumbList JSON-LD
        parseBreadcrumb(doc)?.let { return it }

        return null
    }

    private fun parseBreadcrumb(doc: Document): String? {
        doc.select("script[type=application/ld+json]").forEach { script ->
            try {
                val json = objectMapper.readTree(script.data())
                val nodes = if (json.isArray) json.toList() else listOf(json)
                for (node in nodes) {
                    if (node.get("@type")?.asText() != "BreadcrumbList") continue
                    val items = node.get("itemListElement") ?: continue
                    val names = items.toList()
                        .mapNotNull { it.get("name")?.asText()?.ifBlank { null } }
                        .filter { it.lowercase() !in breadcrumbSkipNames }
                    if (names.isEmpty()) continue
                    // 最後はたいてい商品名なので、その1つ前をカテゴリーとして返す
                    return names.dropLast(1).lastOrNull() ?: names.last()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun resolveUrl(base: String, target: String): String {
        if (target.startsWith("http://") || target.startsWith("https://")) return target
        val baseUri = try { java.net.URI(base) } catch (_: Exception) { return target }
        return try { baseUri.resolve(target).toString() } catch (_: Exception) { target }
    }
}
