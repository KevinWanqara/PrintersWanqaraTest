package com.printerswanqara.core.print.queue

import android.content.Context
import android.net.Uri
import com.printerswanqara.core.print.utils.schema.SchemaParser
import com.printerswanqara.data.AppStorage
import java.util.Locale

internal data class PrintJobDescriptor(
    val jobType: String,
    val tenant: String
)

internal object PrintJobDescriptorResolver {

    fun resolve(context: Context, uri: Uri): PrintJobDescriptor {
        val tenant = AppStorage.getRuc(context)?.takeIf { it.isNotBlank() } ?: "Sin dominio"
        val jobType = resolveJobType(uri)
        return PrintJobDescriptor(jobType = jobType, tenant = tenant)
    }

    private fun resolveJobType(uri: Uri): String {
        if (SchemaParser.isSendToPrintSchema(uri)) {
            val configs = SchemaParser.parse(uri)
            if (configs.isEmpty()) return "Impresion por esquema"
            if (configs.size > 1) return "Impresion por lote (${configs.size} trabajos)"
            return mapType(configs.first().type)
        }

        val rawCommand = uri.schemeSpecificPart
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.removePrefix("//")
            ?.removePrefix("wanqaraprintermobile://")
            ?.trimStart('/')

        if (rawCommand.isNullOrBlank()) return "Trabajo de impresion"
        return mapType(rawCommand)
    }

    private fun mapType(rawType: String): String {
        return when (rawType.uppercase(Locale.ROOT)) {
            "IMPRESION_FACTURA_ELECTRONICA" -> "Factura electronica"
            "IMPRESION_RECIBO" -> "Recibo"
            "IMPRESION_PRE_TICKET", "PRETICKET" -> "Pre-ticket"
            "IMPRESION_COTIZACION", "COTIZACION" -> "Cotizacion"
            "IMPRESION_CIERRE_CAJA", "CIERRE_CAJA" -> "Cierre de caja"
            "IMPRESION_ABONO_CUENTA" -> "Abono de cuenta"
            "IMPRESION_COMANDA:COCINA", "COMMAND_A" -> "Comanda cocina"
            "IMPRESION_COMANDA:BARRA", "COMMAND_B" -> "Comanda barra"
            "IMPRESION_COMANDA:OTROS", "COMMAND_C" -> "Comanda otros"
            else -> rawType
                .replace('_', ' ')
                .replace(':', ' ')
                .lowercase(Locale.ROOT)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}
