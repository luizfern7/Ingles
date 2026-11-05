# Questão na tela após desbloquear

Na edição pessoal 1.2.0, instale **Desbloqueia · Meu acervo** e importe seu JSON primeiro: o banco inicial é vazio. O pacote é separado do app anterior e suas permissões também são separadas. Veja MEU_ACERVO_1_2_0.md.

## Experiência e ativação

As versões 1.0.x publicavam uma notificação para tocar. A versão 1.1.0 acrescenta uma **janela real sobre os outros apps** após o desbloqueio, com alternativas, correção e explicações na própria janela. Não abre Activity arbitrariamente do background e não substitui a tela de bloqueio do Android.

No aparelho:

1. Abra a edição instalada. Na edição pessoal, importe um JSON antes de ativar. Futuras atualizações da mesma edição preservam o histórico.
2. Abra Desbloqueia → Ajustes → **Autorizar e ativar janela automática**.
3. Autorize notificações e **Exibir sobre outros apps** na tela do Android. Volte ao aplicativo.
4. Selecione **A cada desbloqueio**. Preferências anteriores de 15/30/60 minutos são preservadas na atualização; nesse caso, troque o intervalo se desejar exibir em todos os ciclos.
5. Toque **Testar janela agora**. Esse teste é manual e ignora o intervalo, sem simular um desbloqueio.
6. Feche a janela, vá à tela inicial do aparelho, bloqueie e desbloqueie. Para parar, desative a opção em Ajustes ou use **Parar** na notificação persistente.

Há botão de fechar fixo no topo, além de Concluir e fechar na própria resolução. Não é obrigatório responder para usar o aparelho. A janela ocupa 85% da altura e não toma foco do teclado; controles e navegação do sistema continuam disponíveis.

## Estratégia oficial do Android

`UnlockStudyService` é um foreground service **specialUse**, iniciado da Activity visível após opt-in e permissões. Esse trabalho é contínuo por escolha do usuário: manter o receptor ativo para apresentar uma questão local no evento de desbloqueio. É mantida uma notificação persistente e silenciosa com ação de parar. Não foi usado tipo incorreto como dataSync, mediaPlayback ou location.

Permissões adicionais em 1.1.0:

- `SYSTEM_ALERT_WINDOW`: acesso especial concedido pelo usuário no Android; necessário para `TYPE_APPLICATION_OVERLAY`.
- `FOREGROUND_SERVICE`: permite o serviço com notificação visível.
- `FOREGROUND_SERVICE_SPECIAL_USE`: declaração do tipo em Android 14+. O Manifest contém `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` com a finalidade do serviço.

Não há acessibilidade, captura de tela, uso de localização, leitura de chamadas, listener de notificações, instalação de outros pacotes, permissão INTERNET ou pedido de exclusão da otimização de bateria. Uma distribuição na Play Store exige declaração e revisão do uso do foreground service e das permissões aplicáveis; não se afirma aprovação pela loja.

O receptor de `SCREEN_OFF` e `USER_PRESENT` é registrado dinamicamente **no serviço**, não no Manifest nem no Application. O serviço não é exportado. Os eventos de tela são broadcasts protegidos pelo Android. Uma fila serializa seu processamento: desligar a tela avança o ciclo persistido em Room, e desbloquear reserva uma única sessão respeitando pausa, disciplinas e frequência. O modo “A cada desbloqueio” usa intervalo zero, mas mantém a deduplicação por ciclo.

A janela é adicionada diretamente via WindowManager como `TYPE_APPLICATION_OVERLAY`, mediante autorização do usuário. Android 11+ usa contexto de janela associado ao display e métricas da janela. A composição tem lifecycle e saved-state owners próprios, descartados ao fechar ou encerrar o serviço. Nenhuma Activity é iniciada automaticamente; a notificação pode abrir o app somente mediante toque.

## Proteções e limitações

- Não exibe questão por cima da tela bloqueada ou com tela apagada.
- Não abre outra janela se já houver uma, nem cobre o próprio app durante desbloqueio. O botão manual de teste pode exibir a janela enquanto Ajustes está visível.
- Não exibe durante modo de áudio de chamada/comunicação. Enquanto a janela está aberta, uma verificação a cada 500 ms a remove se houver bloqueio, tela apagada, chamada/comunicação ou revogação da permissão. Essa verificação só roda enquanto a janela existe. Alarmes e peculiaridades de telefonia de fabricantes ainda precisam de testes físicos; não existe garantia universal para toda atividade de terceiros.
- Apagar a tela remove a janela imediatamente; responder e fechar persistem os dados sem bloquear o acesso ao aparelho.
- Se adicionar a janela falhar, há fallback para uma notificação silenciosa que abre a sessão mediante toque.
- O serviço usa `START_NOT_STICKY` e não se inicia no boot. Depois de reiniciar o aparelho, force-stop, interrupção do serviço pelo sistema ou por políticas do fabricante, abra o aplicativo novamente para retomar. Nenhum mecanismo ignora uma parada solicitada pelo usuário.
- Android Go e alguns aparelhos restringem ou não oferecem sobreposição. Se a permissão não puder ser concedida, o estudo manual continua disponível.
- Aparelhos podem ocultar sobreposições em telas protegidas, como pagamentos, permissões e outros fluxos de segurança. O app não contorna essa proteção.

## Evidência de validação

Veja `UNLOCK_1_1_0.md` para os resultados da execução desta versão. Os testes simulam Android API 28 e 35 no Robolectric, incluindo a cadeia serviço → receptor → janela e o botão Parar. Isso não substitui testes em um telefone físico. Não é possível garantir comportamento idêntico ao WordBit ou em todos os fabricantes sem validação nos respectivos aparelhos.

Referências oficiais:

- https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY
- https://developer.android.com/develop/background-work/services/fgs/service-types#special-use
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/background-tasks/broadcasts
