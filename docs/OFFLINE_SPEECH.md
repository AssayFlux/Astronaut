# Offline speech input and output

Astronaut's voice path is fail-closed: it never deliberately falls back to a remote speech recognizer or network TTS voice.

## Speech-to-text (ASR)

- Uses `SpeechRecognizer.createOnDeviceSpeechRecognizer` only (Android 12 / API 31+).
- Uses `RecognizerIntent.EXTRA_PREFER_OFFLINE=true` and a selected BCP-47 language tag.
- Does not use the generic `createSpeechRecognizer` cloud-capable factory.
- On Android 13 / API 33+, Settings can inspect installed, downloadable and pending on-device language packs with `checkRecognitionSupport`.
- Android 13+ can request a language pack download with `triggerModelDownload`. Android 14+ reports progress, success, scheduled and error callbacks. Android 13 reports that a request was submitted; refresh support after the system completes it.
- On Android 12, on-device recognition can be used when the platform service and language pack are already installed, but the public pack discovery/download APIs are not available.

The OS speech provider controls model distribution and actual language support. Astronaut does not bundle recognizer weights. A language shown as supported may still need a download; use **Check packs** after a download request and confirm it appears under installed languages before relying on offline speech.

## Text-to-speech (TTS)

- Enumerates installed Android TTS voices and filters out voices for which `Voice.isNetworkConnectionRequired` is true.
- Allows choosing an installed local voice and persists the selected voice name and language tag.
- Speaks only with the selected offline voice. It does not select a network-required voice as a fallback.
- If no offline voice exists, Settings and chat report that the user must install one through Android's text-to-speech settings.

TTS voice packages are installed/managed by the Android TTS engine. Astronaut does not claim to download third-party TTS model files itself.

## Settings

Open **Settings → Offline voice**:
1. Enter a BCP-47 language tag (for example `en-US`, `hi-IN`, or `bn-IN`).
2. Apply the language.
3. On Android 13+, select **Check packs** to list installed, downloadable and pending on-device recognizer languages.
4. If the chosen language is downloadable, select **Download pack**, approve any system prompt, then check pack status again.
5. Choose one of the available offline TTS voices and test it.

The selected ASR language and TTS voice are stored in app preferences. No audio recording is persisted by this voice controller.

## Privacy and limitations

- Microphone permission is requested only when the user taps the microphone.
- Speech recognition is created with the on-device-only Android API; the app does not intentionally invoke remote recognition.
- Android system speech providers and TTS engines are separate system components. Their offline behavior and available packs vary by manufacturer, OS version and installed service.
- The API's on-device factory and offline voice metadata are the strongest public capability checks available to the app, but physical airplane-mode testing is still required to verify a specific OEM implementation.
- On-device speech is available from API 31. Pack discovery/download controls require API 33. App minSdk remains 26; older devices receive a clear unsupported message rather than cloud fallback.
- Accuracy, latency, microphone routing, Bluetooth headsets, interruption handling and TTS quality require physical-device validation.

## Platform references

- [SpeechRecognizer API](https://developer.android.com/reference/android/speech/SpeechRecognizer): on-device recognizer, support checks and model download requests.
- [RecognizerIntent API](https://developer.android.com/reference/android/speech/RecognizerIntent): language tag and offline preference.
- [TextToSpeech API](https://developer.android.com/reference/android/speech/tts/TextToSpeech): installed voices.
- [Voice API](https://developer.android.com/reference/android/speech/tts/Voice): network-connection requirement metadata.
