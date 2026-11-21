package com.example.lince_track

import com.example.lince_track.ui.theme.AppLanguage
import com.example.lince_track.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {

    @Test
    fun `fromStored devuelve SYSTEM cuando es nulo o desconocido`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStored(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStored(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStored("FR"))
    }

    @Test
    fun `fromStored reconoce ES y EN`() {
        assertEquals(AppLanguage.ES, AppLanguage.fromStored("ES"))
        assertEquals(AppLanguage.EN, AppLanguage.fromStored("EN"))
    }

    @Test
    fun `resolveLocale resuelve correctamente cada idioma`() {
        val system = Locale.forLanguageTag("pt-BR")
        assertEquals(system, AppLanguage.SYSTEM.resolveLocale(system))
        assertEquals("es", AppLanguage.ES.resolveLocale(system).language)
        assertEquals("en", AppLanguage.EN.resolveLocale(system).language)
    }

    @Test
    fun `UiText DynamicString conserva el texto`() {
        val uiText = UiText.DynamicString("Hola")
        assertEquals("Hola", uiText.value)
    }
}
