# Validação — janela automática 1.1.0

## Correção da experiência

As versões 1.0.x exigiam tocar em uma notificação. A versão 1.1.0 implementa apresentação direta de uma questão em `TYPE_APPLICATION_OVERLAY` ao receber `ACTION_USER_PRESENT`, quando o usuário habilitou a função e concedeu as permissões. Um foreground service especial de estudo mantém o receptor ativo; não se tenta abrir uma Activity do background. Veja `ANDROID.md` para ativação e limites.

Mudanças desta versão: serviço não exportado de tipo specialUse, solicitação guiada de sobreposição, notificação persistente com Parar, janela Compose com fechamento fixo, botão Testar janela agora, intervalo A cada desbloqueio, processamento serializado de eventos e proteções contra tela apagada/bloqueada, chamadas e duplicidades. A preferência antiga de frequência não é apagada; usuários que tinham 30 minutos precisam selecionar A cada desbloqueio se quiserem esse comportamento.

## Build e testes observados

Com JDK completo, SDK 35 e truststore/proxy configurados no ambiente:

```sh
./gradlew assembleRelease testDebugUnitTest lintRelease \
  testReleaseUnitTest \
  --tests br.com.desbloqueia.StudyTest \
  --tests br.com.desbloqueia.AppStartupTest \
  --tests br.com.desbloqueia.OverlayTest
```

Resultado final: `BUILD SUCCESSFUL`.

- Debug: **32 execuções de teste, zero falhas, erros e ignorados**. Incluem 15 casos de conteúdo/persistência/seleção, 1 abertura real da Activity em Robolectric, 2 de resolução Compose e 14 execuções de sobreposição.
- Release distribuído: **30 execuções aprovadas**, com StudyTest, AppStartupTest e OverlayTest. Os testes Compose isolados com Activity auxiliar ficam na variante debug.
- `OverlayTest`: 7 cenários executados em cada uma das APIs **28 e 35** (Android 9 e Android 15): condições de elegibilidade; tipo/flags da janela; frequência zero com deduplicação e pausa; apresentação/fechamento/remoção ao apagar a tela; evento de desbloqueio mostra uma questão sem toque e evita repetição no ciclo; serviço iniciado registra receptor, recebe broadcasts de tela, apresenta a questão e o botão Parar desativa a preferência; ausência de permissão impede a janela.
- `lintRelease`: nenhum problema. Há avisos do compilador somente pelo uso de uma API de shadow depreciada nos testes Robolectric; não são chamadas depreciadas do código do aplicativo.

Nos testes de serviço, foi usado WindowManager real do ambiente Robolectric com lifecycle/saved-state owners da composição, e o broadcast foi enviado ao receptor registrado pelo serviço. A ação de Parar foi ajustada para retirar a janela imediatamente, antes de concluir a gravação assíncrona da preferência.

**São testes simulados, não ensaios em telefone físico.** Não foi feito teste real de bloqueio/desbloqueio, gerenciamento de bateria de fabricantes, reboot, telefonia, alarmes ou áudio. O botão Testar janela agora permite a primeira checagem no aparelho do usuário.

## Integridade e atualização

- versionCode 3, versionName 1.1.0, minSdk 26, targetSdk 35, variante release sem depuração.
- Mesmo certificado SHA-256 das versões anteriores; o banco Room permanece schema 1. Atualize sem desinstalar para preservar o progresso.
- Pacote de 25 questões e 125 justificativas mantido.
- Sem permissão INTERNET ou acessibilidade. As três novas permissões são SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE e FOREGROUND_SERVICE_SPECIAL_USE; são necessárias para o modo automático agora solicitado e estão documentadas em Ajustes.
- Auditoria estrutural de Manifest, componentes, assets, assinatura e bibliotecas nativas em `../../downloads/review/1.1.0/`.

## Análise antivírus local

O APK final foi examinado com ClamAV 1.5.4, base diária 28171 de 01/11/2026, na mesma imagem oficial e isolamento usados na revisão anterior. Resultado: **1 arquivo examinado, 0 detecções, nenhum erro de leitura**; 49,04 MiB de dados examinados a partir de 7,07 MiB de APK. O relatório está em `../../downloads/review/1.1.0/clamav.txt`.

Não se afirma que a base estivesse atualizada até o dia da execução. Não houve teste pelo Microsoft Defender neste ambiente Linux. Esse resultado não garante ausência de malware nem aprovação do Defender; mantenha a proteção se um novo alerta ocorrer. Nenhum arquivo foi submetido a serviços de análise externos.

SHA-256 do APK final:

```text
1e59c1532e9665d80f48967652645c7b28f7bf1382057842833ce1e864eb13a4
```
