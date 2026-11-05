package br.com.desbloqueia.unlock

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.desbloqueia.data.Settings
import br.com.desbloqueia.ui.QuestionScreen
import br.com.desbloqueia.ui.QuestionUi

@Composable internal fun OverlayContent(state: QuestionUi,settings: Settings,answer: (String)->Unit,next: ()->Unit,close: ()->Unit) {
    val dark=settings.theme=="dark" || (settings.theme=="auto" && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(dark) darkColorScheme() else lightColorScheme(primary=Color(0xFF286640))) {
        Surface(Modifier.fillMaxSize(),tonalElevation=8.dp) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("DESBLOQUEIA",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp))
                    TextButton(onClick=close) {Text("Fechar")}
                }
                HorizontalDivider(Modifier.padding(bottom=12.dp))
                Box(Modifier.weight(1f)) { QuestionScreen(state,settings,answer,next,close,close) }
            }
        }
    }
}
