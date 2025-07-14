package com.example.printerswanqara.core.document

class documentType(){
    private val documentMap : Map<String,String> = mapOf(
        "IMPRESION_RECIBO" to "Recibos",
        "IMPRESION_FACTURA_ELECTRONICA" to "Factura electronica",
        "IMPRESION_PRE_TICKET" to "Pretickets",
        "IMPRESION_COMANDA:COCINA" to "Comandas cocina",
        "IMPRESION_COMANDA:BARRA" to "Comandas barra",
        "IMPRESION_COMANDA:OTROS" to "Comandas otros",
        "IMPRESION_COTIZACION" to "Cotizaciones",
       // "IMPRESION_COTIZACION_RESUMIDA" to "Cotizaciones resumidas",
        "APERTURA_GAVETA" to "Gaveta de dinero",
        "IMPRESION_CIERRE_CAJA" to "Cierre de caja"
    )

    fun getDocuments():MutableList<String>{
        val list:MutableList<String> = mutableListOf()
        this.documentMap.map { document ->
             list.add(document.value)
        }
        return list
    }
    fun findDocumentByKey(key: String): String? {
        return documentMap[key]
    }
    fun findKeyByDocument(document:String): String? {
        return this.documentMap.filterValues { it == document }.keys.first().toString()
    }
}
