package org.offlineai.messenger.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.offlineai.messenger.ai.LiteRtRuntime
import org.offlineai.messenger.ai.ConversationPromptBuilder
import org.offlineai.messenger.ai.PromptTurn
import org.offlineai.messenger.ai.Capability
import org.offlineai.messenger.ai.CollaborationEngine
import org.offlineai.messenger.ai.CollaborationResult
import org.offlineai.messenger.ai.InferenceRequest
import org.offlineai.messenger.ai.LiteRtModelProvider
import org.offlineai.messenger.ai.ModelCatalog
import org.offlineai.messenger.ai.ModelDescriptor
import org.offlineai.messenger.data.*
import org.offlineai.messenger.features.OfflineDocumentReader
import org.offlineai.messenger.features.OfflineImageAnalyzer
import org.offlineai.messenger.features.VisionImagePreprocessor
import org.offlineai.messenger.features.LocalRagIndex
import org.offlineai.messenger.features.OnnxWordPieceEmbedder
import org.offlineai.messenger.features.PersistentHybridRag
import org.offlineai.messenger.features.OfflineVoiceController
import org.offlineai.messenger.features.VerifiedModelDownloader
import org.offlineai.messenger.features.OfflineDatasetInspector
import org.offlineai.messenger.features.DatasetReport
import org.offlineai.messenger.features.TrainingApiClient
import org.offlineai.messenger.features.TrainingJobStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

