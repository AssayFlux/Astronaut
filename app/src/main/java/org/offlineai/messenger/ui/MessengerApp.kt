package org.offlineai.messenger.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
private val Paper = Color(0xFFF5F2E9)
private val Green = Color(0xFF176B55)
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MessengerApp() {
 var section by remember { mutableStateOf("Chats") }
 var draft by remember { mutableStateOf("") }
 var messages by remember { mutableStateOf(listOf("Welcome. Connect verified local model providers to enable inference.")) }
 val sections = listOf("Chats", "AI Hub", "Models", "Workspace", "Activity", "Settings")
 MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Paper, surface = Color.White)) {
  Scaffold(containerColor = Paper, topBar = { TopAppBar(title = { Column { Text("Offline AI", fontWeight = FontWeight.Bold); Text(section, style = MaterialTheme.typography.labelSmall) } }) },
   bottomBar = { NavigationBar { sections.forEach { s ->
    val icon = when(s) { "Chats" -> Icons.Default.Chat; "AI Hub" -> Icons.Default.Hub; "Models" -> Icons.Default.Memory; "Workspace" -> Icons.Default.Folder; "Activity" -> Icons.Default.Timeline; else -> Icons.Default.Settings }
    NavigationBarItem(selected = section == s, onClick = { section = s }, icon = { Icon(icon, contentDescription = s) }, label = { Text(s, maxLines = 1) })
   } } }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
    when(section) {
     "Chats" -> {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
       Text("Conversations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
       TextButton(onClick = { messages = emptyList() }) { Text("New chat") }
      }
      LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
       items(messages) { m -> Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) { Text(m, Modifier.fillMaxWidth().padding(16.dp)) } }
      }
      Row {
       OutlinedTextField(draft, { draft = it }, Modifier.weight(1f), placeholder = { Text("Message your local AI…") }, maxLines = 4)
       IconButton(onClick = { if (draft.isNotBlank()) { messages = messages + "You: $draft" + "AI: No inference provider is configured; no answer was generated."; draft = "" } }) { Icon(Icons.Default.Send, "Send") }
      }
     }
     "AI Hub" -> Info("AI orchestration", listOf("Capability-based task routing", "Two independent models required for collaboration", "Coordinator synthesis; no fabricated participants"))
     "Models" -> Info("Model Library", listOf("Metadata-driven model registry contracts", "llama.cpp and LiteRT-LM integration points", "Model import/download verification is not yet implemented"))
     "Workspace" -> Info("Workspace & Developer Studio", listOf("Project workspace foundation", "Backend provider integration contracts", "Training dataset and LoRA job contracts"))
     "Activity" -> Info("Activity & Diagnostics", listOf("Only real provider execution should be reported", "RAM/thermal monitoring is not yet implemented"))
     else -> Info("Privacy & Settings", listOf("Offline-first shell; no account required", "No telemetry client included", "Encryption and biometric lock remain future hardening"))
    }
   }
  }
 }
}
@Composable private fun Info(title: String, rows: List<String>) {
 Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
 Spacer(Modifier.height(12.dp))
 rows.forEach { ListItem(headlineContent = { Text(it) }, leadingContent = { Icon(Icons.Default.CheckCircleOutline, null, tint = Green) }) }
}
