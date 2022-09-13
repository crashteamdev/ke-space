package dev.crashteam.repricer.client.ke

class KazanExpressClientException(status: Int, rawResponseBody: String, message: String) : RuntimeException(message)