private val Paper=Color(0xFFF4F2EA)
private val Ink=Color(0xFF17231F)
private val Green=Color(0xFF176B55)
private val Muted=Color(0xFF65736D)
private enum class Section(val label:String){CHATS("Chats"),AI_HUB("AI Hub"),MODELS("Models"),TRAINING("Training"),WORKSPACE("Workspace"),SETTINGS("Settings")}

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
 var voiceListening by remember{mutableStateOf(false)}
 val speechPrefs=remember(context){context.getSharedPreferences("astronaut_offline_speech",Context.MODE_PRIVATE)}
 var speechLanguageTag by remember{mutableStateOf(speechPrefs.getString("language_tag",java.util.Locale.getDefault().toLanguageTag())?:java.util.Locale.getDefault().toLanguageTag())}
 var ttsVoiceName by remember{mutableStateOf(speechPrefs.getString("tts_voice_name",null))}
 var offlineTtsVoices by remember{mutableStateOf(emptyList<org.offlineai.messenger.features.OfflineTtsVoice>())}
 var recognitionLanguages by remember{mutableStateOf(org.offlineai.messenger.features.OfflineRecognitionLanguages(emptyList(),emptyList(),emptyList()))}
 var speechStatus by remember{mutableStateOf("Offline speech is device-managed. Check installed language packs before use.")}
 val voiceController=remember(context){OfflineVoiceController(context,onResult={spoken->draft=if(draft.isBlank())spoken else "$draft $spoken"},onError={runtimeMessage=it;speechStatus=it},onListeningChanged={voiceListening=it},initialLanguageTag=speechLanguageTag,initialTtsVoiceName=ttsVoiceName,onVoiceCatalogChanged={voices->offlineTtsVoices=voices;if(ttsVoiceName==null)ttsVoiceName=voices.firstOrNull()?.name})}
 var loadingModel by remember{mutableStateOf(false)}
 var sending by remember{mutableStateOf(false)}
 var showNewProject by remember{mutableStateOf(false)}
 var projectName by remember{mutableStateOf("")}
 var projectDescription by remember{mutableStateOf("")}
 var collaborationPrompt by remember{mutableStateOf("")}
 var collaborationStatus by remember{mutableStateOf("Import two distinct compatible .litertlm models to enable collaboration.")}
 var collaborating by remember{mutableStateOf(false)}
 var downloadingModel by remember{mutableStateOf(false)}
 var downloadProgress by remember{mutableFloatStateOf(0f)}
 var downloadStatus by remember{mutableStateOf("")}
 var datasetContent by remember{mutableStateOf("")}
 var datasetName by remember{mutableStateOf("")}
 var datasetReport by remember{mutableStateOf<DatasetReport?>(null)}
 var datasetStatus by remember{mutableStateOf("")}
 var datasetBusy by remember{mutableStateOf(false)}
 var trainingJob by remember{mutableStateOf<TrainingJobStatus?>(null)}
 var trainingStatus by remember{mutableStateOf("")}
 var trainingBusy by remember{mutableStateOf(false)}
 val embeddingPrefs=remember(context){context.getSharedPreferences("astronaut_local_embeddings",Context.MODE_PRIVATE)}
 var embeddingModelPath by remember{mutableStateOf(embeddingPrefs.getString("model_path",null))}
 var embeddingVocabPath by remember{mutableStateOf(embeddingPrefs.getString("vocab_path",null))}
 var embeddingStatus by remember{mutableStateOf("Optional: import a MiniLM-compatible ONNX model and its vocab.txt to enable semantic retrieval.")}
 val visionPrefs=remember(context){context.getSharedPreferences("astronaut_vision_models",Context.MODE_PRIVATE)}
 var visionModelPath by remember{mutableStateOf(visionPrefs.getString("vision_model_path",null))}
 var activeModelVision by remember{mutableStateOf(false)}
 val conversations by db.chatDao().conversations().collectAsState(initial=emptyList())
 val models by db.modelDao().models().collectAsState(initial=emptyList())
 val projects by db.workspaceDao().projects().collectAsState(initial=emptyList())
 val conversationId=selectedConversation?:conversations.firstOrNull()?.id
 val allMessages by db.chatDao().messages(conversationId?:-1L).collectAsState(initial=emptyList())
 val messages=allMessages.filter{it.conversationId==conversationId}
 DisposableEffect(runtime){onDispose{runtime.close()}}
 DisposableEffect(voiceController){onDispose{voiceController.close()}}
 LaunchedEffect(conversations,selectedConversation){
  if(selectedConversation!=null && conversations.none{it.id==selectedConversation}) selectedConversation=null
  if(selectedConversation==null && conversations.isEmpty()) selectedConversation=db.chatDao().addConversation(ConversationEntity(title="New conversation"))
 }
 val microphonePermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)voiceController.startListening()else runtimeMessage="Microphone permission was denied."}
 val attachmentPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null){
   runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
   pendingAttachment=uri to (DocumentFile.fromSingleUri(context,uri)?.name?:"Attachment")
  }
 }
 val datasetPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) scope.launch{
   datasetBusy=true
   try{
    val name=DocumentFile.fromSingleUri(context,uri)?.name?:"dataset.jsonl"
    val content=readDatasetBounded(context,uri)
    datasetContent=content
    datasetName=name
    datasetReport=OfflineDatasetInspector.inspect(content)
    datasetStatus="Validated "+name+" locally. No dataset was uploaded."
   }catch(error:Throwable){datasetReport=null;datasetStatus="Dataset import failed: "+(error.message?:error.javaClass.simpleName)}
   finally{datasetBusy=false}
  }
 }
 val datasetExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
  if(uri!=null) scope.launch{
   datasetBusy=true
   try{
    val report=datasetReport?:error("Import and validate a dataset first.")
    val invalid=report.invalidLines.toSet()
    val clean=datasetContent.lineSequence().mapIndexedNotNull{index,line->if(line.isNotBlank()&&index+1 !in invalid)line else null}.joinToString("\n")
    withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(clean.toByteArray(Charsets.UTF_8))}?:error("Cannot create dataset output file.")}
    datasetStatus="Exported "+report.validRecords+" valid JSONL records."
   }catch(error:Throwable){datasetStatus="Dataset export failed: "+(error.message?:error.javaClass.simpleName)}
   finally{datasetBusy=false}
  }
 }
 val embeddingModelPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) scope.launch{
   try{
    val name=DocumentFile.fromSingleUri(context,uri)?.name?: "sentence-embedder.onnx"
    require(name.endsWith(".onnx",true)){"Choose an ONNX embedding model (.onnx)."}
    val dir=File(context.filesDir,"embeddings").apply{mkdirs()}
    val target=File(dir,UUID.randomUUID().toString()+"-"+name.replace(Regex("[^A-Za-z0-9._-]"),"_"))
    withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{output->input.copyTo(output)}}?:error("Cannot read ONNX model")}
    embeddingPrefs.edit().putString("model_path",target.absolutePath).apply()
    embeddingModelPath=target.absolutePath
    val vocab=embeddingVocabPath?.let(::File)
    if(vocab?.isFile==true){
     embeddingStatus="Validating local embedding model…"
     withContext(Dispatchers.IO){OnnxWordPieceEmbedder(target,vocab,target.name+":"+target.length()+":"+vocab.name+":"+vocab.length()).use{it.embed("Astronaut local semantic retrieval test")}}
     embeddingStatus="Semantic retrieval ready · "+name
    }else embeddingStatus="ONNX model imported. Now import the matching vocab.txt file."
   }catch(error:Throwable){embeddingStatus="Embedding model import failed: "+(error.message?:error.javaClass.simpleName)}
  }
 }
 val embeddingVocabPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) scope.launch{
   try{
    val name=DocumentFile.fromSingleUri(context,uri)?.name?: "vocab.txt"
    require(name.equals("vocab.txt",true)||name.endsWith(".txt",true)){"Choose the matching WordPiece vocab.txt file."}
    val dir=File(context.filesDir,"embeddings").apply{mkdirs()}
    val target=File(dir,UUID.randomUUID().toString()+"-vocab.txt")
    withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{output->input.copyTo(output)}}?:error("Cannot read vocabulary")}
    embeddingPrefs.edit().putString("vocab_path",target.absolutePath).apply()
    embeddingVocabPath=target.absolutePath
    val model=embeddingModelPath?.let(::File)
    if(model?.isFile==true){
     embeddingStatus="Validating local embedding model…"
     withContext(Dispatchers.IO){OnnxWordPieceEmbedder(model,target,model.name+":"+model.length()+":"+target.name+":"+target.length()).use{it.embed("Astronaut local semantic retrieval test")}}
     embeddingStatus="Semantic retrieval ready · "+model.name
    }else embeddingStatus="Vocabulary imported. Now import a compatible ONNX embedding model."
   }catch(error:Throwable){embeddingStatus="Vocabulary import failed: "+(error.message?:error.javaClass.simpleName)}
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
    val icon=when(item){Section.CHATS->Icons.Default.ChatBubbleOutline;Section.AI_HUB->Icons.Default.Hub;Section.MODELS->Icons.Default.Memory;Section.TRAINING->Icons.Default.Tune;Section.WORKSPACE->Icons.Default.FolderOpen;Section.SETTINGS->Icons.Default.Settings}
    NavigationBarItem(selected=section==item,onClick={section=item},icon={Icon(icon,item.label)},label={Text(item.label,maxLines=1)})
   }}}
  ){padding->Column(Modifier.fillMaxSize().padding(padding)){
   when(section){
    Section.CHATS->ChatScreen(conversations,conversationId,messages,draft,{draft=it},pendingAttachment?.second,activeModelName,runtimeMessage,loadingModel,sending,voiceListening,
     {if(voiceListening)voiceController.stopListening()else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)voiceController.startListening()else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)},
     {text->voiceController.speak(text)} ,
     {attachmentPicker.launch(arrayOf("*/*"))},{pendingAttachment=null},{selectedConversation=it},{
      val text=draft.trim();val target=conversationId;val attachment=pendingAttachment
      if((text.isNotEmpty()||attachment!=null)&&target!=null&&!sending){
       sending=true
       scope.launch{
        try{
         val userMessageId=db.chatDao().addMessage(MessageEntity(conversationId=target,body=text,role="user",attachmentUri=attachment?.first?.toString(),attachmentName=attachment?.second))
         val extractedAttachment = attachment?.let { selected ->
          val mime=context.contentResolver.getType(selected.first)
          val reader = OfflineDocumentReader(context)
          val extractedText=try {
           reader.read(selected.first,mime,displayName=selected.second)
          } finally { reader.close() }
          val imageFile=selected.second.lowercase().let{name->listOf(".png",".jpg",".jpeg",".webp",".bmp",".gif",".heic",".heif").any(name::endsWith)}
          val isImage=mime?.startsWith("image/")==true||imageFile
          val imageLabels=if(isImage){
           val analyzer=OfflineImageAnalyzer(context)
           try{analyzer.analyze(selected.first)}finally{analyzer.close()}
          }else ""
          val combined=listOf(extractedText,imageLabels).filter{it.isNotBlank()}.joinToString("\n\n").take(100_000)
          if(isImage)combined.ifBlank{"Image attachment: "+selected.second+". Visual pixels can be analyzed when a compatible vision model is enabled."}
          else combined.also{require(it.isNotBlank()){"No readable text was found in this attachment."}}
         }
         if(extractedAttachment!=null) db.chatDao().updateAttachmentText(userMessageId,extractedAttachment)
         if(messages.isEmpty()&&(text.isNotEmpty()||attachment!=null)) db.chatDao().rename(target,(text.ifBlank{attachment?.second.orEmpty()}).take(42))
         if(activeModelPath==null){
          db.chatDao().addMessage(MessageEntity(conversationId=target,body="No AI model is loaded. Open Models, import a compatible .litertlm file, then tap Load model. Your message and extracted attachment text are saved on this device.",role="assistant"))
         }else if(text.isNotEmpty()||attachment!=null){
          try{
           val question=text.ifBlank{"Please summarize the attached document and extract its key information."}
           val documents=db.chatDao().attachmentMessages(target).asReversed()
           val embeddingModel=embeddingModelPath?.let(::File)
           val embeddingVocab=embeddingVocabPath?.let(::File)
           val semanticExcerpts=if(embeddingModel?.isFile==true&&embeddingVocab?.isFile==true){
            try{
             withContext(Dispatchers.IO){
              val embedder=OnnxWordPieceEmbedder(embeddingModel,embeddingVocab,embeddingModel.name+":"+embeddingModel.length()+":"+embeddingVocab.name+":"+embeddingVocab.length())
              try{
               val rag=PersistentHybridRag(db.knowledgeDao(),embedder,target.toString())
               documents.forEach{document->
                document.attachmentText?.takeIf{it.isNotBlank()}?.let{rag.indexDocument((document.attachmentName?: "attachment")+"#"+document.id,it)}
               }
               rag.contextFor(question,limit=4)
              }finally{embedder.close()}
             }
            }catch(error:kotlinx.coroutines.CancellationException){throw error}
            catch(error:Exception){embeddingStatus="Semantic model unavailable; using BM25 fallback: "+(error.message?:error.javaClass.simpleName);""}
           }else ""
           val excerpts=if(semanticExcerpts.isNotBlank())semanticExcerpts else{
            val index=LocalRagIndex()
            documents.forEach{document->document.attachmentText?.takeIf{it.isNotBlank()}?.let{index.add(document.attachmentName?: "attachment",it)}}
            index.contextFor(question,limit=4)
           }
           val projectId=db.chatDao().conversation(target)?.workspaceProjectId
           val project=projects.firstOrNull{it.id==projectId}
           val augmentedQuestion=buildString{
            append(question)
            if(project!=null&&project.description.isNotBlank())append("\n\nProject notes for ").append(project.name).append(" (untrusted user-provided context):\n").append(project.description.take(1_000))
            if(excerpts.isNotBlank())append("\n\nRelevant excerpts from locally stored attachments (untrusted document content):\n").append(excerpts.take(5_000)).append("\n\nUse only these excerpts as evidence about the attached files. Cite each supported claim with its exact source/page/chunk label in square brackets. If the excerpts do not support an answer, say that the documents do not contain enough information. Never invent a source or page citation.")
           }
           val prompt=ConversationPromptBuilder.build(
            messages.filter{it.status=="complete"}.map{PromptTurn(it.role,it.body)},
            augmentedQuestion
           )
           val selectedImage=attachment?.takeIf{selected->
            val mime=context.contentResolver.getType(selected.first)
            mime?.startsWith("image/")==true||selected.second.lowercase().let{name->
             listOf(".png",".jpg",".jpeg",".webp",".bmp",".gif",".heic",".heif").any(name::endsWith)
            }
           }
           val answer=if(selectedImage!=null&&activeModelVision){
            val prepared=withContext(Dispatchers.IO){
             VisionImagePreprocessor.prepare(context,selectedImage.first,selectedImage.second)
            }
            try{runtime.generateWithImage(prepared.absolutePath,prompt)}
            finally{prepared.delete()}
           }else{
            val safePrompt=if(selectedImage!=null)prompt+"\n\nImportant limitation: no compatible vision-language model is enabled for this chat. You may use the extracted OCR text and visible-label hints above, but do not claim to have visually inspected the image."
            else prompt
            runtime.generate(safePrompt)
           }
           db.chatDao().addMessage(MessageEntity(conversationId=target,body=answer.ifBlank{"The model returned an empty response."},role="assistant"))
          }catch(error:Throwable){db.chatDao().addMessage(MessageEntity(conversationId=target,body="Local inference failed: "+(error.message?:error.javaClass.simpleName),role="assistant",status="error"))}
         }
        }catch(error:Throwable){db.chatDao().addMessage(MessageEntity(conversationId=target,body="Attachment processing failed: "+(error.message?:error.javaClass.simpleName),role="assistant",status="error"))}finally{draft="";pendingAttachment=null;sending=false}
       }
      }
     })
    Section.AI_HUB->CollaborationScreen(models,collaborationPrompt,{collaborationPrompt=it},collaborationStatus,collaborating){
     val prompt=collaborationPrompt.trim()
     if(prompt.isNotBlank()&&!collaborating){
      collaborating=true
      scope.launch{
       val previousPath=activeModelPath
       val previousName=activeModelName
       try{
        if(previousPath!=null){runtime.unload();activeModelPath=null;activeModelName=null}
        val target=conversationId?:db.chatDao().addConversation(ConversationEntity(title="Model collaboration"))
        val provider=LiteRtModelProvider(context)
        val catalog=ModelCatalog()
        models.filter{it.modelPath!=null&&File(it.modelPath).isFile}.distinctBy{it.modelPath}.forEach{model->
         catalog.register(ModelDescriptor(model.uri,model.displayName,provider.providerId,setOf(Capability.CHAT),true,localPath=model.modelPath))
        }
        val result=CollaborationEngine(listOf(provider),catalog).execute(
         InferenceRequest(prompt,target),
         setOf(Capability.CHAT),
         provider
        ){progress->collaborationStatus=progress}
        when(result){
         is CollaborationResult.Success->{
          val report=buildString{
           append("TWO-MODEL REVIEW\n\n")
           result.participants.forEachIndexed{index,answer->
            append("Independent model ").append(index+1).append(": ").append(answer.modelId).append(" (").append(answer.elapsedMs).append(" ms)\n")
            append(answer.text).append("\n\n")
           }
           append("COORDINATOR SYNTHESIS\n").append(result.finalAnswer)
          }
          db.chatDao().addMessage(MessageEntity(conversationId=target,body=report,role="assistant"))
          selectedConversation=target
          section=Section.CHATS
          collaborationStatus="Review completed. The report was saved to this conversation."
         }
         is CollaborationResult.Unavailable->collaborationStatus=result.reason
        }
       }catch(error:Throwable){
        collaborationStatus="Collaboration failed: "+(error.message?:error.javaClass.simpleName)
       }finally{
        if(previousPath!=null&&File(previousPath).isFile){
         try{runtime.load(previousPath);activeModelPath=previousPath;activeModelName=previousName}
         catch(error:Throwable){activeModelPath=null;activeModelName=null;runtimeMessage="Could not restore active model: "+(error.message?:error.javaClass.simpleName)}
        }
        collaborating=false
       }
      }
     }
    }
    Section.MODELS->ModelsScreen(
     models,activeModelPath,activeModelName,runtimeMessage,loadingModel,downloadingModel,downloadProgress,downloadStatus,
     embeddingModelPath?.let(::File)?.isFile==true&&embeddingVocabPath?.let(::File)?.isFile==true,embeddingStatus,
     {embeddingModelPicker.launch(arrayOf("application/octet-stream","*/*"))},
     {embeddingVocabPicker.launch(arrayOf("text/plain","*/*"))},
     {modelPicker.launch(arrayOf("*/*"))},
     {url,checksum,name->
      if(!downloadingModel){
       downloadingModel=true;downloadProgress=0f;downloadStatus="Connecting to HTTPS model host…"
       scope.launch{
        try{
         val result=VerifiedModelDownloader(context).download(url,checksum,name){downloaded,total->
          downloadProgress=if(total!=null&&total>0)(downloaded.toFloat()/total).coerceIn(0f,1f)else 0f
          downloadStatus=if(total!=null)"Downloaded "+formatBytes(downloaded)+" / "+formatBytes(total)else "Downloaded "+formatBytes(downloaded)
         }
         db.modelDao().add(LocalModelEntity("sha256:"+result.sha256,name,"application/octet-stream",result.bytes,modelPath=result.file.absolutePath))
         downloadStatus="Verified SHA-256 "+result.sha256.take(16)+"…"
         runtimeMessage="Downloaded and verified "+name+". Tap Load model to activate it."
        }catch(error:Throwable){downloadStatus="Download failed: "+(error.message?:error.javaClass.simpleName)}
        finally{downloadingModel=false}
       }
      }
     },
     {model->

     if(model.modelPath!=null){loadingModel=true;scope.launch{
      try{val visionEnabled=model.modelPath==visionModelPath;runtime.load(model.modelPath,enableVision=visionEnabled);activeModelPath=model.modelPath;activeModelName=model.displayName;activeModelVision=visionEnabled;runtimeMessage=if(visionEnabled)"Loaded "+model.displayName+" with local image input enabled." else "Loaded "+model.displayName+". Text chat is ready offline."}
      catch(error:Throwable){activeModelPath=null;activeModelName=null;activeModelVision=false;runtimeMessage="Could not load model: "+(error.message?:error.javaClass.simpleName)}
      finally{loadingModel=false}
     }}
    },{model->scope.launch{if(activeModelPath==model.modelPath){runtime.unload();activeModelPath=null;activeModelName=null;activeModelVision=false};if(visionModelPath==model.modelPath){visionPrefs.edit().remove("vision_model_path").apply();visionModelPath=null};model.modelPath?.let{File(it).delete()};db.modelDao().remove(model.uri)}},visionModelPath,{path->
      val next=if(visionModelPath==path)null else path
      visionPrefs.edit().putString("vision_model_path",next).apply()
      visionModelPath=next
      if(activeModelPath==path){scope.launch{runtime.unload();activeModelPath=null;activeModelName=null;activeModelVision=false;runtimeMessage="Vision setting changed. Reload this model to apply it."}}
     }, { entry, file ->
      scope.launch { db.modelDao().add(LocalModelEntity("catalog:"+entry.id+":"+entry.version,entry.name,"application/octet-stream",file.length(),modelPath=file.absolutePath)) }
     })
    Section.TRAINING->TrainingLabScreen(
     datasetName,datasetReport,datasetStatus,datasetBusy,trainingJob,trainingStatus,trainingBusy,
     {datasetPicker.launch(arrayOf("*/*"))},
     {datasetExport.launch("astronaut-cleaned-dataset.jsonl")},
     {serverUrl,apiKey,baseModel,method,epochs->
      if(!trainingBusy){
       trainingBusy=true;trainingStatus="Preparing secure upload…";trainingJob=null
       scope.launch{
        try{
         val report=datasetReport?:error("Import a valid JSONL dataset first.")
         val invalid=report.invalidLines.toSet()
         val clean=datasetContent.lineSequence().mapIndexedNotNull{index,line->if(line.isNotBlank()&&index+1 !in invalid)line else null}.joinToString("\n")
         val client=TrainingApiClient(serverUrl,apiKey)
         val datasetId=client.uploadDataset(datasetName.ifBlank{"astronaut-dataset.jsonl"},clean)
         trainingStatus="Dataset uploaded to your configured workstation. Creating job…"
         var job=client.startJob(datasetId,baseModel,method,epochs)
         trainingJob=job;trainingStatus=job.message
         while(job.state=="queued"||job.state=="running"){
          delay(3_000)
          job=client.getJob(job.id)
          trainingJob=job
          trainingStatus=job.message
         }
         trainingStatus=job.message
        }catch(error:Throwable){trainingStatus="Training request failed: "+(error.message?:error.javaClass.simpleName)}
        finally{trainingBusy=false}
       }
      }
     },
     {serverUrl,apiKey,jobId->
      scope.launch{
       try{
        val job=TrainingApiClient(serverUrl,apiKey).cancelJob(jobId)
        trainingJob=job;trainingStatus=job.message
       }catch(error:Throwable){trainingStatus="Cancellation failed: "+(error.message?:error.javaClass.simpleName)}
      }
     }
    )
    Section.WORKSPACE->DeveloperStudioScreen()
    Section.SETTINGS->SettingsScreen(
     speechLanguageTag,ttsVoiceName,offlineTtsVoices,recognitionLanguages,speechStatus,
     {tag->try{
      val normalized=OfflineVoiceController.normalizeLanguageTag(tag)
      speechLanguageTag=normalized
      speechPrefs.edit().putString("language_tag",normalized).apply()
      voiceController.setLanguageTag(normalized)
      ttsVoiceName=voiceController.selectedTtsVoiceName()
      speechPrefs.edit().putString("tts_voice_name",ttsVoiceName).apply()
      speechStatus="Language set to $normalized. Check on-device language support."
     }catch(error:Throwable){speechStatus=error.message?: "Invalid language tag"}},
     {speechStatus="Checking installed and downloadable on-device language packs…";voiceController.checkRecognitionSupport{support->recognitionLanguages=support;speechStatus="Language support refreshed for $speechLanguageTag."}},
     {voiceController.downloadRecognitionLanguage{speechStatus=it}},
     {name->voiceController.setTtsVoice(name);ttsVoiceName=name;speechPrefs.edit().putString("tts_voice_name",name).apply();speechStatus="Offline voice selected."},
     {voiceController.speak("Astronaut offline speech is ready.")}
    )
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

@Composable private fun ChatScreen(conversations:List<ConversationEntity>,selectedId:Long?,messages:List<MessageEntity>,draft:String,onDraftChange:(String)->Unit,attachmentName:String?,activeModelName:String?,runtimeMessage:String,loading:Boolean,sending:Boolean,voiceListening:Boolean,onVoice:()->Unit,onSpeak:(String)->Unit,onAttach:()->Unit,onClearAttachment:()->Unit,onSelect:(Long)->Unit,onSend:()->Unit){
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
       if(!mine&&message.body.isNotBlank()) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){IconButton(onClick={onSpeak(message.body)}){Icon(Icons.Default.VolumeUp,"Read response aloud",tint=Green)}}
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
    IconButton(onClick=onVoice,enabled=!sending){Icon(if(voiceListening)Icons.Default.MicOff else Icons.Default.Mic,if(voiceListening)"Listening" else "Speak message",tint=if(voiceListening)Green else Muted)}
    TextField(draft,onDraftChange,Modifier.weight(1f),placeholder={Text("Message privately…")},maxLines=5,enabled=!sending,colors=TextFieldDefaults.colors(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
    IconButton(onClick=onSend,enabled=!sending&&(draft.isNotBlank()||attachmentName!=null)){if(sending)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Icon(Icons.Default.Send,"Send",tint=Green)}
   }
  }
  Text("Offline inference "+(if(activeModelName!=null)"· "+activeModelName else "· no model loaded"),style=MaterialTheme.typography.labelSmall,color=Muted,modifier=Modifier.padding(top=6.dp,bottom=8.dp))
 }
}

