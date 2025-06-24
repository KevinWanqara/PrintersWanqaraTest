package com.example.printerswanqaratest.core.print.utils.printer1

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode

class PrinterHelpers(var impresoraCaracteres: Int, var impresoraCopias: Int) {

    private var trabajo:String = ""
    private val det: JSONObject? = null

    // CONSTRUCTOR
    fun Helper(impresoraCaracteres: Int, impresoraCopias: Int) {
        this.impresoraCaracteres = impresoraCaracteres
        this.impresoraCopias = impresoraCopias
    }

    fun getTrabajo(): String {
        var resutado = ""
        trabajo = narmalizarString(trabajo)
        for (i in 0 until impresoraCopias) {
            resutado += trabajo
        }
        return resutado
    }

    // METODOS

    // METODOS
    private fun narmalizarString(szString: String): String {
        var szString = szString
        szString = szString.replace("Á", "A")
        szString = szString.replace("É", "E")
        szString = szString.replace("Í", "I")
        szString = szString.replace("Ó", "O")
        szString = szString.replace("Ú", "U")
        szString = szString.replace("á", "a")
        szString = szString.replace("é", "e")
        szString = szString.replace("í", "i")
        szString = szString.replace("ó", "o")
        szString = szString.replace("ú", "u")
        szString = szString.replace("ñ", "n")
        szString = szString.replace("Ñ", "N")
        szString = szString.replace("º", "o")
        szString = szString.replace("ª", "o")
        return szString
    }

    fun beep1() {
        escribirTextoSinSalto("\u001B" + "\u0042" + "\u0005" + "\u0002")
    }

    fun beep2() {
        escribirTextoSinSalto("\u001B" + "\u0042" + "\u0004" + "\u0003")
    }

    fun iniciar() {
        escribirTextoSinSalto("\u001B" + "\u0040")
        escribirTextoSinSalto("\u001B" + "\u0074" + "\u0002")
    }

    fun lineHeight() {
        escribirTextoSinSalto("\u001B" + "\u0033" + "\u0013")
    }

    fun lineHeight2() {
        escribirTextoSinSalto("\u001B" + "\u0032")
    }

    fun feedFinal() {
        val desplazamientoFinal = "\n\n\n\n\n\n\n"
        trabajo = trabajo + desplazamientoFinal
        trabajo = trabajo + "\u001B" + "\u0033" + "\u0013"
    }

    fun negritaOff() {
        escribirTextoSinSalto("\u001B" + "\u0045" + "\u0000")
    }

    fun negritaOn() {
        escribirTextoSinSalto("\u001B" + "\u0045" + "\u0001")
    }

    fun cortar() {
        escribirTextoSinSalto("\u001B" + "\u0069")
    }

    fun setFontA() {
        escribirTextoSinSalto("\u001B" + "\u004D" + "\u0001")
    }

    fun setFontB() {
        escribirTextoSinSalto("\u001B" + "\u004D" + "\u0001")
    }

    fun setFontC() {
        escribirTextoSinSalto("\u001B" + "\u004D" + "\u0002")
    }

    fun setFontD() {
        escribirTextoSinSalto("\u001B" + "\u004D" + "\u0003")
    }

    fun setFontE() {
        escribirTextoSinSalto("\u001B" + "\u004D" + "\u0004")
    }

    fun dobleAnchoOn() {
        escribirTextoSinSalto("\u001B" + "\u0021" + "\u0020")
    }

    fun dobleAnchoOff() {
        escribirTextoSinSalto("\u001B" + "\u0021" + "\u0000")
    }

    fun dobleAltoOn() {
        escribirTextoSinSalto("\u001B" + "\u0021" + "\u0010")
    }

    fun dobleAltoOff() {
        escribirTextoSinSalto("\u001B" + "\u0021" + "\u0000")
    }

    fun abrirGaveta1() {
        escribirTextoSinSalto("\u001B" + "p" + "\u0000" + "\u000F" + "\u0096")
    }

    fun abrirGaveta2() {
        escribirTextoSinSalto("\u001B" + "p" + "\u0001" + "\u000F" + "\u0096")
    }

