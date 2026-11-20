package cl.pagoflex.app.nucleo.util

object ValidadorRut {

    fun formatearRut(input: String): String {
        val limpio = input.uppercase().filter { it.isDigit() || it == 'K' }.take(9)
        if (limpio.length <= 1) return limpio

        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()

        val cuerpoConPuntos = cuerpo
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

        return "$cuerpoConPuntos-$dv"
    }

    fun esRutValido(rut: String): Boolean {
        val limpio = rut.uppercase().filter { it.isDigit() || it == 'K' }
        if (limpio.length !in 8..9) return false

        val cuerpo = limpio.dropLast(1)
        val dvIngresado = limpio.last()
        if (cuerpo.any { !it.isDigit() }) return false

        var suma = 0
        var multiplicador = 2
        for (i in cuerpo.length - 1 downTo 0) {
            suma += cuerpo[i].digitToInt() * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }

        val resto = 11 - (suma % 11)
        val dvEsperado = when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> resto.digitToChar()
        }

        return dvIngresado == dvEsperado
    }
}