@Composable private fun ModelsScreen(models:List<LocalModelEntity>,activePath:String?,activeName:String?,status:String,loading:Boolean,downloading:Boolean,downloadProgress:Float,downloadStatus:String,embeddingReady:Boolean,embeddingStatus:String,onImportEmbeddingModel:()->Unit,onImportEmbeddingVocab:()->Unit,onImport:()->Unit,onDownload:(String,String,String)->Unit,onLoad:(LocalModelEntity)->Unit,onRemove:(LocalModelEntity)->Unit,visionModelPath:String?,onEnableVision:(String)->Unit,onCatalogInstalled:(org.offlineai.messenger.features.ModelCatalogEntry,File)->Unit){
 var showCatalog by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("On-device model library",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  Text("Import a .litertlm model. Model files stay in app-private storage and are not included in the APK.",color=Muted)
  OutlinedButton(onClick={showCatalog=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Verified,null);Spacer(Modifier.width(8.dp));Text("Browse verified model catalog")}
  if(showCatalog) AlertDialog(onDismissRequest={showCatalog=false},title={Text("Verified model catalog")},text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState())){CatalogLibraryPanel(onCatalogInstalled)}},confirmButton={TextButton(onClick={showCatalog=false}){Text("Close")}})
  Spacer(Modifier.height(14.dp));Button(onClick=onImport,modifier=Modifier.fillMaxWidth(),enabled=!downloading){Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp));Text("Import .litertlm model")}
  Spacer(Modifier.height(18.dp))
  Text("Semantic document search",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
  Text("Optional local embedding model. Use a MiniLM-compatible ONNX export with input_ids and attention_mask (int64), plus its matching WordPiece vocab.txt. Files stay on this device.",color=Muted,style=MaterialTheme.typography.bodySmall)
  OutlinedButton(onClick=onImportEmbeddingModel,modifier=Modifier.fillMaxWidth(),enabled=!downloading){Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp));Text("Import embedding model (.onnx)")}
  OutlinedButton(onClick=onImportEmbeddingVocab,modifier=Modifier.fillMaxWidth(),enabled=!downloading){Icon(Icons.Default.Description,null);Spacer(Modifier.width(8.dp));Text("Import matching vocab.txt")}
  Text(if(embeddingReady)"SEMANTIC RETRIEVAL CONFIGURED" else "BM25 fallback active",color=if(embeddingReady)Green else Muted,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
  Text(embeddingStatus,color=Muted,style=MaterialTheme.typography.bodySmall)

  var downloadUrl by remember{mutableStateOf("")}
  var downloadSha by remember{mutableStateOf("")}
  var downloadName by remember{mutableStateOf("model.litertlm")}
  Spacer(Modifier.height(16.dp))
  Text("Download and verify",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
  Text("Paste an HTTPS model URL and the SHA-256 published by its source. Downloads are rejected if the checksum does not match.",color=Muted,style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(downloadUrl,{downloadUrl=it},modifier=Modifier.fillMaxWidth(),label={Text("HTTPS download URL")},singleLine=true,enabled=!downloading)
  OutlinedTextField(downloadSha,{downloadSha=it},modifier=Modifier.fillMaxWidth(),label={Text("SHA-256 checksum (64 hex characters)")},singleLine=true,enabled=!downloading)
  OutlinedTextField(downloadName,{downloadName=it},modifier=Modifier.fillMaxWidth(),label={Text("Filename (.litertlm)")},singleLine=true,enabled=!downloading)
  Button(onClick={onDownload(downloadUrl.trim(),downloadSha.trim(),downloadName.trim())},modifier=Modifier.fillMaxWidth(),enabled=!downloading&&downloadUrl.startsWith("https://")&&downloadSha.matches(Regex("[A-Fa-f0-9]{64}"))&&downloadName.endsWith(".litertlm",true)){if(downloading)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Default.Download,null);Spacer(Modifier.width(8.dp));Text(if(downloading)"Downloading…" else "Download and verify")}
  if(downloading&&downloadProgress>0f)LinearProgressIndicator(progress=downloadProgress,modifier=Modifier.fillMaxWidth())
  if(downloadStatus.isNotBlank())Text(downloadStatus,color=Green,style=MaterialTheme.typography.bodySmall)
  Spacer(Modifier.height(10.dp));Text(status,color=Green,style=MaterialTheme.typography.bodySmall)
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
  Spacer(Modifier.height(10.dp))
  if(models.isEmpty())EmptyState("No model imported","Choose a compatible LiteRT-LM .litertlm file. A small model is best for phones.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(models,key={it.uri}){model->Card{Column(Modifier.fillMaxWidth().padding(14.dp)){
   Text(model.displayName,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
   Text(formatBytes(model.sizeBytes),color=Muted,style=MaterialTheme.typography.labelSmall)
   Text("Vision input is available only for compatible multimodal .litertlm models. Enable this only when the model publisher confirms image support.",color=Muted,style=MaterialTheme.typography.bodySmall)
   Row(verticalAlignment=Alignment.CenterVertically){
    if(activePath==model.modelPath)Text("ACTIVE · "+activeName,color=Green,modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelSmall) else Spacer(Modifier.weight(1f))
    TextButton(onClick={model.modelPath?.let(onEnableVision)?:Unit},enabled=model.modelPath!=null){Text(if(model.modelPath==visionModelPath)"Vision enabled" else "Enable vision")}
    TextButton(onClick={onRemove(model)}){Text("Remove")}
    Button(onClick={onLoad(model)},enabled=!loading&&model.modelPath!=null){Text(if(activePath==model.modelPath)"Reload" else "Load model")}
   }
  }} }}
 }
}
@Composable private fun CollaborationScreen(
 models:List<LocalModelEntity>,
 prompt:String,
 onPrompt:(String)->Unit,
 status:String,
 running:Boolean,
 onRun:()->Unit
){
 val available=models.filter{it.modelPath!=null}.distinctBy{it.modelPath}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Independent model review",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  Text("Astronaut runs two different local models one at a time, then asks a coordinator pass to compare their answers. This reduces peak RAM use; it can take several minutes on a phone.",color=Muted)
  Text("Available local models: "+available.size,style=MaterialTheme.typography.labelLarge,color=Green)
  available.forEach{model->Text("• "+model.displayName,style=MaterialTheme.typography.bodySmall,color=Muted)}
  OutlinedTextField(prompt,onPrompt,modifier=Modifier.fillMaxWidth(),label={Text("Task for both models")},minLines=3,maxLines=6,enabled=!running)
  Button(onClick=onRun,modifier=Modifier.fillMaxWidth(),enabled=!running&&prompt.isNotBlank()&&available.size>=2){
   if(running)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Icon(Icons.Default.Hub,null)
   Spacer(Modifier.width(8.dp));Text(if(running)"Reviewing…" else "Run two-model review")
  }
  Text(status,color=if(running)Green else Muted,style=MaterialTheme.typography.bodySmall)
  if(available.size<2)EmptyState("Two models required","Import at least two distinct .litertlm model files in Models. No model output is simulated.")
 }
}

