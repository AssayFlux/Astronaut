package org.offlineai.messenger.features

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.io.Closeable
import java.util.Locale

/**
 * Uses Android's on-device speech recognizer only (API 31+) and a locally installed
 * TTS voice. It fails closed rather than falling back to network speech services.
 */
class OfflineVoiceController(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit = {}
) : Closeable {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var offlineVoice: Voice? = null
    private var closed = false

    init {
        textToSpeech = TextToSpeech(appContext) { status ->
            if (closed) return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                onError("Text-to-speech could not be initialized.")
                return@TextToSpeech
            }
            val voices = textToSpeech?.voices.orEmpty()
                .filterNot { it.isNetworkConnectionRequired }
            offlineVoice = voices.firstOrNull { it.locale.language == Locale.getDefault().language }
                ?: voices.firstOrNull()
            if (offlineVoice == null) {
                onError("No offline speech voice is installed. Install an offline voice in Android system settings.")
            } else {
                textToSpeech?.voice = offlineVoice
            }
        }
    }

    fun startListening() {
        if (closed) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onError("Offline speech recognition requires Android 12 or newer on this device.")
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)) {
            onError("This device has no on-device speech recognition service installed.")
            return
        }
        try {
            val service = recognizer ?: SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext).also {
                it.setRecognitionListener(listener)
                recognizer = it
            }
            service.cancel()
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            service.startListening(request)
        } catch (error: Throwable) {
            onListeningChanged(false)
            onError("Could not start offline speech recognition: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        onListeningChanged(false)
    }

    fun speak(text: String) {
        if (closed || text.isBlank()) return
        val engine = textToSpeech ?: run {
            onError("Speech output is not ready yet.")
            return
        }
        val voice = offlineVoice ?: run {
            onError("No offline speech voice is installed.")
            return
        }
        engine.voice = voice
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "astronaut-offline-speech")
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onListeningChanged(true) }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { onListeningChanged(false) }
        override fun onError(error: Int) {
            onListeningChanged(false)
            onError(
                when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was cancelled."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "The on-device recognizer reported a network error; no network fallback was requested."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognised. Try again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Try again shortly."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected."
                    else -> "Offline speech recognition failed (code $error)."
                }
            )
        }
        override fun onResults(results: Bundle?) {
            onListeningChanged(false)
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isBlank()) onError("No speech was recognised.") else onResult(text)
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
        runCatching { textToSpeech?.stop() }
        runCatching { textToSpeech?.shutdown() }
        textToSpeech = null
        offlineVoice = null
    }
}
