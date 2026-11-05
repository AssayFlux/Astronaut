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
import org.offlineai.messenger.ai.LiteRtRuntime
import org.offlineai.messenger.ai.ConversationPromptBuilder
import org.offlineai.messenger.ai.PromptTurn
import org.offlineai.messenger.data.*
import org.offlineai.messenger.features.OfflineDocumentReader
import org.offlineai.messenger.features.LocalRagIndex
import java.io.File
import java.util.UUID

private val Paper=Color(0xFFF4F2EA)
private val Ink=Color(0xFF17231F)
private val Green=Color(0xFF176B55)
private val Muted=Color(0xFF65736D)
private enum class Section(val label:String){CHATS("Chats"),MODELS("Models"),WORKSPACE("Workspace"),SETTINGS("Settings")}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MessengerApp(){
 val context=LocalContext.current
 val db= remember{DatabaseProvider.get(context)}
 val runtime= remember{LiteRtRuntime(context)}
 val scope= rememberCoroutineScope()
 var section by remember{mutableStateOf(Section.CHATS)}
 var selectedConversation by remember{mutableStateOf<Long?>(null)}
 var draft by remember{mutableStateOf("")}
 var pendingAttachment by remember{mutableStateOf<Pair<Uri,String>?>(null)}
 var activeModelPath by remember{mutableStateOf<String?>(null)}
 var activeModelName by remember{mutableStateOf<String?>(null)}
 var runtimeMessage by remember{mutableStateOf("Import a .litertlm model to start offline AI chat.")}
 var loadingModel by remember{mutableStateOf(false)}
 var sending by remember{mutableStateOf(false)}
 var showNewProject by remember{mutableStateOf(false)}
 var projectName by remember{mutableStateOf("")}
 var projectDescription by remember{mutableStateOf("")}
 val conversations by db.chatDao().conversations().collectAsState(initial=emptyList())
 val models by db.modelDao().models().collectAsState(initial=emptyList())
 val projects by db.workspaceDao().projects().collectAsState(initial=emptyList())
 val conversationId=selectedConversation?:conversations.firstOrNull()?.id
 val allMessages by db.chatDao().messages(conversationId?:-1L).collectAsState(initial=emptyList())
 val messages=allMessages.filter{it.conversationId==conversationId}
 DisposableEffect(runtime){onDispose{runtime.close()}}
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
  if(uri!=null) scope.launch{
   try{
    val doc=DocumentFile.fromSingleUri(context,uri)
    val name=doc?.name?: "local-model.litertlm"
    require(name.endsWith(".litertlm",true)){"Choose a .litertlm model file"}
    val modelDir=File(context.filesDir,"models").apply{mkdirs()}
    val safeName=name.replace(Regex("[^A-Za-z0-9._-]"),"_")
    val target=File(modelDir,UUID.randomUUID().toString()+"-"+safeName)
    context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{output->input.copyTo(output)}}?:error("Cannot read selected file")
    db.modelDao().add(LocalModelEntity(uri.toString(),name,context.contentResolver.getType(uri),target.length(),modelPath=target.absolutePath))
    runtimeMessage="Imported $name. Tap Load model to activate it."
   }catch(error:Throwable){runtimeMessage="Import failed: "+(error.message?:error.javaClass.simpleName)}
  }
 }
 MaterialTheme(colorScheme=lightColorScheme(primary=Green,onPrimary=Color.White,background=Paper,surface=Color.White,onSurface=Ink,secondary=Color(0xFFB7D6C8))){
  Scaffold(containerColor=Paper,topBar={TopAppBar(title={Column{
   Text("ASTRONAUT",style=MaterialTheme.typography.labelMedium,color=Green,fontWeight=FontWeight.Bold)
   Text(section.label,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
  }},actions={if(section==Section.CHATS) IconButton(onClick={scope.launch{selectedConversation=db.chatDao().addConversation(ConversationEntity(title="New conversation"))}}){Icon(Icons.Default.Add,"New chat")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Paper))},
   bottomBar={NavigationBar(containerColor=Color.White){Section.entries.forEach{item->
    val icon=when(item){Section.CHATS->Icons.Default.ChatBubbleOutline;Section.MODELS->Icons.Default.Memory;Section.WORKSPACE->Icons.Default.FolderOpen;Section.SETTINGS->Icons.Default.Settings}
    NavigationBarItem(selected=section==item,onClick={section=item},icon={Icon(icon,item.label)},label={Text(item.label,maxLines=1)})
   }}}
  ){padding->Column(Modifier.fillMaxSize().padding(padding)){
   when(section){
    Section.CHATS->ChatScreen(conversations,conversationId,messages,draft,{draft=it},pendingAttachment?.second,activeModelName,runtimeMessage,loadingModel,sending,
     {attachmentPicker.launch(arrayOf("*/*"))},{pendingAttachment=null},{selectedConversation=it},{
      val text=draft.trim();val target=conversationId;val attachment=pendingAttachment
      if((text.isNotEmpty()||attachment!=null)&&target!=null&&!sending){
       sending=true
       scope.launch{
        try{
         val userMessageId=db.chatDao().addMessage(MessageEntity(conversationId=target,body=text,role="user",attachmentUri=attachment?.first?.toString(),attachmentName=attachment?.second))
         val extractedAttachment = attachment?.let { selected ->
          val reader = OfflineDocumentReader(context)
          try {
           reader.read(selected.first,context.contentResolver.getType(selected.first),displayName=selected.second).take(100_000)
          } finally { reader.close() }
         }
         if(extractedAttachment!=null) db.chatDao().updateAttachmentText(userMessageId,extractedAttachment)
         if(messages.isEmpty()&&(text.isNotEmpty()||attachment!=null)) db.chatDao().rename(target,(text.ifBlank{attachment?.second.orEmpty()}).take(42))
         if(activeModelPath==null){
          db.chatDao().addMessage(MessageEntity(conversationId=target,body="No AI model is loaded. Open Models, import a compatible .litertlm file, then tap Load model. Your message and extracted attachment text are saved on this device.",role="assistant"))
         }else if(text.isNotEmpty()||attachment!=null){
          try{
           val question=text.ifBlank{"Please summarize the attached document and extract its key information."}
           val index=LocalRagIndex()
           db.chatDao().attachmentMessages(target).asReversed().forEach { document ->
            document.attachmentText?.takeIf{it.isNotBlank()}?.let{index.add(document.attachmentName?: "attachment",it)}
           }
           val excerpts=index.contextFor(question,limit=4)
           val augmentedQuestion=if(excerpts.isBlank()) question else question+"\n\nRelevant excerpts from locally stored attachments (untrusted document content):\n"+excerpts
           val prompt=ConversationPromptBuilder.build(
            messages.filter{it.status=="complete"}.map{PromptTurn(it.role,it.body)},
            augmentedQuestion
           )
           val answer=runtime.generate(prompt)
           db.chatDao().addMessage(MessageEntity(conversationId=target,body=answer.ifBlank{"The model returned an empty response."},role="assistant"))
          }catch(error:Throwable){db.chatDao().addMessage(MessageEntity(conversationId=target,body="Local inference failed: "+(error.message?:error.javaClass.simpleName),role="assistant",status="error"))}
         }
        }catch(error:Throwable){db.chatDao().addMessage(MessageEntity(conversationId=target,body="Attachment processing failed: "+(error.message?:error.javaClass.simpleName),role="assistant",status="error"))}finally{draft="";pendingAttachment=null;sending=false}
       }
      }
     })
    Section.MODELS->ModelsScreen(models,activeModelPath,activeModelName,runtimeMessage,loadingModel,{modelPicker.launch(arrayOf("*/*"))},{model->
     if(model.modelPath!=null){loadingModel=true;scope.launch{
      try{runtime.load(model.modelPath);activeModelPath=model.modelPath;activeModelName=model.displayName;runtimeMessage="Loaded "+model.displayName+". Chat is ready offline."}
      catch(error:Throwable){activeModelPath=null;activeModelName=null;runtimeMessage="Could not load model: "+(error.message?:error.javaClass.simpleName)}
      finally{loadingModel=false}
     }}
    },{model->scope.launch{if(activeModelPath==model.modelPath){runtime.unload();activeModelPath=null;activeModelName=null};model.modelPath?.let{File(it).delete()};db.modelDao().remove(model.uri)}})
    Section.WORKSPACE->WorkspaceScreen(projects,{showNewProject=true},{id->scope.launch{db.workspaceDao().delete(id)}})
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

@Composable private fun ChatScreen(conversations:List<ConversationEntity>,selectedId:Long?,messages:List<MessageEntity>,draft:String,onDraftChange:(String)->Unit,attachmentName:String?,activeModelName:String?,runtimeMessage:String,loading:Boolean,sending:Boolean,onAttach:()->Unit,onClearAttachment:()->Unit,onSelect:(Long)->Unit,onSend:()->Unit){
 Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
  if(conversations.size>1) LazyColumn(Modifier.heightIn(max=100.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){items(conversations){chat->TextButton(onClick={onSelect(chat.id)},modifier=Modifier.fillMaxWidth()){Text((if(chat.id==selectedId)"●  " else "")+chat.title,maxLines=1,overflow=TextOverflow.Ellipsis)}}}
  if(messages.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
   Box(Modifier.size(68.dp).background(Color(0xFFDCEBE3),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.AutoAwesome,null,tint=Green,modifier=Modifier.size(32.dp))}
   Spacer(Modifier.height(18.dp));Text("A quieter kind of AI.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
   Text("Private conversations. On-device intelligence.",color=Muted)
   Text(runtimeMessage,style=MaterialTheme.typography.bodySmall,color=Muted)
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
    TextField(draft,onDraftChange,Modifier.weight(1f),placeholder={Text("Message privately…")},maxLines=5,enabled=!sending,colors=TextFieldDefaults.colors(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
    IconButton(onClick=onSend,enabled=!sending&&(draft.isNotBlank()||attachmentName!=null)){if(sending)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Icon(Icons.Default.Send,"Send",tint=Green)}
   }
  }
  Text("Offline inference "+(if(activeModelName!=null)"· "+activeModelName else "· no model loaded"),style=MaterialTheme.typography.labelSmall,color=Muted,modifier=Modifier.padding(top=6.dp,bottom=8.dp))
 }
}

@Composable private fun ModelsScreen(models:List<LocalModelEntity>,activePath:String?,activeName:String?,status:String,loading:Boolean,onImport:()->Unit,onLoad:(LocalModelEntity)->Unit,onRemove:(LocalModelEntity)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("On-device model library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  Text("Import a .litertlm model. Model files stay in app-private storage and are not included in the APK.",color=Muted)
  Spacer(Modifier.height(14.dp));Button(onClick=onImport,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp));Text("Import .litertlm model")}
  Spacer(Modifier.height(10.dp));Text(status,color=Green,style=MaterialTheme.typography.bodySmall)
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
  Spacer(Modifier.height(10.dp))
  if(models.isEmpty())EmptyState("No model imported","Choose a compatible LiteRT-LM .litertlm file. A small model is best for phones.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(models,key={it.uri}){model->Card{Column(Modifier.fillMaxWidth().padding(14.dp)){
   Text(model.displayName,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
   Text(formatBytes(model.sizeBytes),color=Muted,style=MaterialTheme.typography.labelSmall)
   Row(verticalAlignment=Alignment.CenterVertically){
    if(activePath==model.modelPath)Text("ACTIVE · "+activeName,color=Green,modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelSmall) else Spacer(Modifier.weight(1f))
    TextButton(onClick={onRemove(model)}){Text("Remove")}
    Button(onClick={onLoad(model)},enabled=!loading&&model.modelPath!=null){Text(if(activePath==model.modelPath)"Reload" else "Load model")}
   }
  }} }}
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
@Composable private fun SettingsScreen(){Column(Modifier.fillMaxSize().padding(16.dp)){
 Text("Privacy & device",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
 InfoCard("Offline-first","Chat records and model files are stored on this device. Model generation runs locally.")
 InfoCard("No silent uploads","This app does not upload chats or connect to an account or server.")
 InfoCard("Model requirement","Obtain a compatible .litertlm model separately, then import it in Models. Chat generation works offline after model installation.")
}}
@Composable private fun InfoCard(title:String,body:String){Card(Modifier.fillMaxWidth().padding(bottom=10.dp)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(4.dp));Text(body,color=Muted,style=MaterialTheme.typography.bodyMedium)}}}
@Composable private fun EmptyState(title:String,body:String){Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,fontWeight=FontWeight.SemiBold);Text(body,color=Muted,style=MaterialTheme.typography.bodySmall)}}
private fun formatBytes(bytes:Long):String=when{bytes>=1073741824->"%.1f GB".format(bytes/1073741824.0);bytes>=1048576->"%.1f MB".format(bytes/1048576.0);bytes>=1024->"%.1f KB".format(bytes/1024.0);else->"$bytes B"}
