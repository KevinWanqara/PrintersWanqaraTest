package com.example.printerswanqara.core.print.messageBuilder

class MediaBuilder(private val mediaMessage: Map<String, List<String>>) {

    fun getMediaMessage():Map<String,List<String>>{
        return this.mediaMessage
    }
}