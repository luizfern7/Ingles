package br.com.desbloqueia.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.desbloqueia.data.*
import br.com.desbloqueia.domain.FlashcardDraft
import java.text.DateFormat
import java.util.Date

fun reviewDate(time: Long): String=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(time))
@Composable internal fun StudyContent(state: QuestionUi,settings: Settings,answer: (String)->Unit,next: ()->Unit,done: ()->Unit,close: ()->Unit,flip: ()->Unit,rate: (Boolean)->Unit,suggest: ((Question)->Unit)?=null) {
    if(state.flash!=null) FlashcardScreen(state,settings,flip,rate,next,done,close)
    else QuestionScreen(state,settings,answer,next,done,close,suggest)
}
@Composable internal fun FlashcardScreen(state: QuestionUi,settings: Settings,flip: ()->Unit,rate: (Boolean)->Unit,next: ()->Unit,done: ()->Unit,close: ()->Unit) {
    val card=state.flash?.card?:return
    val angle by animateFloatAsState(if(state.flipped)180f else 0f,tween(if(settings.animations)350 else 0),label="virar flashcard")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("FLASHCARD · ${card.disciplina}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        card.assunto?.takeIf {it.isNotBlank() && it!=card.assunto_id}?.let {Text(it,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        card.subassunto?.let {Text(it)}
        Card(Modifier.fillMaxWidth().heightIn(min=200.dp).graphicsLayer {rotationY=angle;cameraDistance=12*density}.clickable(enabled=!state.busy,onClick=flip)) {
            Column(Modifier.padding(24.dp).graphicsLayer {rotationY=if(angle>90)180f else 0f},verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(if(angle>90)"VERSO" else "FRENTE",style=MaterialTheme.typography.labelMedium)
                Text(if(angle>90)card.verso else card.frente,style=MaterialTheme.typography.titleLarge)
                if(angle>90 && !card.explicacao.isNullOrBlank()) Text(card.explicacao!!)
            }
        }
        Button(onClick=flip,enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text(if(state.flipped) "Voltar à frente" else "Virar cartão")}
        if(state.flipped && state.flashRating==null) {
            Text("Agora avalie o que lembrou:")
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={rate(false)},enabled=!state.busy && angle>=179.9f,modifier=Modifier.weight(1f)) {Text("Não sei")}
                Button(onClick={rate(true)},enabled=!state.busy && angle>=179.9f,modifier=Modifier.weight(1f)) {Text("Sei")}
            }
        } else if(state.flashRating!=null) {
            Text(if(state.flashRating==true)"✓ Conhecimento registrado!" else "Vamos revisar em breve.",fontWeight=FontWeight.Bold)
            state.nextDue?.let {Text("Próxima revisão: ${reviewDate(it)}")}
            Button(onClick=next,modifier=Modifier.fillMaxWidth()) {Text("Próximo item")}
            OutlinedButton(onClick=done,modifier=Modifier.fillMaxWidth()) {Text("Concluir")}
        }
        state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        TextButton(onClick=close,modifier=Modifier.fillMaxWidth()) {Text("Fechar e usar o celular")}
    }
}
@Composable internal fun FlashcardDraftDialog(draft: FlashcardDraft,onDismiss: ()->Unit,onSave: (String,String,String,String,Boolean)->Unit) {
    var topic by rememberSaveable(draft.sourceQuestion.id) {mutableStateOf(draft.topic)}
    var front by rememberSaveable(draft.sourceQuestion.id) {mutableStateOf(draft.front)}
    var back by rememberSaveable(draft.sourceQuestion.id) {mutableStateOf(draft.back)}
    var explanation by rememberSaveable(draft.sourceQuestion.id) {mutableStateOf(draft.explanation)}
    var reviewed by rememberSaveable(draft.sourceQuestion.id) {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Revisar sugestão local")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Texto extraído do gabarito do pacote, sem IA ou validação automática. Edite perguntas ambíguas antes de salvar. Um ID de assunto estável é obrigatório; ele não é deduzido pelo nome.")
        OutlinedTextField(value=topic,onValueChange={topic=it},readOnly=draft.topic.isNotBlank(),label={Text("assunto_id")})
        OutlinedTextField(value=front,onValueChange={front=it},label={Text("Frente")})
        OutlinedTextField(value=back,onValueChange={back=it},label={Text("Verso")})
        OutlinedTextField(value=explanation,onValueChange={explanation=it},label={Text("Explicação")})
        Row {Checkbox(checked=reviewed,onCheckedChange={reviewed=it});Text("Conferi o conteúdo e declaro revisado")}
    }},confirmButton={TextButton(onClick={onSave(topic,front,back,explanation,reviewed)},enabled=topic.isNotBlank() && front.isNotBlank() && back.isNotBlank()) {Text(if(reviewed)"Salvar revisado" else "Salvar pendente")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancelar")}})
}
