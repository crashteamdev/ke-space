package dev.crashteam.repricer.service.encryption

interface PasswordEncryptor {
    fun encryptPassword(password: String): ByteArray

    fun decryptPassword(password: ByteArray): String
}
