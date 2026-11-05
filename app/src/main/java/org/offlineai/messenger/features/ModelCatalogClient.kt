package org.offlineai.messenger.features
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Fetches a bounded signed catalog. The trusted public key is build-time configuration. */
class ModelCatalogClient(context: Context, private val catalogUrl: String, private val trustedPublicKeyBase64: String) {
    suspend fun fetch(): VerifiedModelCatalog = withContext(Dispatchers.IO) {
        require(catalogUrl.startsWith("https://")) { "Catalog URL must use HTTPS." }
        require(trustedPublicKeyBase64.isNotBlank()) { "No trusted catalog publisher key is configured in this build." }
        var current = URL(catalogUrl); var redirects = 0
        while (true) {
            val connection = current.openConnection() as? HttpsURLConnection ?: error("Catalog connections must use HTTPS.")
            try {
                connection.connectTimeout = 15_000; connection.readTimeout = 20_000
                connection.instanceFollowRedirects = false; connection.setRequestProperty("Accept", "application/json"); connection.connect()
                if (connection.responseCode in 300..399) {
                    require(redirects++ < 2) { "Too many catalog redirects." }
                    val next = URL(current, connection.getHeaderField("Location") ?: error("Missing redirect location"))
                    require(next.protocol == "https") { "Catalog redirect downgraded from HTTPS." }; current = next; continue
                }
                require(connection.responseCode == HttpURLConnection.HTTP_OK) { "Catalog request failed: HTTP \${connection.responseCode}" }
                val bytes = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
                    while (true) { val count = input.read(buffer); if (count < 0) break; total += count; require(total <= 2 * 1024 * 1024) { "Catalog exceeds 2 MiB." }; output.write(buffer, 0, count) }
                    output.toByteArray()
                }
                return@withContext ModelCatalogParser.parseSigned(bytes, Ed25519CatalogVerifier(trustedPublicKeyBase64))
            } finally { connection.disconnect() }
        }
        error("Unreachable catalog state")
    }
}
