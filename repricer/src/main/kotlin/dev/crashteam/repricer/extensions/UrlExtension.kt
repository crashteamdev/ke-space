package dev.crashteam.repricer.extensions

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun Map<String, String>.toUrlParams(): String =
    entries.joinToString("&") {
        it.key.toUrlEncoded() + "=" + it.value.toUrlEncoded()
    }

fun String.toUrlEncoded(): String = URLEncoder.encode(
    this, StandardCharsets.UTF_8
)
