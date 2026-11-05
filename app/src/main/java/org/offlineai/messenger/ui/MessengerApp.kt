package org.offlineai.messenger.ui
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import org.offlineai.messenger.data.*

private val Paper=Color(0xFFF4F2EA)
private val Ink=Color(0xFF17231F)
private val Green=Color(0xFF176B55)
private val Muted=Color(0xFF65736D)
private enum class Section(val label:String){CHATS("Chats"),MODELS("Models"),WORKSPACE("Workspace"),TRAINING("Training"),SETTINGS("Settings")}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MessengerApp(){
 val context= LocalContext.current
 val db= remember{DatabaseProvider.get(context)}
 val scope= rememberCoroutineScope()
 var section by remember{mutableStateOf(Section.CHATS)}
 var selectedConversation by remember{mutableStateOf<Long?>(null)}
 var draft by remember{mutableStateOf("")}
 var pendingAttachment by remember{mutableStateOf<Pair<Uri,String>?>(null)}
 var showNewProject by remember{mutableStateOf(false)}
 var projectName by remember{mutableStateOf("")}
 var projectDescription by remember{mutableStateOf("")}
 val conversations by db.chatDao().conversations().collectAsState(initial=emptyList())
 val models by db.modelDao().models().collectAsState(initial=emptyList())
 val projects by db.workspaceDao().projects().collectAsState(initial=emptyList())
 val conversationId=selectedConversation?:conversations.firstOrNull()?.id
 val messages by db.chatDao().messages(conversationId?:-1L).collectAsState(initial=emptyList())
 LaunchedEffect(conversations,selectedConversation){
  if(selectedConversation!=null && conversations.none{it.id==selectedConversation}) selectedConversation=null
  if(selectedConversation==null && conversations.isEmpty()) selectedConversation=db.chatDao().addConversation(ConversationEntity(title="New conversation"))
 }
 val attachmentPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null){
   runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
   pendingAttachment=uri to (DocumentFile.fromSingleUri(context,uri)?.name?:"Attachment")
  }
 }
 val modelPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null){
   runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
   scope.launch{val doc=DocumentFile.fromSingleUri(context,uri);db.modelDao().add(LocalModelEntity(uri.toString(),doc?.name?:"Imported model",context.contentResolver.getType(uri),doc?.length()?:0L))}
  }
 }
 MaterialTheme(colorScheme=lightColorScheme(primary=Green,onPrimary=Color.White,background=Paper,surface=Color.White,onSurface=Ink,secondary=Color(0xFFB7D6C8))){
  Scaffold(containerColor=Paper,topBar={TopAppBar(title={Column{
   Text("ASTRONAUT",style=MaterialTheme.typography.labelMedium,color=Green,fontWeight=FontWeight.Bold)
   Text(section.label,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
  }},actions={if(section==Section.CHATS) IconButton(onClick={scope.launch{selectedConversation=db.chatDao().addConversation(ConversationEntity(title="New conversation"))}}){Icon(Icons.Default.Add,"New chat")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Paper))},
   bottomBar={NavigationBar(containerColor=Color.White){Section.entries.forEach{item->
    val icon=when(item){Section.CHATS->Icons.Default.ChatBubbleOutline;Section.MODELS->Icons.Default.Memory;Section.WORKSPACE->Icons.Default.FolderOpen;Section.TRAINING->Icons.Default.Science;Section.SETTINGS->Icons.Default.Settings}
    NavigationBarItem(selected=section==item,onClick={section=item},icon={Icon(icon,item.label)},label={Text(item.label,maxLines=1)})
   }}}
  ){padding->Column(Modifier.fillMaxSize().padding(padding)){
   when(section){
    Section.CHATS->ChatScreen(conversations,conversationId,messages,draft,{draft=it},pendingAttachment?.second,
     {attachmentPicker.launch(arrayOf("*/*"))},{pendingAttachment=null},{selectedConversation=it},{
      val text=draft.trim();val target=conversationId
      if((text.isNotEmpty()||pendingAttachment!=null)&&target!=null){
       scope.launch{
        val first=messages.isEmpty()
        db.chatDao().addMessage(MessageEntity(conversationId=target,body=text,role="user",attachmentUri=pendingAttachment?.first?.toString(),attachmentName=pendingAttachment?.second))
        if(first&&text.isNotEmpty()) db.chatDao().rename(target,text.take(42))
       }
       draft="";pendingAttachment=null
      }
     })
    Section.MODELS->ModelsScreen(models,{modelPicker.launch(arrayOf("*/*"))},{uri->scope.launch{db.modelDao().remove(uri)}})
    Section.WORKSPACE->WorkspaceScreen(projects,{showNewProject=true},{id->scope.launch{db.workspaceDao().delete(id)}})
    Section.TRAINING->TrainingScreen()
    Section.SETTINGS->SettingsScreen()
   }
  }}
 }
 if(showNewProject) AlertDialog(onDismissRequest={showNewProject=false},title={Text("Create project")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  OutlinedTextField(projectName,{projectName=it},label={Text("Project name")},singleLine=true)
  OutlinedTextField(projectDescription,{projectDescription=it},label={Text("Description")},minLines=2)
 }},confirmButton={TextButton(onClick={
  if(projectName.isNotBlank()) scope.launch{db.workspaceDao().add(WorkspaceProjectEntity(name=projectName.trim(),description=projectDescription.trim()));projectName="";projectDescription="";showNewProject=false}
 }){Text("Create")}},dismissButton={TextButton(onClick={showNewProject=false}){Text("Cancel")}})
}

