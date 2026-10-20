package com.aicallassistant.app

import com.aicallassistant.app.data.audio.AudioResampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioResamplerTest {

    @Test
    fun testEmptyBytesReturnsEmpty() {
        val res = AudioResampler.resample24kTo16k(ByteArray(0))
        assertEquals(0, res.size)
    }

    @Test
    fun testSameRateReturnsOriginal() {
        val original = ByteArray(100) { it.toByte() }
        val res = AudioResampler.resamplePcm16Mono(original, 16000, 16000)
        assertEquals(original.size, res.size)
    }

    @Test
    fun testResample24kTo16kProportion() {
        // 24000 samples = 48000 bytes for 1 second of 24kHz 16-bit mono
        val samples24kCount = 24000
        val bytes24k = ByteArray(samples24kCount * 2)

        // Fill with a 440Hz sine wave
        val bb = ByteBuffer.wrap(bytes24k).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until samples24kCount) {
            val angle = 2.0 * Math.PI * 440.0 * i / 24000.0
            val sample = (Math.sin(angle) * 16000.0).toInt().toShort()
            bb.putShort(sample)
        }

        val resampled = AudioResampler.resample24kTo16k(bytes24k)

        // 16000 samples = 32000 bytes for 1 second of 16kHz
        assertEquals(16000 * 2, resampled.size)
    }
}
