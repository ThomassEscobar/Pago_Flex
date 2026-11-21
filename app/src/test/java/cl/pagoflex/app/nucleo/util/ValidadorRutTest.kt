package cl.pagoflex.app.nucleo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorRutTest {

    @Test
    fun `formatearRut agrega puntos y guion correctamente`() {
        assertEquals("12.345.678-5", ValidadorRut.formatearRut("123456785"))
        assertEquals("76.123.456-0", ValidadorRut.formatearRut("76.123.456-0"))
        assertEquals("11.111.112-K", ValidadorRut.formatearRut("11111112k"))
    }

    @Test
    fun `formatearRut limita largo maximo a 9 caracteres validos`() {
        assertEquals("12.345.678-9", ValidadorRut.formatearRut("123456789000"))
    }

    @Test
    fun `esRutValido acepta RUTs chilenos validos con digito numerico, 0 y K`() {
        assertTrue(ValidadorRut.esRutValido("12.345.678-5"))
        assertTrue(ValidadorRut.esRutValido("123456785"))
        assertTrue(ValidadorRut.esRutValido("76.123.456-0"))
        assertTrue(ValidadorRut.esRutValido("11.111.111-1"))
        assertTrue(ValidadorRut.esRutValido("11.111.112-K"))
        assertTrue(ValidadorRut.esRutValido("11111112k"))
    }

    @Test
    fun `esRutValido rechaza digitos verificadores erroneos o formatos invalidos`() {
        assertFalse(ValidadorRut.esRutValido("12.345.678-9"))
        assertFalse(ValidadorRut.esRutValido("76.123.456-K"))
        assertFalse(ValidadorRut.esRutValido("123456"))
        assertFalse(ValidadorRut.esRutValido(""))
        assertFalse(ValidadorRut.esRutValido("12.34K.678-5"))
    }
}