    fun alineadoDerechaForce(texto: String) {
        var textoProcesado = ""
        var i = 0
        while (i < texto.length) {
            var text = ""
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
                var aux = ""
                for (j in 0 until impresoraCaracteres - text.length) {
                    aux = "$aux "
                }
                text = aux + text
            }
            escribirTexto(text)
            i += impresoraCaracteres
        }
    }

    fun alineadoIzquierdaForce(texto: String) {
        var textoProcesado = ""
        var i = 0
        while (i < texto.length) {
            var text = ""
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
                var aux = ""
                for (j in 0 until impresoraCaracteres - text.length) {
                    aux = "$aux "
                }
                text = text + aux
            }
            escribirTexto(text)
            i += impresoraCaracteres
        }
    }

    fun alineadoCentroForce(texto: String) {
        var textoProcesado = ""
        var i = 0
        while (i < texto.length) {
            var text = ""
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
                var aux = ""
                var aux1 = ""
                var `val` = 1
                for (j in 0 until impresoraCaracteres - text.length) {
                    if (`val` == 1) {
                        aux = "$aux "
                        `val` = 0
                    } else {
                        aux1 = "$aux1 "
                        `val` = 1
                    }
                }
                text = aux + text + aux1
            }
            escribirTexto(text)
            i += impresoraCaracteres
        }
    }

    fun LineasGuion() {
        var lineasGuion = ""
        for (i in 0 until impresoraCaracteres) {
            lineasGuion = "$lineasGuion-"
        }
        agregarTexto(lineasGuion)
    }

    fun LineasAsterisco() {
        var lineasAsterisco = ""
        for (i in 0 until impresoraCaracteres) {
            lineasAsterisco = "$lineasAsterisco*"
        }
        agregarTexto(lineasAsterisco)
    }

    fun LineasIgual() {
        var lineasAsterisco = ""
        for (i in 0 until impresoraCaracteres) {
            lineasAsterisco = "$lineasAsterisco="
        }
        agregarTexto(lineasAsterisco)
    }

    fun LineasIgualTexto(texto: String) {
        var textoProcesado = ""
        var i = 0
        while (i < texto.length) {
            var text: String
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
                var aux = ""
                var aux1 = ""
                var `val` = 1
                for (j in 0 until impresoraCaracteres - text.length) {
                    if (`val` == 1) {
                        aux = "$aux="
                        `val` = 0
                    } else {
                        aux1 = "$aux1="
                        `val` = 1
                    }
                }
                text = aux + text + aux1
            }
            escribirTexto(text)
            i += impresoraCaracteres
        }
    }

    fun LineaGuionTexto(texto: String) {
        var textoProcesado = ""
        var i = 0
        while (i < texto.length) {
            var text = ""
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
                var aux = ""
                for (j in 0 until impresoraCaracteres - text.length) {
                    aux = aux + "_"
                }
                text = text + aux
            }
            escribirTexto(text)
            i += impresoraCaracteres
        }
    }

    fun agregarTexto(texto: String?) {
        var textoProcesado = ""
        var cont = 1
        if (texto != null) {
            for (i in 0 until texto.length) {
                textoProcesado += texto[i]
                if (cont >= impresoraCaracteres) {
                    escribirTexto(textoProcesado)
                    textoProcesado = ""
                    cont = 0
                }
                cont++
            }
        }
        if (textoProcesado != "") {
            escribirTexto(textoProcesado)
        }
    }


    fun agregarCaracteres(NumeroCaracteres: Int, text: String) {
        var text = text
        var caracteresfaltantes = 0
        if (text.length < NumeroCaracteres) {
            caracteresfaltantes = NumeroCaracteres - text.length
            for (i in 0 until caracteresfaltantes) {
                text = "$text "
            }
            escribirTextoSinSalto(text.substring(0, NumeroCaracteres))
        } else {
            escribirTextoSinSalto(text.substring(0, NumeroCaracteres))
        }
    }

    fun agregarCaracteresDerecha(NumeroCaracteres: Int, text: String) {
        var text = text
        var caracteresfaltantes = 0
        var t = ""
        if (text.length < NumeroCaracteres) {
            caracteresfaltantes = NumeroCaracteres - text.length
            for (i in 0 until caracteresfaltantes) {
                t = "$t "
            }
            text = t + text
            escribirTextoSinSalto(text.substring(0, NumeroCaracteres))
        } else {
            escribirTextoSinSalto(text.substring(0, NumeroCaracteres))
        }
    }

    fun retornarCaracteresDerecha(NumeroCaracteres: Int, text: String): String? {
        var text = text
        var caracteresfaltantes = 0
        var t = ""
        return if (text.length < NumeroCaracteres) {
            caracteresfaltantes = NumeroCaracteres - text.length
            for (i in 0 until caracteresfaltantes) {
                t = "$t "
            }
            text = t + text
            text.substring(0, NumeroCaracteres)
        } else {
            text.substring(0, NumeroCaracteres)
        }
    }

    fun agregarTextoUnaLinea(texto: String) {
        var textoProcesado = ""
        var i = 0
        while (i < texto.length) {
            var text = ""
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
            }
            escribirTexto(text)
            return
            i += impresoraCaracteres
        }
    }

    fun agregarTextoDosLineas(texto: String) {
        var textoProcesado = ""
        var j = 0
        var i = 0
        while (i < texto.length) {
            var text = ""
            if (texto.length > textoProcesado.length + impresoraCaracteres) {
                text = texto.substring(i, impresoraCaracteres)
                textoProcesado += text
            } else {
                text = if (textoProcesado != "") {
                    texto.replace(textoProcesado, "")
                } else {
                    texto
                }
            }
            escribirTexto(text)
            j++
            if (j == 2) {
                return
            }
            i += impresoraCaracteres
        }
    }

    fun alineadoIzquierda() {
        escribirTextoSinSalto("\u001B" + "\u0061" + "\u0000")
    }

    fun alineadoDerecha() {
        escribirTextoSinSalto("\u001B" + "\u0061" + "\u0002")
    }

    fun alineadoCentro() {
        escribirTextoSinSalto("\u001B" + "\u0061" + "\u0001")
    }

    fun escribirTexto(texto: String) {
        trabajo += """
             $texto
             
             """.trimIndent()
    }

    fun escribirTextoSinSalto(texto: String) {
        trabajo += texto
    }

    fun agregarSalto() {
        trabajo += "\n"
    }

    fun formatoText(texto: String): String? {
        var msg = texto
        msg = msg.replace("Á", "A")
        msg = msg.replace("É", "E")
        msg = msg.replace("Í", "I")
        msg = msg.replace("Ó", "O")
        msg = msg.replace("Ú", "U")
        return msg
    }

    fun lineaCierre(
        cantidad: String?,
        fPago: String,
        Total: String,
        caracteresDisponibles: Int
    ): String {
        //Cantidad
        var fPago = fPago
        var Total = Total
        var _cantidad = BigDecimal(cantidad).setScale(2, RoundingMode.HALF_UP).toString()
        _cantidad = "$_cantidad "
        //Precio Unitario
        var _Total = ""
        Total = BigDecimal(Total).setScale(2, RoundingMode.HALF_UP).toString()
        if (Total.length < 5) {
            val faltante = 5 - Total.length
            for (i in 0 until faltante) {
                _Total = "$_Total "
            }
            _Total = "$_Total$Total "
        } else {
            _Total = "$Total "
        }
        val caracteresDiponibles = caracteresDisponibles - (_cantidad.length + _Total.length) - 1
        //Descripcion
        var _detalle = ""
        if (fPago.length <= caracteresDiponibles) {
            val caracteresFaltantes = caracteresDiponibles - fPago.length
            for (i in 0 until caracteresFaltantes) {
                fPago = "$fPago "
            }
            _detalle = "$fPago "
        } else {
            _detalle = fPago.substring(0, caracteresDiponibles)
            _detalle = "$_detalle "
        }
        return _cantidad + _detalle + _Total
    }

    @Throws(JSONException::class)
    fun lineaVenta(
        cantidad: String?,
        detalle: String,
        precioU: String,
        Total: String,
        impuesto: JSONArray,
        caracteresDisponibles: Int
    ): String {
        //Cantidad
        var detalle = detalle
        var precioU = precioU
        var Total = Total
        var _cantidad = BigDecimal(cantidad).setScale(2, RoundingMode.HALF_UP).toString()
        _cantidad = "$_cantidad "
        //Precio Unitario
        var _precioU = ""
        precioU = BigDecimal(precioU).setScale(2, RoundingMode.HALF_UP).toString()
        if (precioU.length < 5) {
            val faltante = 5 - precioU.length
            for (i in 0 until faltante) {
                _precioU = "$_precioU "
            }
            _precioU = "$_precioU$precioU "
        } else {
            _precioU = "$precioU "
        }
        //Precio Total
        var _total = ""
        Total = BigDecimal(Total).setScale(2, RoundingMode.HALF_UP).toString()
        if (Total.length < 5) {
            val faltante = 5 - Total.length
            for (i in 0 until faltante) {
                _total = "$_total "
            }
            _total = _total + Total
        } else {
            _total = Total
        }
        //Impuesto
        var _impuesto = " "
        var jsonObject: JSONObject
        val items = impuesto.length()
        for (j in 0 until items) {
            jsonObject = impuesto.getJSONObject(j)
            val aux = BigDecimal(jsonObject.getString("porcenjeDim"))
            if (aux.compareTo(BigDecimal.ZERO) == 1) {
                _impuesto = "\u00DC"
            }
        }
        val caracteresDiponibles =
            caracteresDisponibles - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        //Descripcion
        var _detalle = ""
        if (detalle.length <= caracteresDiponibles) {
            val caracteresFaltantes = caracteresDiponibles - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else {
            _detalle = detalle.substring(0, caracteresDiponibles)
            _detalle = "$_detalle "
        }
        return _cantidad + _detalle + _precioU + _total + _impuesto
    }

    @Throws(IOException::class, JSONException::class, InterruptedException::class)
    fun lineaVentaDobleR(
        cantidad: String?,
        detalle: String,
        precioU: String,
        Total: String,
        impuesto: JSONObject?,
        caracteresDisponibles: Int
    ): String? {
        //Cantidad
        var detalle = detalle
        var precioU = precioU
        var Total = Total
        var _cantidad = BigDecimal(cantidad).setScale(2, RoundingMode.HALF_UP).toString()
        _cantidad = "$_cantidad "
        //Precio Unitario
        var _precioU = ""
        precioU = BigDecimal(precioU).setScale(2, RoundingMode.HALF_UP).toString()
        if (precioU.length < 6) {
            val faltante = 6 - precioU.length
            for (i in 0 until faltante) {
                _precioU = "$_precioU "
            }
            _precioU = "$_precioU$precioU "
        } else {
            _precioU = "$precioU "
        }
        //Precio Total
        var _total = ""
        Total = BigDecimal(Total).setScale(2, RoundingMode.HALF_UP).toString()
        if (Total.length < 6) {
            val faltante = 6 - Total.length
            for (i in 0 until faltante) {
                _total = "$_total "
            }
            _total = _total + Total
        } else {
            _total = Total
        }
        //Impuesto
        val _impuesto = ""
        val caracteresDiponibles =
            caracteresDisponibles * 2 - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        val caracteresDis =
            caracteresDisponibles - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        //Descripcion
        var _detalle = ""
        if (detalle.length <= caracteresDis) {
            val caracteresFaltantes = caracteresDis - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else if (detalle.length <= caracteresDiponibles) {
            val caracteresFaltantes = caracteresDiponibles - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else {
            _detalle = detalle.substring(0, caracteresDiponibles)
            _detalle = "$_detalle "
        }
        return _cantidad + _detalle + _precioU + _total + _impuesto
    }

    @Throws(JSONException::class)
    fun lineaVentaDoble(
        cantidad: String,
        detalle: String,
        precioU: String,
        Total: String,
        impuesto: JSONArray,
        caracteresDisponibles: Int
    ): String {
        //Cantidad
        var detalle = detalle
        var precioU = precioU
        var Total = Total
        var _cantidad = BigDecimal(cantidad).setScale(2, RoundingMode.HALF_UP).toString()
        _cantidad = "$_cantidad "
        //Precio Unitario
        var _precioU = ""
        precioU = BigDecimal(precioU).setScale(2, RoundingMode.HALF_UP).toString()
        if (precioU.length < 5) {
            val faltante = 5 - precioU.length
            for (i in 0 until faltante) {
                _precioU = "$_precioU "
            }
            _precioU = "$_precioU$precioU "
        } else {
            _precioU = "$precioU "
        }
        //Precio Total
        var _total = ""
        Total = BigDecimal(Total).setScale(2, RoundingMode.HALF_UP).toString()
        if (Total.length < 6) {
            val faltante = 6 - Total.length
            for (i in 0 until faltante) {
                _total = "$_total "
            }
            _total = _total + Total
        } else {
            _total = Total
        }
        //Impuesto
        var _impuesto = " "
        var jsonObject: JSONObject
        val items = impuesto.length()
        for (j in 0 until items) {
            jsonObject = impuesto.getJSONObject(j)
            var rev = false
            if (jsonObject.has("porcenjeDim")) {
                if (jsonObject["porcenjeDim"] != null) {
                    rev = true
                }
            }
            if (rev) {
                val aux = BigDecimal(jsonObject.getString("porcenjeDim"))
                if (aux.compareTo(BigDecimal.ZERO) == 1) {
                    _impuesto = "\u00DC"
                }
            }
        }
        val caracteresDiponibles =
            caracteresDisponibles * 2 - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        val caracteresDis =
            caracteresDisponibles - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        //Descripcion
        var _detalle = ""
        if (detalle.length <= caracteresDis) {
            val caracteresFaltantes = caracteresDis - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else if (detalle.length <= caracteresDiponibles) {
            val caracteresFaltantes = caracteresDiponibles - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else {
            _detalle = detalle.substring(0, caracteresDiponibles)
            _detalle = "$_detalle "
        }
        return _cantidad + _detalle + _precioU + _total + _impuesto
    }

    @Throws(JSONException::class)
    fun lineaVentaDobleResumida(
        cantidad: String?,
        detalle: String,
        precioU: String?,
        Total: String?,
        impuesto: JSONArray,
        caracteresDisponibles: Int
    ): String {
        //Cantidad
        var detalle = detalle
        var _cantidad = BigDecimal(cantidad).setScale(2, RoundingMode.HALF_UP).toString()
        _cantidad = "$_cantidad   "
        //Precio Unitario
        val _precioU = ""

        //Precio Total
        val _total = ""

        //Impuesto
        var _impuesto = " "
        var jsonObject: JSONObject
        val items = impuesto.length()
        for (j in 0 until items) {
            jsonObject = impuesto.getJSONObject(j)
            val aux = BigDecimal(jsonObject.getString("porcenjeDim"))
            if (aux.compareTo(BigDecimal.ZERO) == 1) {
                _impuesto = "\u00DC"
            }
        }
        val caracteresDiponibles =
            caracteresDisponibles * 2 - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        val caracteresDis =
            caracteresDisponibles - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        //Descripcion
        var _detalle = ""
        if (detalle.length < caracteresDis) {
            val caracteresFaltantes = caracteresDis - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else if (detalle.length < caracteresDiponibles) {
            val caracteresFaltantes = caracteresDiponibles - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else {
            _detalle = detalle.substring(0, caracteresDiponibles)
            _detalle = "$_detalle "
        }
        return _cantidad + _detalle + _precioU + _total + _impuesto
    }

    fun lineaDobleDetalleComanda(
        signo: String,
        cantidad: String?,
        detalle: String,
        observacion: String,
        caracteresDisponibles: Int
    ): String? {
        //Cantidad
        var detalle = detalle
        var _cantidad = BigDecimal(cantidad).setScale(2, RoundingMode.HALF_UP).toString()
        _cantidad = "$_cantidad   "
        //Precio Unitario
        val _precioU = ""

        //Precio Total
        val _total = ""

        //Impuesto
        val _impuesto = " "
        val caracteresDiponibles =
            caracteresDisponibles * 2 - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        val caracteresDis =
            caracteresDisponibles - (_cantidad.length + _precioU.length + _total.length + _impuesto.length) - 1
        //Descripcion
        var _detalle = ""
        if (observacion != "") {
            detalle = "$detalle ($observacion)"
        }
        if (detalle.length < caracteresDis) {
            val caracteresFaltantes = caracteresDis - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else if (detalle.length < caracteresDiponibles) {
            val caracteresFaltantes = caracteresDiponibles - detalle.length
            for (i in 0 until caracteresFaltantes) {
                detalle = "$detalle "
            }
            _detalle = "$detalle "
        } else {
            _detalle = detalle.substring(0, caracteresDiponibles)
            _detalle = "$_detalle "
        }
        return "$signo $_cantidad$_detalle"
    }
}