@Composable private fun ChatScreen(conversations:List<ConversationEntity>,selectedId:Long?,messages:List<MessageEntity>,draft:String,onDraftChange:(String)->Unit,attachmentName:String?,onAttach:()->Unit,onClearAttachment:()->Unit,onSelect:(Long)->Unit,onSend:()->Unit){
 Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
  if(conversations.size>1) LazyColumn(Modifier.heightIn(max=112.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){items(conversations){chat->
   TextButton(onClick={onSelect(chat.id)},modifier=Modifier.fillMaxWidth()){Text((if(chat.id==selectedId)"●  " else "")+chat.title,maxLines=1,overflow=TextOverflow.Ellipsis)}
  }}
  if(messages.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
   Box(Modifier.size(68.dp).background(Color(0xFFDCEBE3),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.AutoAwesome,null,tint=Green,modifier=Modifier.size(32.dp))}
   Spacer(Modifier.height(18.dp));Text("A quieter kind of AI.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
   Text("Your conversations stay on this device.",color=Muted)
   Text("Import a compatible model in Models to prepare local inference.",style=MaterialTheme.typography.bodySmall,color=Muted)
  }else LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   items(messages,key={it.id}){message->val mine=message.role=="user"
    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start){
     Surface(color=if(mine)Color(0xFFDCEBE3)else Color.White,shape=RoundedCornerShape(18.dp),modifier=Modifier.widthIn(max=320.dp)){
      Column(Modifier.padding(14.dp)){
       if(message.body.isNotBlank()) Text(message.body,color=Ink)
       if(message.attachmentName!=null){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AttachFile,null,tint=Green,modifier=Modifier.size(16.dp));Text(message.attachmentName,color=Green,style=MaterialTheme.typography.labelMedium)}}
       Text(if(mine)"YOU" else "ASTRONAUT",style=MaterialTheme.typography.labelSmall,color=Muted)
      }
     }
    }
   }
  }
  if(attachmentName!=null) Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().padding(bottom=6.dp)){
   Icon(Icons.Default.AttachFile,null,tint=Green);Text(attachmentName,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
   IconButton(onClick=onClearAttachment){Icon(Icons.Default.Close,"Remove attachment")}
  }
  Surface(color=Color.White,shape=RoundedCornerShape(24.dp),shadowElevation=2.dp){
   Row(Modifier.padding(start=6.dp,end=8.dp,top=4.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically){
    IconButton(onClick=onAttach){Icon(Icons.Default.AttachFile,"Attach file",tint=Muted)}
    TextField(draft,onDraftChange,Modifier.weight(1f),placeholder={Text("Message privately…")},maxLines=5,colors=TextFieldDefaults.colors(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
    IconButton(onClick=onSend,enabled=draft.isNotBlank()||attachmentName!=null){Icon(Icons.Default.Send,"Save message",tint=Green)}
   }
  }
  Text("Messages are saved locally. AI replies are disabled until a real inference runtime is connected.",style=MaterialTheme.typography.labelSmall,color=Muted,modifier=Modifier.padding(top=6.dp,bottom=8.dp))
 }
}

@Composable private fun ModelsScreen(models:List<LocalModelEntity>,onImport:()->Unit,onRemove:(String)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("Your local model library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  Text("Import a model file from storage. Importing records the file; it does not claim the runtime can load it yet.",color=Muted)
  Spacer(Modifier.height(14.dp));Button(onClick=onImport,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp));Text("Import model file")}
  Spacer(Modifier.height(12.dp))
  if(models.isEmpty()) EmptyState("No model files imported","Choose a compatible GGUF or LiteRT model file to add it to this library.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(models,key={it.uri}){model->
   Card{Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Default.Memory,null,tint=Green,modifier=Modifier.size(30.dp));Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)){Text(model.displayName,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis);Text(formatBytes(model.sizeBytes)+" · Runtime not connected",color=Muted,style=MaterialTheme.typography.labelSmall)}
    IconButton(onClick={onRemove(model.uri)}){Icon(Icons.Default.DeleteOutline,"Remove model")}
   }}
  }}
 }
}
@Composable private fun WorkspaceScreen(projects:List<WorkspaceProjectEntity>,onAdd:()->Unit,onDelete:(Long)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Developer workspace",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold);Text("Organise local projects and notes.",color=Muted)};FilledTonalButton(onClick=onAdd){Icon(Icons.Default.Add,null);Text("New")}}
  Spacer(Modifier.height(14.dp));if(projects.isEmpty())EmptyState("A space for your work","Create a project to keep its name and description on this device.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(projects,key={it.id}){project->Card{Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
   Icon(Icons.Default.Folder,null,tint=Green,modifier=Modifier.size(28.dp));Spacer(Modifier.width(12.dp))
   Column(Modifier.weight(1f)){Text(project.name,fontWeight=FontWeight.SemiBold);if(project.description.isNotBlank())Text(project.description,color=Muted,maxLines=2)}
   IconButton(onClick={onDelete(project.id)}){Icon(Icons.Default.DeleteOutline,"Delete project")}
  }} }}
 }
}
@Composable private fun TrainingScreen(){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("AI Training Studio",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  Text("Prepare datasets on Android; run fine-tuning on a configured training host.",color=Muted)
  Spacer(Modifier.height(16.dp))
  InfoCard("Dataset preparation","A future workflow will validate JSONL/CSV data, remove duplicates and create train/validation splits.")
  InfoCard("LoRA fine-tuning","Training execution is not enabled in this Android build. It requires a real trainer and compatible GPU or a configured remote worker.")
  InfoCard("Model promotion","Only evaluated, explicitly approved model artifacts should be installed into the local model library.")
 }
}
@Composable private fun SettingsScreen(){Column(Modifier.fillMaxSize().padding(16.dp)){
 Text("Privacy & device",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
 InfoCard("Offline-first","Chat records, imported model references and project metadata are stored in the app's local Room database.")
 InfoCard("No silent uploads","This build contains no account sign-in, analytics upload or automatic chat export.")
 InfoCard("Runtime status","Model file import is available. Actual inference, encryption-at-rest and biometric lock still need implementation and verification.")
}}
@Composable private fun InfoCard(title:String,body:String){Card(Modifier.fillMaxWidth().padding(bottom=10.dp)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(4.dp));Text(body,color=Muted,style=MaterialTheme.typography.bodyMedium)}}}
@Composable private fun EmptyState(title:String,body:String){Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,fontWeight=FontWeight.SemiBold);Text(body,color=Muted,style=MaterialTheme.typography.bodySmall)}}
private fun formatBytes(bytes:Long):String=when{bytes>=1_073_741_824->"%.1f GB".format(bytes/1_073_741_824.0);bytes>=1_048_576->"%.1f MB".format(bytes/1_048_576.0);bytes>=1024->"%.1f KB".format(bytes/1024.0);else->"$bytes B"}
