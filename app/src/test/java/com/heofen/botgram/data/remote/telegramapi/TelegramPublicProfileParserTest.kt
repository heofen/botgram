package com.heofen.botgram.data.remote.telegramapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelegramPublicProfileParserTest {
    @Test
    fun extractTelegramProfileBioFromHtml_readsDescriptionBlock() {
        val html = """
            <html>
                <body>
                    <div class="tgme_page_description" dir="auto">
                        Hello<br/>world <a href="https://t.me/example">@example</a>&nbsp;&amp; friends
                    </div>
                </body>
            </html>
        """.trimIndent()

        val bio = extractTelegramProfileBioFromHtml(html)

        assertEquals("Hello\nworld @example & friends", bio)
    }

    @Test
    fun extractTelegramProfileBioFromHtml_ignoresTelegramFallbackMeta() {
        val html = """
            <html>
                <head>
                    <meta property="og:description" content="If you have Telegram, you can contact Example right away.">
                </head>
            </html>
        """.trimIndent()

        val bio = extractTelegramProfileBioFromHtml(html)

        assertNull(bio)
    }

    @Test
    fun extractTelegramProfileBioFromHtml_ignoresTelegramFallbackDescriptionBlock() {
        val html = """
            <html>
                <body>
                    <div class="tgme_page_description" dir="auto">
                        You can contact <a href="https://t.me/example">@example</a> right away.
                    </div>
                </body>
            </html>
        """.trimIndent()

        val bio = extractTelegramProfileBioFromHtml(html)

        assertNull(bio)
    }

    @Test
    fun extractTelegramProfileBioFromHtml_usesMetaDescriptionWhenNeeded() {
        val html = """
            <html>
                <head>
                    <meta property="og:description" content="Build stuff &amp; ship">
                </head>
            </html>
        """.trimIndent()

        val bio = extractTelegramProfileBioFromHtml(html)

        assertEquals("Build stuff & ship", bio)
    }
}
