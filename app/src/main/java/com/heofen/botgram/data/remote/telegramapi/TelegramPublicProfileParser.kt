package com.heofen.botgram.data.remote.telegramapi

private val descriptionDivRegex = Regex(
    pattern = """<div\s+[^>]*class=["'][^"']*\btgme_page_description\b[^"']*["'][^>]*>(.*?)</div>""",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

private val metaTagRegex = Regex(
    pattern = """<meta\b[^>]*>""",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

private val attributeRegex = Regex(
    pattern = """([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*("([^"]*)"|'([^']*)')""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val tagRegex = Regex(
    pattern = """<[^>]+>""",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

private val breakRegex = Regex(
    pattern = """<br\s*/?>""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val paragraphEndRegex = Regex(
    pattern = """</p\s*>""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val blankLineRegex = Regex("""\n{3,}""")

/** Извлекает публичное описание профиля из HTML страницы `t.me/<username>`. */
internal fun extractTelegramProfileBioFromHtml(html: String): String? {
    extractDescriptionDiv(html)?.takeUnless(::looksLikeTelegramFallbackDescription)?.let { return it }
    extractMetaDescription(html)?.takeUnless(::looksLikeTelegramFallbackDescription)?.let { return it }
    return null
}

private fun extractDescriptionDiv(html: String): String? {
    val raw = descriptionDivRegex.find(html)?.groupValues?.getOrNull(1) ?: return null
    return normalizeHtmlText(raw)
}

private fun extractMetaDescription(html: String): String? {
    val metaTag = metaTagRegex.findAll(html)
        .map { it.value }
        .firstOrNull { tag ->
            val attributes = tag.extractAttributes()
            val marker = attributes["property"] ?: attributes["name"]
            marker.equals("og:description", ignoreCase = true) ||
                marker.equals("description", ignoreCase = true)
        }
        ?: return null

    val content = metaTag.extractAttributes()["content"] ?: return null
    return normalizeHtmlText(content)
}

private fun String.extractAttributes(): Map<String, String> {
    return attributeRegex.findAll(this).associate { match ->
        val key = match.groupValues[1].lowercase()
        val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
        key to value
    }
}

private fun normalizeHtmlText(raw: String): String? {
    val text = raw
        .replace(breakRegex, "\n")
        .replace(paragraphEndRegex, "\n\n")
        .replace(tagRegex, "")
        .decodeHtmlEntities()
        .lines()
        .joinToString("\n") { it.trim() }
        .replace(blankLineRegex, "\n\n")
        .trim()

    return text.takeIf { it.isNotBlank() }
}

private fun looksLikeTelegramFallbackDescription(text: String): Boolean {
    val normalized = text
        .trim()
        .replace(Regex("""\s+"""), " ")
        .lowercase()

    return normalized.startsWith("if you have telegram, you can contact") ||
        normalized.startsWith("if you have telegram, you can view and join") ||
        normalized.startsWith("you can contact ") ||
        normalized.startsWith("you can view and join ")
}

private fun String.decodeHtmlEntities(): String {
    val namedEntities = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "#39" to "'",
        "apos" to "'",
        "nbsp" to " "
    )

    return Regex("""&(#x?[0-9A-Fa-f]+|[A-Za-z]+);""").replace(this) { match ->
        val entity = match.groupValues[1]
        namedEntities[entity]
            ?: decodeNumericEntity(entity)
            ?: match.value
    }
}

private fun decodeNumericEntity(entity: String): String? {
    return when {
        entity.startsWith("#x", ignoreCase = true) -> entity.substring(2)
            .toIntOrNull(radix = 16)
            ?.toChar()
            ?.toString()

        entity.startsWith("#") -> entity.substring(1)
            .toIntOrNull()
            ?.toChar()
            ?.toString()

        else -> null
    }
}
