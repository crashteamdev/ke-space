package dev.crashteam.repricer.service.encryption

import dev.crashteam.repricer.config.properties.RepricerProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
class AESPasswordEncryptor(
    private val repricerProperties: RepricerProperties
) : PasswordEncryptor {

    override fun encryptPassword(password: String): ByteArray {
        val aesKey: Key = SecretKeySpec(repricerProperties.passwordKey!!.toByteArray(StandardCharsets.UTF_8), "AES")
        val cipher: Cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)

        return cipher.doFinal(password.toByteArray())
    }

    override fun decryptPassword(encryptedPassword: ByteArray): String {
        val aesKey: Key = SecretKeySpec(repricerProperties.passwordKey!!.toByteArray(StandardCharsets.UTF_8), "AES")
        val cipher: Cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, aesKey)

        return String(cipher.doFinal(encryptedPassword))
    }
}
