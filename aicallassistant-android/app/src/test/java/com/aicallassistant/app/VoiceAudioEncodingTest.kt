package com.aicallassistant.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class VoiceAudioEncodingTest {

    @Test
    fun testPcm16AudioChunkBase64Encoding() {
        // 100 ms of 16kHz 16-bit mono PCM = 1600 samples = 3200 bytes
        val pcmBytes = ByteArray(3200) { (it % 128).toByte() }
        val base64Encoded = Base64.getEncoder().encodeToString(pcmBytes)

        assertTrue(base64Encoded.isNotBlank())
        val decoded = Base64.getDecoder().decode(base64Encoded)
        assertArrayEquals(pcmBytes, decoded)
    }

    @Test
    fun testPcm24AudioChunkBase64Decoding() {
        // 100 ms of 24kHz 16-bit mono PCM = 2400 samples = 4800 bytes
        val originalBytes = ByteArray(4800) { ((it * 3) % 256).toByte() }
        val b64 = Base64.getEncoder().encodeToString(originalBytes)

        val decoded = Base64.getDecoder().decode(b64)
        assertEquals(4800, decoded.size)
        assertArrayEquals(originalBytes, decoded)
    }

    @Test
    fun testCorruptBase64HandledGracefully() {
        val corrupt = "%%%NotValidBase64@@@"
        val result = try {
            Base64.getDecoder().decode(corrupt)
        } catch (_: IllegalArgumentException) {
            null
        }
        assertEquals(null, result)
    }
}