@Composable private fun WorkspaceScreen(projects:List<WorkspaceProjectEntity>,onAdd:()->Unit,onOpen:(WorkspaceProjectEntity)->Unit,onDelete:(Long)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Developer workspace",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold);Text("Organise local projects and notes.",color=Muted)};FilledTonalButton(onClick=onAdd){Icon(Icons.Default.Add,null);Text("New")}}
  Spacer(Modifier.height(14.dp));if(projects.isEmpty())EmptyState("A space for your work","Create a project to keep its name and description on this device.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(projects,key={it.id}){project->Card{Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
   Icon(Icons.Default.Folder,null,tint=Green,modifier=Modifier.size(28.dp));Spacer(Modifier.width(12.dp))
   Column(Modifier.weight(1f)){Text(project.name,fontWeight=FontWeight.SemiBold);if(project.description.isNotBlank())Text(project.description,color=Muted,maxLines=2)}
   TextButton(onClick={onOpen(project)}){Text("Open assistant")}
   IconButton(onClick={onDelete(project.id)}){Icon(Icons.Default.DeleteOutline,"Delete project")}
  }} }}
 }
}
@Composable private fun TrainingLabScreen(
 datasetName:String,
 report:DatasetReport?,
 status:String,
 busy:Boolean,
 job:TrainingJobStatus?,
 jobStatus:String,
 trainingBusy:Boolean,
 onImport:()->Unit,
 onExport:()->Unit,
 onStartTraining:(String,String,String,String,Int)->Unit,
 onCancelTraining:(String,String,String)->Unit
){
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Training data lab",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  Text("Import JSONL examples, validate their structure and export a cleaned dataset. Files are processed on this device.",color=Muted)
  Button(onClick=onImport,modifier=Modifier.fillMaxWidth(),enabled=!busy){Icon(Icons.Default.FileOpen,null);Spacer(Modifier.width(8.dp));Text("Import JSONL dataset")}
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(report!=null){
   Text(datasetName,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
   Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Text("Valid records: "+report.validRecords,style=MaterialTheme.typography.titleMedium,color=Green)
     Text("Training split: "+report.trainRecords)
     Text("Validation split: "+report.validationRecords)
     Text("Invalid lines: "+report.invalidLines.size,color=if(report.invalidLines.isEmpty())Green else Color(0xFF9A4D27))
     if(report.invalidLines.isNotEmpty())Text("Invalid line numbers: "+report.invalidLines.take(20).joinToString(", "),color=Muted,style=MaterialTheme.typography.bodySmall)
    }
   }
   Button(onClick=onExport,modifier=Modifier.fillMaxWidth(),enabled=!busy&&report.validRecords>0){Icon(Icons.Default.FileDownload,null);Spacer(Modifier.width(8.dp));Text("Export cleaned JSONL")}
  }else EmptyState("No dataset loaded","Choose a JSONL file containing text, prompt, or messages fields.")
  if(status.isNotBlank())Text(status,color=Green,style=MaterialTheme.typography.bodySmall)
  Divider()
  Text("Optional workstation training",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
  Text("Training uploads the cleaned dataset to the HTTPS server you configure below. The API key is held only in this screen's memory and is not saved.",color=Muted,style=MaterialTheme.typography.bodySmall)
  var serverUrl by remember{mutableStateOf("")}
  var apiKey by remember{mutableStateOf("")}
  var baseModelPath by remember{mutableStateOf("")}
  var method by remember{mutableStateOf("lora")}
  var epochs by remember{mutableStateOf("1")}
  OutlinedTextField(serverUrl,{serverUrl=it},modifier=Modifier.fillMaxWidth(),label={Text("Training server HTTPS URL")},singleLine=true,enabled=!trainingBusy)
  OutlinedTextField(apiKey,{apiKey=it},modifier=Modifier.fillMaxWidth(),label={Text("Training API key")},singleLine=true,visualTransformation=PasswordVisualTransformation(),enabled=!trainingBusy)
  OutlinedTextField(baseModelPath,{baseModelPath=it},modifier=Modifier.fillMaxWidth(),label={Text("Server-local base model directory")},singleLine=true,enabled=!trainingBusy)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
   FilterChip(selected=method=="lora",onClick={method="lora"},label={Text("LoRA")},enabled=!trainingBusy)
   FilterChip(selected=method=="qlora",onClick={method="qlora"},label={Text("QLoRA")},enabled=!trainingBusy)
   OutlinedTextField(epochs,{epochs=it.filter(Char::isDigit)},modifier=Modifier.width(90.dp),label={Text("Epochs")},singleLine=true,enabled=!trainingBusy)
  }
  Button(onClick={onStartTraining(serverUrl.trim(),apiKey,baseModelPath.trim(),method,epochs.toIntOrNull()?:1)},modifier=Modifier.fillMaxWidth(),enabled=!trainingBusy&&report?.validRecords?.let{it>0}==true&&serverUrl.startsWith("https://")&&apiKey.length>=16&&baseModelPath.isNotBlank()&&(epochs.toIntOrNull()?:0) in 1..10){
   if(trainingBusy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Default.PlayArrow,null)
   Spacer(Modifier.width(8.dp));Text(if(trainingBusy)"Training job running…" else "Upload dataset and start training")
  }
  if(trainingBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(job!=null){
   Text("Job "+job.id+" · "+job.state.uppercase(),fontWeight=FontWeight.SemiBold)
   LinearProgressIndicator(progress=job.progress/100f,modifier=Modifier.fillMaxWidth())
   Text(job.message+" ("+job.progress.toInt()+"%)",color=Muted,style=MaterialTheme.typography.bodySmall)
   if(job.state=="queued"||job.state=="running")TextButton(onClick={onCancelTraining(serverUrl.trim(),apiKey,job.id)}){Text("Cancel training")}
   if(job.artifactUrl!=null)Text("Adapter artifact endpoint: "+serverUrl.trimEnd('/')+job.artifactUrl,color=Green,style=MaterialTheme.typography.bodySmall)
  }
  if(jobStatus.isNotBlank())Text(jobStatus,color=Green,style=MaterialTheme.typography.bodySmall)
  Text("The GPU worker runs on your workstation, not on Android. It returns a PEFT adapter and manifest; conversion to a directly loadable .litertlm model is a separate step.",color=Muted,style=MaterialTheme.typography.bodySmall)
 }
}

