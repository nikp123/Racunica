package com.github.nikp123.racunica.util

import java.net.URI

class URIUtils {
    private val uri: URI?

    constructor(uri: URI) {
        this.uri = uri
    }

    fun parseQuery(): MutableMap<String?, String?> {
        val query = uri?.query

        val queryParams: MutableMap<String?, String?> = HashMap()
        if (query != null) {
            val pairs = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (pair in pairs) {
                val keyValue =
                    pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val key: String? = keyValue[0]
                val value = if (keyValue.size > 1) keyValue[1] else null
                queryParams.put(key, value)
            }
        }
        return queryParams
    }
}