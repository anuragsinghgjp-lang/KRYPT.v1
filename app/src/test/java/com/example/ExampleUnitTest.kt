package com.example

import com.example.data.local.GhostModeState
import com.example.data.model.MessageType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ExampleUnitTest {
    @Test
    fun ghostModeManualAndTimedExpiryWorkCorrectly() {
        val off = GhostModeState(enabled = false, expiresAtEpochMs = 0L)
        assertFalse(off.isEffectivelyActive(1000L))

        val manual = GhostModeState(enabled = true, expiresAtEpochMs = 0L)
        assertTrue(manual.isEffectivelyActive(999_999_999L))

        val activeTimed = GhostModeState(enabled = true, expiresAtEpochMs = 2000L)
        assertTrue(activeTimed.isEffectivelyActive(1500L))
        assertFalse(activeTimed.isEffectivelyActive(2000L))
    }

    @Test
    fun messageTypeSafeFallbackHandlesUnknownTypes() {
        assertEquals(MessageType.IMAGE, MessageType.safeValueOf("IMAGE"))
        assertEquals(MessageType.TEXT, MessageType.safeValueOf("UNKNOWN_FUTURE_TYPE"))
    }

    @Test
    fun normalizedAccessPassHashingIsDeterministic() {
        fun hash(code: String): String {
            val norm = code.trim().uppercase().toByteArray(StandardCharsets.UTF_8)
            return MessageDigest.getInstance("SHA-256").digest(norm)
                .joinToString("") { "%02x".format(it) }
        }
        assertEquals(hash("  krypt-abcd-1234-efgh "), hash("KRYPT-ABCD-1234-EFGH"))
        assertEquals(64, hash("KRYPT-ABCD-1234-EFGH").length)
        assertNotEquals(hash("KRYPT-ABCD-1234-EFGH"), hash("KRYPT-ABCD-1234-EFGI"))
    }

    @Test
    fun ecdhP256HkdfAes256GcmDirectAndGroupKeyExchangeRoundtrip() {
        val random = SecureRandom()
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), random)

        val aliceId = kpg.generateKeyPair()
        val bobId = kpg.generateKeyPair()
        val ephemeral = kpg.generateKeyPair()

        fun hkdf(ikm: ByteArray, info: String): ByteArray {
            val mac1 = Mac.getInstance("HmacSHA256")
            mac1.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
            val prk = mac1.doFinal(ikm)
            val mac2 = Mac.getInstance("HmacSHA256")
            mac2.init(SecretKeySpec(prk, "HmacSHA256"))
            mac2.update(info.toByteArray(StandardCharsets.UTF_8))
            mac2.update(1.toByte())
            return mac2.doFinal().copyOf(32)
        }

        // Sender derives wrap key for Bob
        val kaSender = KeyAgreement.getInstance("ECDH").apply {
            init(ephemeral.private)
            doPhase(bobId.public, true)
        }
        val senderWrapKey = hkdf(kaSender.generateSecret(), "KRYPT-DM-WRAP")

        // Recipient Bob derives exact same wrap key from ephemeral public key
        val kaRecipient = KeyAgreement.getInstance("ECDH").apply {
            init(bobId.private)
            doPhase(ephemeral.public, true)
        }
        val recipientWrapKey = hkdf(kaRecipient.generateSecret(), "KRYPT-DM-WRAP")
        assertArrayEquals(senderWrapKey, recipientWrapKey)

        // Wrap content key & encrypt message with AES-256-GCM + AAD
        val contentKey = ByteArray(32).also { random.nextBytes(it) }
        val aad = "dm_alice_bob".toByteArray(StandardCharsets.UTF_8)
        val wrapIv = ByteArray(12).also { random.nextBytes(it) }
        val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(senderWrapKey, "AES"), GCMParameterSpec(128, wrapIv))
            updateAAD(aad)
        }.doFinal(contentKey)

        val msgIv = ByteArray(12).also { random.nextBytes(it) }
        val secretText = "Zero-trust verified payload for KRYPT"
        val msgCipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, msgIv))
            updateAAD(aad)
        }.doFinal(secretText.toByteArray(StandardCharsets.UTF_8))

        // Bob unwraps & decrypts
        val unwrappedKey = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(recipientWrapKey, "AES"), GCMParameterSpec(128, wrapIv))
            updateAAD(aad)
        }.doFinal(wrapCipher)
        assertArrayEquals(contentKey, unwrappedKey)

        val decryptedText = String(
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(unwrappedKey, "AES"), GCMParameterSpec(128, msgIv))
                updateAAD(aad)
            }.doFinal(msgCipher),
            StandardCharsets.UTF_8
        )
        assertEquals(secretText, decryptedText)

        // Ensure wrong identity (Alice trying to use Bob's wrap) fails GCM authentication
        val kaWrong = KeyAgreement.getInstance("ECDH").apply {
            init(aliceId.private)
            doPhase(ephemeral.public, true)
        }
        val wrongKey = hkdf(kaWrong.generateSecret(), "KRYPT-DM-WRAP")
        var authFailed = false
        try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(wrongKey, "AES"), GCMParameterSpec(128, wrapIv))
                updateAAD(aad)
            }.doFinal(wrapCipher)
        } catch (_: Exception) {
            authFailed = true
        }
        assertTrue(authFailed)
    }
}
