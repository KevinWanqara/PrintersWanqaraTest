package com.example.printerswanqara.domain.models

class FaQ(
    private val id: Int,
    private val quest: String,
    private val response: String,
    private val url: String?
) {
    fun getQuest(): String {
        return this.quest
    }

    fun getResponse(): String {
        return this.response
    }

    fun getUrl(): String? {
        return this.url
    }

    fun getId(): Int {
        return this.id
    }
}