package com.example.printerswanqaratest.core.print.utils.printer1

enum class TransactionType(private var nombreVista: String, private var nombreComando: String) {
    IMPRESION_RECIBO("Impresora de Recibos", "IMPRESION_RECIBO"),
    IMPRESION_PRE_TICKET("Impresora de Pretickets", "IMPRESION_PRE_TICKET"),
    IMPRESION_COMANDA_COCINA("Impresora de Comandas Cocina", "IMPRESION_COMANDA:COCINA"),
    IMPRESION_COMANDA_BARRA("Impresora de Comandas Barra", "IMPRESION_COMANDA:BARRA"),
    IMPRESION_COMANDA_OTRO("Impresora de Comandas Otro", "IMPRESION_COMANDA:OTROS"),
    IMPRESION_FACTURA_PRE_IMPRESA(
        "Impresora de Facturas Preimpresas",
        "IMPRESION_FACTURA_PRE_IMPRESA"
    ),
    IMPRESION_FACTURA_ELECTRONICA(
        "Impresora de Facturas Electrónicas",
        "IMPRESION_FACTURA_ELECTRONICA"
    ),
    IMPRESION_COTIZACION_DETALLADA(
        "Impresora de Cotizaciones Detalladas",
        "IMPRESION_COTIZACION_DETALLADA"
    ),
    IMPRESION_COTIZACION_RESUMIDA(
        "Impresora de Cotizaciones Resumidas",
        "IMPRESION_COTIZACION_RESUMIDA"
    ),
    APERTURA_GAVETA("Gaveta de Dinero", "APERTURA_GAVETA"),
    IMPRESION_CIERRE_CAJA("Impresora de Cierres de Caja", "IMPRESION_CIERRE_CAJA");


    open fun getNombreVista(): String? {
        return nombreVista
    }

    open fun getNombreComando(): String? {
        return nombreComando
    }

    override fun toString(): String {
        return nombreVista
    }
}