private const val MAX_DATASET_BYTES = 20 * 1024 * 1024
private suspend fun readDatasetBounded(context:Context,uri:Uri):String = withContext(Dispatchers.IO){
 val input=context.contentResolver.openInputStream(uri)?:error("Cannot open dataset file")
 input.use{stream->
  val output=ByteArrayOutputStream()
  val buffer=ByteArray(8192)
  var total=0
  while(true){
   val count=stream.read(buffer)
   if(count<0)break
   total+=count
   require(total<=MAX_DATASET_BYTES){"Dataset exceeds the 20 MiB import limit."}
   output.write(buffer,0,count)
  }
  String(output.toByteArray(),Charsets.UTF_8)
 }
}

@Composable private fun SettingsScreen(
 languageTag:String,
 selectedVoiceName:String?,
 voices:List<org.offlineai.messenger.features.OfflineTtsVoice>,
 recognition:org.offlineai.messenger.features.OfflineRecognitionLanguages,
 speechStatus:String,
 onLanguageChange:(String)->Unit,
 onCheckLanguages:()->Unit,
 onDownloadLanguage:()->Unit,
 onVoiceSelect:(String)->Unit,
 onTestVoice:()->Unit
){
 var languageDraft by remember(languageTag){mutableStateOf(languageTag)}
 var voiceMenu by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
  Text("Privacy & device",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
  InfoCard("Offline-first","Chat records and model files are stored on this device. Model generation runs locally.")
  InfoCard("No silent uploads","This app does not upload chats or connect to an account or server.")
  InfoCard("Model requirement","Obtain a compatible .litertlm model separately, then import it in Models. Chat generation works offline after model installation.")
  Spacer(Modifier.height(12.dp))
  Text("Offline voice",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
  Text("Speech recognition uses Android's on-device recognizer only. Text-to-speech is restricted to installed voices that do not require a network connection.",color=Muted,style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(languageDraft,{languageDraft=it},modifier=Modifier.fillMaxWidth(),label={Text("Speech language (BCP-47)")},placeholder={Text("en-US, hi-IN, bn-IN")},singleLine=true)
  Button(onClick={onLanguageChange(languageDraft)},modifier=Modifier.fillMaxWidth()){Text("Apply speech language")}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(onClick=onCheckLanguages,modifier=Modifier.weight(1f)){Text("Check packs")}
   Button(onClick=onDownloadLanguage,modifier=Modifier.weight(1f),enabled=recognition.downloadable.isNotEmpty()){Text("Download pack")}
  }
  if(recognition.installed.isNotEmpty())Text("Installed offline ASR: "+recognition.installed.joinToString(),color=Green,style=MaterialTheme.typography.bodySmall)
  if(recognition.downloadable.isNotEmpty())Text("Available to download: "+recognition.downloadable.joinToString(),color=Muted,style=MaterialTheme.typography.bodySmall)
  if(recognition.pending.isNotEmpty())Text("Pending: "+recognition.pending.joinToString(),color=Muted,style=MaterialTheme.typography.bodySmall)
  Text("Offline TTS voice",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
  Box{
   OutlinedButton(onClick={voiceMenu=true},modifier=Modifier.fillMaxWidth(),enabled=voices.isNotEmpty()){
    Text(voices.firstOrNull{it.name==selectedVoiceName}?.label?:if(voices.isEmpty())"No offline TTS voices installed" else voices.first().label)
   }
   DropdownMenu(expanded=voiceMenu,onDismissRequest={voiceMenu=false}){
    voices.forEach{voice->DropdownMenuItem(text={Text(voice.label)},onClick={voiceMenu=false;onVoiceSelect(voice.name)})}
   }
  }
  OutlinedButton(onClick=onTestVoice,modifier=Modifier.fillMaxWidth(),enabled=voices.isNotEmpty()){Icon(Icons.Default.VolumeUp,null);Spacer(Modifier.width(8.dp));Text("Test offline voice")}
  Text(speechStatus,color=Green,style=MaterialTheme.typography.bodySmall)
  InfoCard("Language pack downloads","On Android 13 and newer, Astronaut can ask the system speech provider to download supported on-device recognition languages. The system provider controls the actual download and availability. Android 12 can use an already-installed on-device recognizer but cannot expose the same pack-management API.")
 }
}
@Composable private fun InfoCard(title:String,body:String){Card(Modifier.fillMaxWidth().padding(bottom=10.dp)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(4.dp));Text(body,color=Muted,style=MaterialTheme.typography.bodyMedium)}}}
@Composable private fun EmptyState(title:String,body:String){Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,fontWeight=FontWeight.SemiBold);Text(body,color=Muted,style=MaterialTheme.typography.bodySmall)}}
private fun formatBytes(bytes:Long):String=when{bytes>=1073741824->"%.1f GB".format(bytes/1073741824.0);bytes>=1048576->"%.1f MB".format(bytes/1048576.0);bytes>=1024->"%.1f KB".format(bytes/1024.0);else->"$bytes B"}
