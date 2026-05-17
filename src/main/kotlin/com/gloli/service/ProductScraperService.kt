package com.gloli.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gloli.dto.ScrapedProductInfo
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
            imageUrl = parseImage(doc, url),
            brand = parseBrand(doc),
            price = parsePrice(doc),
            description = doc.og("og:description"),
            category = parseCategory(doc)
        )
    }

    private fun Document.og(property: String): String? =
        select("meta[property=$property]").attr("content").ifBlank { null }

    private fun Document.meta(name: String): String? =
        select("meta[name=$name]").attr("content").ifBlank { null }

    // ---- Image ----
    private fun parseImage(doc: Document, baseUrl: String): String? {
        // OGP
        listOf("og:image", "og:image:secure_url").forEach { p ->
            doc.og(p)?.let { return resolveUrl(baseUrl, it) }
        }
        // Twitter card
        doc.meta("twitter:image")?.let { return resolveUrl(baseUrl, it) }

        // link rel="image_src"
        doc.select("link[rel=image_src]").attr("href").ifBlank { null }
            ?.let { return resolveUrl(baseUrl, it) }

        // itemprop=image
        doc.select("[itemprop=image]").firstOrNull()?.let { el ->
            val src = el.attr("content").ifBlank { el.attr("src") }.ifBlank { null }
            src?.let { return resolveUrl(baseUrl, it) }
        }

        // Common product image selectors (abs:src resolves relative URLs automatically)
        val selectors = listOf(
            "[class*=product-image] img", "[class*=product-img] img",
            "[class*=main-image] img",    "[class*=main-img] img",
            "[id*=main-image] img",       "[id*=main-img] img",
            "[id*=mainImg] img",          "[class*=item-image] img",
            "[class*=item-img] img",      "figure.product img",
            ".swiper-slide img",          "figure img"
        )
        for (sel in selectors) {
            doc.select(sel).firstOrNull()
                ?.attr("abs:src")?.ifBlank { null }
                ?.let { return it }
        }

        // Fallback: first large enough img in main content area
        val mainArea = doc.select("main, article, [role=main], #main, .main, #content, .content").firstOrNull() ?: doc
        mainArea.select("img[src]").firstOrNull { el ->
            val w = el.attr("width").toIntOrNull() ?: 0
            val h = el.attr("height").toIntOrNull() ?: 0
            (w == 0 && h == 0) || (w >= 100 && h >= 100)
        }?.attr("abs:src")?.ifBlank { null }?.let { return it }

        return null
    }

    // ---- Brand ----
    private fun parseBrand(doc: Document): String? {
        // og:site_name
        doc.og("og:site_name")?.let { return it }

        // Schema.org Product JSON-LD
        parseSchemaOrgProduct(doc)?.get("brand")?.let { brand ->
            (brand.get("name") ?: brand).asText()?.ifBlank { null }?.let { return it }
        }

        // itemprop=brand
        doc.select("[itemprop=brand]").firstOrNull()?.let { el ->
            el.attr("content").ifBlank { el.text() }.ifBlank { null }?.let { return it }
        }

        return null
    }

    // ---- Price ----
    private fun parsePrice(doc: Document): BigDecimal? {
        // OGP price tags
        listOf("og:price:amount", "product:price:amount", "og:price")
            .firstNotNullOfOrNull { doc.og(it) }
            ?.replace(Regex("[^0-9.]"), "")?.toBigDecimalOrNull()
            ?.let { return it }

        // Schema.org Product offers
        parseSchemaOrgProduct(doc)?.let { product ->
            val offers = product.get("offers") ?: return@let
            val offer = if (offers.isArray) offers.get(0) else offers
            offer?.get("price")?.asText()
                ?.replace(Regex("[^0-9.]"), "")?.toBigDecimalOrNull()
                ?.let { return it }
        }

        // itemprop=price
        doc.select("[itemprop=price]").firstOrNull()?.let { el ->
            el.attr("content").ifBlank { el.text() }
                .replace(Regex("[^0-9.]"), "").toBigDecimalOrNull()
                ?.let { return it }
        }

        // Price from DOM elements with "price" in class/id
        val priceSelectors = listOf(
            "[class*=price]", "[id*=price]",
            "dd", "span strong", "p strong"
        )
        for (sel in priceSelectors) {
            doc.select(sel).firstOrNull { el ->
                el.text().contains("¥") || el.text().contains("円")
            }?.text()?.let { text ->
                extractYenPrice(text)?.let { return it }
            }
        }

        return null
    }

    private fun extractYenPrice(text: String): BigDecimal? {
        // Prefer tax-included (税込) price, e.g. "¥16,500(税込)" or "16,500円(税込)"
        val taxIncluded = Regex("""¥\s*([\d,]+)\s*\(?税込\)?""")
        taxIncluded.find(text)?.groupValues?.get(1)
            ?.replace(",", "")?.toBigDecimalOrNull()?.let { return it }

        // Plain ¥ pattern
        val yen = Regex("""¥\s*([\d,]+)""")
        yen.find(text)?.groupValues?.get(1)
            ?.replace(",", "")?.toBigDecimalOrNull()?.let { return it }

        // XX,XXX円 pattern
        val kin = Regex("""([\d,]+)\s*円""")
        kin.find(text)?.groupValues?.get(1)
            ?.replace(",", "")?.toBigDecimalOrNull()?.let { return it }

        return null
    }

    // ---- Category ----
    private fun parseCategory(doc: Document): String? {
        doc.og("og:category")?.let { return it }
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
                    return names.dropLast(1).lastOrNull() ?: names.last()
                }
            } catch (_: Exception) {}
        }

        // HTML breadcrumb elements
        val breadcrumbSelectors = listOf(
            "[class*=breadcrumb] a", "[class*=bread-crumb] a",
            "[aria-label=breadcrumb] a", "nav.breadcrumb a"
        )
        for (sel in breadcrumbSelectors) {
            val crumbs = doc.select(sel)
                .map { it.text().trim() }
                .filter { it.isNotBlank() && it.lowercase() !in breadcrumbSkipNames }
            if (crumbs.size >= 2) return crumbs[crumbs.size - 2]
            if (crumbs.size == 1) return crumbs[0]
        }

        return null
    }

    // ---- Schema.org Product JSON-LD ----
    private fun parseSchemaOrgProduct(doc: Document): com.fasterxml.jackson.databind.JsonNode? {
        doc.select("script[type=application/ld+json]").forEach { script ->
            try {
                val json = objectMapper.readTree(script.data())
                val nodes = if (json.isArray) json.toList() else listOf(json)
                for (node in nodes) {
                    if (node.get("@type")?.asText() == "Product") return node
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
