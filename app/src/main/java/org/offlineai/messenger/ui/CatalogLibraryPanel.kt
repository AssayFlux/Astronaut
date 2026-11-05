package org.offlineai.messenger.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.offlineai.messenger.BuildConfig
import org.offlineai.messenger.features.*
import java.io.File

@Composable
fun CatalogLibraryPanel(onInstalled: (ModelCatalogEntry, File) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(emptyList<ModelCatalogEntry>()) }
    var status by remember { mutableStateOf("Signed publisher catalog is not configured in this build.") }
    var loading by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf<String?>(null) }
    var compatibility by remember { mutableStateOf<Map<String, ModelCompatibility>>(emptyMap()) }
    val configured = BuildConfig.MODEL_CATALOG_URL.isNotBlank() && BuildConfig.MODEL_CATALOG_PUBLIC_KEY.isNotBlank()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Verified model catalog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Only entries from a valid Ed25519-signed catalog are displayed as trusted. Catalog downloads are HTTPS and SHA-256 verified.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (configured) {
            OutlinedButton(onClick = {
                loading = true
                scope.launch {
                    try {
                        val catalog = ModelCatalogClient(context, BuildConfig.MODEL_CATALOG_URL, BuildConfig.MODEL_CATALOG_PUBLIC_KEY).fetch()
                        entries = catalog.models
                        val checker = ModelCompatibilityChecker(context)
                        compatibility = entries.associate { it.id to checker.check(it) }
                        status = "Verified \${entries.size} models · catalog SHA-256 \${catalog.payloadSha256.take(16)}…"
                    } catch (e: Throwable) {
                        status = "Catalog verification failed: \${e.message ?: e.javaClass.simpleName}"
                        entries = emptyList()
                    } finally { loading = false }
                }
            }, enabled = !loading && installing == null) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Verified, null)
                Spacer(Modifier.width(6.dp)); Text(if (loading) "Verifying…" else "Refresh signed catalog")
            }
        } else {
            Text("Publisher setup required: configure Gradle properties ASTRONAUT_CATALOG_URL and ASTRONAUT_CATALOG_PUBLIC_KEY. Never put the private signing key in the app.", style = MaterialTheme.typography.bodySmall)
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        entries.forEach { entry ->
            val check = compatibility[entry.id]
            var licenseAccepted by remember(entry.id, entry.version) { mutableStateOf(false) }
            val installedVersions = remember(entry.id) { mutableStateListOf<InstalledModelVersion>().apply { addAll(ModelInstallStore(context).list(entry.id)) } }
            val activeInstalled = installedVersions.firstOrNull { it.active }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(entry.name, fontWeight = FontWeight.SemiBold)
                    Text("\${entry.publisher} · \${entry.version} · \${entry.architecture} · \${entry.quantization}", style = MaterialTheme.typography.labelSmall)
                    Text(entry.description, style = MaterialTheme.typography.bodySmall)
                    if (activeInstalled != null) Text("Installed version: ${activeInstalled.version}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.licenseUrl))) }) { Text("Read model license") }
                    Row { Checkbox(checked = licenseAccepted, onCheckedChange = { licenseAccepted = it }); Text("I have reviewed and accept this model license", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall) }
                    Text("\${entry.license} · \${entry.sizeBytes / (1024 * 1024)} MiB · \${entry.capabilities.joinToString()}", style = MaterialTheme.typography.labelSmall)
                    if (check != null && !check.compatible) check.reasons.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    else if (check?.compatible == true) Text("Compatible with this device", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Button(onClick = {
                        installing = entry.id
                        scope.launch {
                            try {
                                val downloaded = VerifiedModelDownloader(context).download(entry.downloadUrl, entry.sha256, entry.fileName)
                                val installed = ModelInstallStore(context).install(entry, downloaded.file)
                                onInstalled(entry, installed.file)
                                status = "Installed \${entry.name} \${entry.version} and verified its active version."
                            } catch (e: Throwable) {
                                status = "Install failed: \${e.message ?: e.javaClass.simpleName}"
                            } finally { installing = null }
                        }
                    }, enabled = check?.compatible == true && installing == null && licenseAccepted && activeInstalled?.version != entry.version) {
                        if (installing == entry.id) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(6.dp)); Text(if (installing == entry.id) "Installing…" else if (activeInstalled != null) "Update model" else "Download and install")
                    }
                    if (installedVersions.count { !it.active } > 0) {
                        OutlinedButton(onClick = {
                            try {
                                val rolledBack = ModelInstallStore(context).rollback(entry.id)
                                installedVersions.clear(); installedVersions.addAll(ModelInstallStore(context).list(entry.id))
                                onInstalled(entry, rolledBack.file)
                                status = "Rolled back ${entry.id} to ${rolledBack.version}."
                            } catch (e: Throwable) { status = "Rollback failed: ${e.message}" }
                        }, enabled = installing == null) { Icon(Icons.Default.History, null); Spacer(Modifier.width(6.dp)); Text("Rollback to previous version") }
                    }
                }
            }
        }
    }
}
