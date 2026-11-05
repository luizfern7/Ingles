package br.com.desbloqueia.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.desbloqueia.StudyApplication
import br.com.desbloqueia.data.*

private val Green=Color(0xFF286640)
private val Red=Color(0xFFAB303B)
@Composable fun StudyApp(model: StudyViewModel,onClose: ()->Unit) {
    val settings by model.settings.collectAsStateWithLifecycle()
    val state by model.ui.collectAsStateWithLifecycle()
    val stats by model.stats.collectAsStateWithLifecycle()
    val reviews by model.reviews.collectAsStateWithLifecycle()
    val disciplines by model.disciplines.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("Início") }
    val context=LocalContext.current
    val view=LocalView.current
    val feedback=remember { Feedback(context) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    LaunchedEffect(model) { model.feedback.collect { feedback.play(it,model.settings.value,view) } }
    LaunchedEffect(state.session?.id) { if(state.session!=null) tab="Questão" }
    val dark=settings.theme=="dark" || (settings.theme=="auto" && isSystemInDarkTheme())
    val colors=if(dark) darkColorScheme(primary=Color(0xFF99D5A5),background=Color(0xFF101914),surface=Color(0xFF17231C)) else lightColorScheme(primary=Green,background=Color(0xFFF5F7F1),surface=Color(0xFFFCFDF8),secondary=Color(0xFF506653))
    MaterialTheme(colorScheme=colors) {
        Scaffold(bottomBar={
            NavigationBar { listOf("Início","Estatísticas","Ajustes").forEachIndexed { index,name ->
                NavigationBarItem(selected=tab==name,onClick={tab=name},icon={ Text(listOf("⌂","▥","⚙")[index]) },label={Text(name)})
            } }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("DESBLOQUEIA",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text("Um passo de cada vez.",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
                    TextButton(onClick=onClose) { Text("Fechar") }
                }
                message?.let { text -> Card(Modifier.fillMaxWidth().padding(bottom=8.dp)) { Column(Modifier.padding(12.dp)) { Text(text);TextButton(onClick={model.message.value=null}) {Text("Entendi")} } } }
                when(tab) {
                    "Questão" -> QuestionScreen(state,settings,model::answer,model::next,{model.clearQuestion();tab="Início"},onClose)
                    "Estatísticas" -> StatisticsScreen(stats,reviews)
                    "Ajustes" -> SettingsScreen(settings,disciplines,model)
                    else -> HomeScreen(stats,reviews,ready,settings,{model.next()},{tab="Questão"})
                }
            }
        }
    }
}
@Composable private fun HomeScreen(stats: List<Stats>,reviews: Int,ready: Boolean,settings: Settings,start: ()->Unit,resume: ()->Unit) {
    val answered=stats.sumOf { it.answered };val correct=stats.sumOf { it.correct }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("PEQUENAS PAUSAS, GRANDES CONQUISTAS",style=MaterialTheme.typography.labelMedium)
                Text("Sua próxima\nconquista começa\ncom uma questão.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                Text("Responda, entenda cada alternativa e avance no seu ritmo. Tudo funciona offline.")
                Button(onClick=start,enabled=ready,modifier=Modifier.fillMaxWidth()) { Text(if(ready) "Estudar agora →" else "Preparando questões…") }
                TextButton(onClick=resume) { Text("Voltar à questão em andamento") }
            }
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { Metric("Respostas","$answered",Modifier.weight(1f));Metric("Acertos","$correct",Modifier.weight(1f));Metric("Revisão","$reviews",Modifier.weight(1f)) } }
        item { Text("Aprender, não decorar",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Cada resposta revela uma explicação e a análise de todas as opções. As questões mantêm a identificação da prova e do gabarito.",Modifier.padding(top=8.dp)) }
        item { Card { Column(Modifier.padding(16.dp)) { Text(if(settings.unlock) "Lembretes ativados" else "Você controla suas pausas",fontWeight=FontWeight.Bold);Text("A detecção de desbloqueio depende de o processo estar ativo. O Android pode encerrá-lo. O estudo manual continua disponível.") } } }
        item { Text("Acervo inicial: 25 questões conferidas • BASA 2013. Outros 18 PDFs e os itens restantes aguardam curadoria. Comentários didáticos autorais.",style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(16.dp)) }
    }
}
@Composable private fun Metric(label: String,value: String,modifier: Modifier=Modifier) { Card(modifier) { Column(Modifier.padding(14.dp)) { Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(label,style=MaterialTheme.typography.labelMedium) } } }
@Composable internal fun QuestionScreen(state: QuestionUi,settings: Settings,answer: (String)->Unit,next: ()->Unit,done: ()->Unit,close: ()->Unit) {
    val session=state.session
    if(session==null) { Column(verticalArrangement=Arrangement.spacedBy(16.dp)) { Text(state.error?:"Pronto para uma pequena conquista?");Button(onClick=next,enabled=!state.busy) { Text("Carregar questão") };TextButton(onClick=close) { Text("Fechar e usar o celular") } };return }
    val q=session.question
    var expanded by rememberSaveable(session.id) { mutableStateOf(false) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text(q.disciplina.uppercase(),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Text(q.assunto,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${q.fonte.concurso} · ${q.fonte.banca} · ${q.fonte.ano}\n${q.fonte.cargo} · Questão ${q.fonte.questao} · ${q.fonte.versao}",style=MaterialTheme.typography.bodySmall) }
        item { Text(q.enunciado,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.padding(vertical=8.dp)) }
        q.alternativas.toSortedMap().forEach { (letter,text) -> item(key=letter) {
            val revealed=state.selected!=null
            val correct=revealed && letter==q.resposta_correta
            val wrong=revealed && letter==state.selected && !correct
            val target=when {correct->Green;wrong->Red;else->MaterialTheme.colorScheme.outlineVariant}
            val border by animateColorAsState(target,animationSpec=tween(if(settings.animations) 220 else 0),label="resultado")
            Surface(onClick={answer(letter)},enabled=!revealed && !state.busy,shape=RoundedCornerShape(16.dp),border=BorderStroke(if(correct||wrong) 2.dp else 1.dp,border),color=if(correct||wrong) border.copy(alpha=0.10f) else MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) { Text(if(correct) "✓ $letter" else if(wrong) "✕ $letter" else letter,fontWeight=FontWeight.Bold);Text(text,Modifier.weight(1f)) }
            }
        } }
        state.error?.let { error -> item { Text(error,color=MaterialTheme.colorScheme.error) } }
        item { AnimatedVisibility(visible=state.selected!=null,enter=fadeIn(tween(if(settings.animations) 220 else 0)),exit=fadeOut(tween(0))) {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(state.selected==q.resposta_correta) "✓ Resposta correta!" else "Resposta incorreta · Correta: ${q.resposta_correta}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=if(state.selected==q.resposta_correta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Card { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Entenda a resposta",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(q.explicacao);Text(q.autoria_explicacao,style=MaterialTheme.typography.bodySmall) } }
                OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) { Text(if(expanded) "Recolher alternativas ↑" else "Analisar alternativas ↓") }
                if(expanded) q.explicacoes_alternativas.toSortedMap().forEach { (letter,text) -> Text("$letter · ${if(letter==q.resposta_correta) "Correta" else "Incorreta"}\n$text",Modifier.padding(vertical=4.dp)) }
                Text("Fonte: ${q.fonte.arquivo}, página ${q.fonte.pagina}; gabarito na página ${q.fonte.pagina_gabarito}. Gabarito conferido no documento anexado.",style=MaterialTheme.typography.bodySmall)
                Button(onClick=next,enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Próxima questão")}
                OutlinedButton(onClick=done,modifier=Modifier.fillMaxWidth()) {Text("Concluir")}
            }
        } }
        item { TextButton(onClick=close,modifier=Modifier.fillMaxWidth()) {Text("Fechar e usar o celular")};Spacer(Modifier.height(12.dp)) }
    }
}
@Composable private fun StatisticsScreen(stats: List<Stats>,reviews: Int) {
    val count=stats.sumOf { it.answered };val correct=stats.sumOf { it.correct }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("Seu caminho até aqui",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Metric("Respostas","$count",Modifier.weight(1f));Metric("Acertos","$correct",Modifier.weight(1f));Metric("Erros","${count-correct}",Modifier.weight(1f))} }
        item { Text("Aproveitamento: ${if(count==0) 0 else correct*100/count}% · $reviews questões para revisar") }
        if(stats.isEmpty()) item {Text("Seu histórico começa com a primeira resposta. Que tal estudar agora?")}
        stats.forEach { stat -> item { Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(stat.discipline,fontWeight=FontWeight.Bold);Text("${stat.correct} acertos de ${stat.answered} respostas");LinearProgressIndicator(progress={stat.correct.toFloat()/stat.answered},modifier=Modifier.fillMaxWidth())}} } }
        item { Text("Respostas repetidas contam como novas tentativas. A fila de revisão considera o resultado mais recente de cada questão.",style=MaterialTheme.typography.bodySmall) }
    }
}
@Composable private fun SettingsScreen(settings: Settings,disciplines: List<String>,model: StudyViewModel) {
    val context=LocalContext.current
    val app=context.applicationContext as StudyApplication
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) model.update {it.copy(unlock=true)} else model.message.value="Notificações não autorizadas. O estudo manual continua disponível."
    }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) model.import(uri) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("Do seu jeito",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold) }
        item { Toggle("Questões após desbloquear",settings.unlock) { checked ->
            if(!checked) model.update { it.copy(unlock=false) }
            else if(Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else if(!app.monitor.canNotify()) model.message.value="Ative as notificações nas configurações do Android."
            else model.update {it.copy(unlock=true)}
        } }
        item { Text("O Android limita a abertura de telas em segundo plano. Você receberá uma notificação silenciosa para tocar e responder, somente enquanto o processo do app estiver ativo. Após encerramento ou reinicialização do aparelho, abra o app novamente. Não há serviço permanente, sobreposição ou uso de acessibilidade.",style=MaterialTheme.typography.bodySmall) }
        item { TextButton(onClick={context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE,context.packageName))}) {Text("Configurar notificações no Android")} }
        item { Text("Intervalo mínimo entre lembretes");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(15,30,60).forEach { minutes -> FilterChip(selected=settings.frequency==minutes,onClick={model.update {it.copy(frequency=minutes)}},label={Text("$minutes min")}) } } }
        item { OutlinedButton(onClick={model.update {it.copy(pausedUntil=if(it.pausedUntil>System.currentTimeMillis()) 0 else System.currentTimeMillis()+60*60_000)}}) {Text(if(settings.pausedUntil>System.currentTimeMillis()) "Retomar lembretes" else "Pausar por 1 hora")} }
        item { HorizontalDivider();Toggle("Sons de resposta",settings.sound) {v->model.update {it.copy(sound=v)}} }
        item { Toggle("Vibração",settings.vibration) {v->model.update {it.copy(vibration=v)}} }
        item { Toggle("Animações adicionais",settings.animations) {v->model.update {it.copy(animations=v)}} }
        item { Text("Aparência");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("auto" to "Sistema","light" to "Claro","dark" to "Escuro").forEach { (value,label) -> FilterChip(selected=settings.theme==value,onClick={model.update {it.copy(theme=value)}},label={Text(label)})}} }
        item { HorizontalDivider();Text("Disciplinas",style=MaterialTheme.typography.titleLarge);Text("Sem seleção, todas as disciplinas participam da fila.",style=MaterialTheme.typography.bodySmall) }
        disciplines.forEach { discipline -> item { Toggle(discipline,discipline in settings.disciplines) { checked -> model.update {it.copy(disciplines=if(checked) it.disciplines+discipline else it.disciplines-discipline)} } } }
        item { HorizontalDivider();Text("Seu acervo, offline",style=MaterialTheme.typography.titleLarge);Text("Importe pacotes JSON no formato documentado. São aceitas somente questões com gabarito conferido e explicações completas. IDs existentes são preservados.");OutlinedButton(onClick={importer.launch(arrayOf("application/json","text/plain"))}) {Text("Importar pacote de questões")};Spacer(Modifier.height(16.dp)) }
    }
}
@Composable private fun Toggle(label: String,checked: Boolean,onChange: (Boolean)->Unit) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f));Switch(checked=checked,onCheckedChange=onChange)} }
