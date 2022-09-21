package dev.crashteam.repricer.client.ke

class KazanExpressProxyClientException(val status: Int, val rawResponseBody: String, message: String) :
    RuntimeException(message)
