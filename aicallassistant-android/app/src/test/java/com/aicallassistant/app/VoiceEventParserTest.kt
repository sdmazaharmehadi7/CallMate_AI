package com.aicallassistant.app

import com.aicallassistant.app.data.remote.VoiceEventParser
import com.aicallassistant.app.data.remote.VoiceServerEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEventParserTest {

    @Test
    fun testParseCallStarted() {
        val json = """{"event":"call_started","call_id":"c123","model":"gemini-3.8-live","voice":"Puck"}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.CallStarted)
        val started = event as VoiceServerEvent.CallStarted
        assertEquals("c123", started.callId)
        assertEquals("gemini-3.8-live", started.model)
        assertEquals("Puck", started.voice)
    }

    @Test
    fun testParseAudioOut() {
        val json = """{"event":"audio_out","data":"AQIDBA==","timestamp_ms":1500}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.AudioOut)
        val audioOut = event as VoiceServerEvent.AudioOut
        assertEquals("AQIDBA==", audioOut.data)
        assertEquals(1500L, audioOut.timestampMs)
    }

    @Test
    fun testParseTranscriptDelta() {
        val json = """{"event":"transcript_delta","speaker":"assistant","text":"Hello there!","is_final":true,"timestamp_ms":2000}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.TranscriptDelta)
        val delta = event as VoiceServerEvent.TranscriptDelta
        assertEquals("assistant", delta.speaker)
        assertEquals("Hello there!", delta.text)
        assertTrue(delta.isFinal)
        assertEquals(2000L, delta.timestampMs)
    }

    @Test
    fun testParseInterrupted() {
        val json = """{"event":"interrupted"}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.Interrupted)
    }

    @Test
    fun testParseCallEnded() {
        val json = """{"event":"call_ended","call_id":"c123","summary":"Courier delivered food.","duration_seconds":42,"key_facts":["Left at doorstep"],"action_items":["Collect package"],"urgency":"low","urgency_explanation":"Food arrived"}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.CallEnded)
        val ended = event as VoiceServerEvent.CallEnded
        assertEquals("c123", ended.callId)
        assertEquals("Courier delivered food.", ended.summary)
        assertEquals(42, ended.durationSeconds)
        assertEquals(listOf("Left at doorstep"), ended.keyFacts)
        assertEquals(listOf("Collect package"), ended.actionItems)
        assertEquals("low", ended.urgency)
        assertEquals("Food arrived", ended.urgencyExplanation)
    }

    @Test
    fun testParseError() {
        val json = """{"event":"error","code":"GEMINI_NOT_CONFIGURED","message":"Key missing"}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.StreamError)
        val err = event as VoiceServerEvent.StreamError
        assertEquals("GEMINI_NOT_CONFIGURED", err.code)
        assertEquals("Key missing", err.message)
    }

    @Test
    fun testParseMalformedJsonSafe() {
        val json = """not a json string{{{"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.Unknown)
        assertEquals("not a json string{{{", (event as VoiceServerEvent.Unknown).raw)
    }

    @Test
    fun testParseUnknownEventSafe() {
        val json = """{"event":"some_future_unsupported_event","data":123}"""
        val event = VoiceEventParser.parse(json)
        assertTrue(event is VoiceServerEvent.Unknown)
    }

    @Test
    fun testParseEmptyStringSafe() {
        val event = VoiceEventParser.parse("")
        assertTrue(event is VoiceServerEvent.Unknown)
    }
}
