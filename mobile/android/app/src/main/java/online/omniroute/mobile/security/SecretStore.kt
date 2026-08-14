package online.omniroute.mobile.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecretStore(context: Context) {
    private val preferences = context.getSharedPreferences("omniroute_secrets", Context.MODE_PRIVATE)
    private val alias = "omniroute-mobile-aes"

    fun put(key: String, value: String) {
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(key, Base64.getEncoder().encodeToString(iv + ciphertext))
            .apply()
    }

    fun get(key: String): String? {
        val encoded = preferences.getString(key, null) ?: return null
        val payload = Base64.getDecoder().decode(encoded)
        require(payload.size > 12) { "Corrupt secret payload" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return cipher.doFinal(payload.copyOfRange(12, payload.size)).toString(StandardCharsets.UTF_8)
    }

    fun remove(key: String) = preferences.edit().remove(key).apply()

    private fun secretKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }
}
