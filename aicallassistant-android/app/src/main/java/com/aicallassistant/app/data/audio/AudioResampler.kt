package com.aicallassistant.app.data.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance audio resampler for 16-bit mono PCM buffers.
 * Converts 24kHz assistant audio to 16kHz for uniform local call recording.
 */
object AudioResampler {

    /**
     * Resamples 16-bit signed PCM mono audio from [inRate] to [outRate] using linear interpolation.
     */
    fun resamplePcm16Mono(pcmBytes: ByteArray, inRate: Int, outRate: Int): ByteArray {
        if (inRate == outRate || pcmBytes.isEmpty()) {
            return pcmBytes
        }

        val inSamplesCount = pcmBytes.size / 2
        if (inSamplesCount == 0) return ByteArray(0)

        val inBuffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val inSamples = ShortArray(inSamplesCount)
        inBuffer.get(inSamples)

        val outSamplesCount = (inSamplesCount.toLong() * outRate / inRate).toInt()
        val outSamples = ShortArray(outSamplesCount)
        val ratio = inRate.toDouble() / outRate.toDouble()

        for (i in 0 until outSamplesCount) {
            val origIdx = i * ratio
            val idx = origIdx.toInt()
            val frac = origIdx - idx

            val sample = if (idx + 1 < inSamplesCount) {
                val s0 = inSamples[idx].toFloat()
                val s1 = inSamples[idx + 1].toFloat()
                (s0 * (1f - frac) + s1 * frac).toInt()
            } else {
                inSamples[minOf(idx, inSamplesCount - 1)].toInt()
            }
            outSamples[i] = sample.coerceIn(-32768, 32767).toShort()
        }

        val outBytes = ByteArray(outSamplesCount * 2)
        ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outSamples)
        return outBytes
    }

    /**
     * Resamples 24,000 Hz 16-bit PCM (from Gemini Live output) to 16,000 Hz PCM.
     */
    fun resample24kTo16k(pcmBytes: ByteArray): ByteArray {
        return resamplePcm16Mono(pcmBytes, inRate = 24000, outRate = 16000)
    }
}
