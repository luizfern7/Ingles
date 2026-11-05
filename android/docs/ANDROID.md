# Integração com desbloqueio

A versão 1 não abre Activity automaticamente em segundo plano. Usa `ACTION_SCREEN_OFF` e `ACTION_USER_PRESENT` em um `BroadcastReceiver` registrado pelo `Application`, e oferece uma notificação silenciosa, privada e de baixa importância que abre a questão mediante toque do usuário. Não há receiver de desbloqueio no Manifest, acessibilidade, overlay, full-screen intent, acesso a chamadas ou solicitação de exclusão da otimização de bateria.

Esse receptor depende de o processo estar vivo. O Android e fabricantes podem encerrá-lo; após morte de processo ou reinicialização do aparelho, abra o aplicativo novamente. A notificação de atalho já publicada pode continuar disponível, mas não mantém o processo vivo. Removê-la não é comando para desativar a preferência: isso é feito em Ajustes. **Não se promete detecção de todos os desbloqueios.** O aplicativo permanece uma ferramenta completa de estudo manual. Não foi adotado foreground service permanente: a observação contínua de desbloqueios não justifica simular outro tipo de serviço em versões recentes do Android.

O usuário ativa a função explicitamente. Em Android 13+, há pedido de `POST_NOTIFICATIONS`; se recusado, o estudo manual permanece disponível. Canais podem ser desativados no sistema. Os eventos são protegidos pelo Android; o registro é exportado para recebê-los do sistema, não aceita comandos arbitrários para iniciar Activity.

O ciclo avança em `SCREEN_OFF`. Room persiste ciclo, último ciclo atendido, instante do último lembrete e a sessão pendente em transação. O intervalo de 15, 30 ou 60 minutos e a pausa são verificados antes de reservar a sessão. Um ID imutável associa a notificação à questão e só pode ser consumido uma vez. Repetições de `USER_PRESENT` no mesmo ciclo não criam outra sessão. Ao contrário de um processo vivo, esse estado persiste após reinício. Ajustes de relógio para trás podem prolongar conservadoramente o intervalo até o relógio alcançar o instante armazenado.

Notificações não tocam, não vibram, não acendem uma tela de questão e não usam heads-up por padrão. Não se publica lembrete enquanto o dispositivo ainda está bloqueado ou o áudio está em modo de chamada/comunicação. Não se pede acesso ao estado telefônico. Não há mecanismo capaz de garantir detecção de toda atividade relevante, inclusive alarmes de terceiros; a proteção principal é não assumir a tela nem emitir som/vibração nos lembretes. Sons e vibração de resposta só acontecem após ação na tela do app. Sons respeitam modo silencioso/vibração, volume multimídia e roteamento do sistema. Vibração usa feedback tátil do sistema.

Referências oficiais para as restrições adotadas:
- https://developer.android.com/develop/background-work/background-tasks/broadcasts
- https://developer.android.com/guide/components/activities/background-starts
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/develop/ui/views/notifications/notification-permission

Compatibilidade declarada: Android 8 (API 26) ou posterior; compile/target API 35. Não foi feita validação física de desbloqueio, sons, vibração, chamadas ou particularidades de fabricantes. Não há permissão INTERNET no Manifest: o conteúdo e o histórico funcionam offline.
