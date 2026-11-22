package com.example.lince_track

import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.data.formatNombreMes
import com.example.lince_track.ui.theme.AppLanguage
import com.example.lince_track.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.Month
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
    fun `fromLanguageTags traduce el idioma guardado por Android`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(""))
        assertEquals(AppLanguage.ES, AppLanguage.fromLanguageTags("es-CL"))
        assertEquals(AppLanguage.ES, AppLanguage.fromLanguageTags("es"))
        assertEquals(AppLanguage.EN, AppLanguage.fromLanguageTags("en-US,es-CL"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags("pt-BR"))
    }

    @Test
    fun `cada idioma tiene la etiqueta que se guarda en Android`() {
        assertEquals(null, AppLanguage.SYSTEM.languageTag)
        assertEquals("es-CL", AppLanguage.ES.languageTag)
        assertEquals("en", AppLanguage.EN.languageTag)
    }

    @Test
    fun `UiText DynamicString conserva el texto`() {
        val uiText = UiText.DynamicString("Hola")
        assertEquals("Hola", uiText.value)
    }

    @Test
    fun `formatos de fecha respetan el idioma seleccionado`() {
        val fecha = LocalDate.of(2026, 11, 21)
        assertEquals("November", formatNombreMes(Month.NOVEMBER, Locale.ENGLISH))
        assertEquals("November 21", formatFechaCorta(fecha, Locale.ENGLISH))
        assertEquals("Saturday, November 21", formatFecha(fecha, Locale.ENGLISH))
        assertEquals("Noviembre", formatNombreMes(Month.NOVEMBER))
    }
}
