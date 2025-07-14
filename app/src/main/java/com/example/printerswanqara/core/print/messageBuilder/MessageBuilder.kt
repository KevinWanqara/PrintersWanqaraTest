package com.example.printerswanqara.core.print.messageBuilder

class MessageBuilder(
    private val titleMessage: TitleBuilder,
    private val bodyMessage: BodyBuilder,
    private val mediaMessage: MediaBuilder
) {

    fun getTitleMessage():TitleBuilder{
        return this.titleMessage
    }
    fun getBodyMessage():BodyBuilder{
        return this.bodyMessage
    }
    fun getMediaMessage():MediaBuilder{
        return this.mediaMessage
    }
}