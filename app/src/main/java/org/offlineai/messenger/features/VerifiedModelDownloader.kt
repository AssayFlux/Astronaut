package org.offlineai.messenger.features

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

data class DownloadedModel(val file: File, val sha256: String, val bytes: Long)

/**
 * Downloads only over HTTPS and verifies the publisher-provided SHA-256 before exposing
 * the file to the model library. Redirects are limited and may never downgrade to HTTP.
 */
class VerifiedModelDownloader(context: Context) {
    private val modelDirectory = File(context.filesDir, "models").apply { mkdirs() }

    suspend fun download(
        sourceUrl: String,
        expectedSha256: String,
        requestedFileName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
    ): DownloadedModel = withContext(Dispatchers.IO) {
        require(expectedSha256.matches(Regex("[A-Fa-f0-9]{64}"))) { "Enter a valid 64-character SHA-256 checksum." }
        val safeName = requestedFileName.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        require(safeName.endsWith(".litertlm", ignoreCase = true)) { "The downloaded model must use the .litertlm extension." }
        require(safeName.isNotBlank()) { "A model filename is required." }

        var currentUrl = URL(sourceUrl)
        require(currentUrl.protocol.equals("https", true)) { "Model downloads require HTTPS." }
        val temporary = File.createTempFile("astronaut-model-", ".part", modelDirectory)
        var connection: HttpsURLConnection? = null
        try {
            var redirects = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                val candidate = currentUrl.openConnection() as? HttpsURLConnection
                    ?: error("Only HTTPS model download endpoints are allowed.")
                connection = candidate
                candidate.connectTimeout = 20_000
                candidate.readTimeout = 45_000
                candidate.instanceFollowRedirects = false
                candidate.setRequestProperty("User-Agent", "Astronaut-Offline-AI/1.0")
                candidate.connect()
                val status = candidate.responseCode
                if (status in 300..399) {
                    val location = candidate.getHeaderField("Location") ?: error("The server returned a redirect without a destination.")
                    require(redirects++ < 3) { "Too many model download redirects." }
                    val redirected = URL(currentUrl, location)
                    require(redirected.protocol.equals("https", true)) { "Redirects to non-HTTPS destinations are blocked." }
                    candidate.disconnect()
                    connection = null
                    currentUrl = redirected
                    continue
                }
                require(status == HttpURLConnection.HTTP_OK) { "Download failed with HTTP status $status." }
                val total = candidate.contentLengthLong.takeIf { it > 0 }
                if (total != null) require(total <= MAX_BYTES) { "Model exceeds the 8 GiB download limit." }
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                candidate.inputStream.use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            downloaded += count
                            require(downloaded <= MAX_BYTES) { "Model exceeds the 8 GiB download limit." }
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            if (downloaded % PROGRESS_INTERVAL < count) {
                                val progress = downloaded
                                withContext(Dispatchers.Main) { onProgress(progress, total) }
                            }
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual.equals(expectedSha256, ignoreCase = true)) {
                    "SHA-256 mismatch. Expected $expectedSha256 but received $actual. The file was discarded."
                }
                val target = File(modelDirectory, actual.take(16) + "-" + safeName)
                if (target.exists()) temporary.delete()
                else check(temporary.renameTo(target)) { "Could not move the verified model into private storage." }
                withContext(Dispatchers.Main) { onProgress(downloaded, total ?: downloaded) }
                return@withContext DownloadedModel(target, actual, downloaded)
            }
            error("Unreachable download state")
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val MAX_BYTES = 8L * 1024L * 1024L * 1024L
        private const val PROGRESS_INTERVAL = 512L * 1024L
    }
}
