# Autoria de IA e dicas opcionais — validação 1.3.0

Atualização da edição Meu acervo: applicationId br.com.desbloqueia.personal, versionCode 5 e versionName 1.3.0. Sem questões embarcadas e sem novas permissões. A origem autoral e a dica são conteúdos fornecidos no JSON, não respostas obtidas pela rede. O banco Room mantém schema 1, o certificado de atualização permanece o mesmo e questões/histórico existentes são preservados.

## Comportamento implementado

- `fonte.tipo: "autoral_ia"` aceita `descricao` textual, sem exigir PDF, SHA-256 ou dados de concurso.
- O status autoral é `revisao_pendente` ou `revisado`. Questões pendentes mostram aviso antes e depois de responder; revisão declarada pelo autor não é validação feita pelo app. A correção segue a letra informada no pacote.
- Pacotes oficiais anteriores, sem o campo tipo, continuam sendo lidos como oficiais. Metadados oficiais completos e gabarito_conferido permanecem exigidos nesse caminho.
- `dica` é opcional. Ausente, null, vazia ou só espaços: não há ícone/botão. Com texto, há lâmpada e Ver dica antes da resposta. Abrir/ocultar só altera a apresentação local, sem gravar resposta ou revelar o gabarito. O campo usa texto simples, inclusive texto complementar de apoio.
- Estudo manual e janela automática compartilham QuestionScreen, portanto usam as mesmas regras de dica e identificação de autoria.

Formato e prompt: JSON_AUTORAL_IA.md, examples/autoral-ia.json e PROMPT_GERAR_QUESTOES_IA.txt.

## Validação observada

Com o SDK/JDK e proxy/truststore já preparados, o wrapper executou:

```sh
./gradlew assembleRelease testDebugUnitTest lintRelease \
  testReleaseUnitTest \
  --tests br.com.desbloqueia.StudyTest \
  --tests br.com.desbloqueia.AppStartupTest \
  --tests br.com.desbloqueia.OverlayTest \
  --tests br.com.desbloqueia.EmptyDatabaseTest \
  --tests br.com.desbloqueia.AiPackTest
```

Resultado: BUILD SUCCESSFUL; **55 execuções debug aprovadas e 48 release aprovadas**, sem falhas, erros ou ignorados. Lint release sem problemas. Avisos de compilador existentes são apenas da API depreciada de shadow nos testes de interação Robolectric.

AiPackTest roda nas APIs 28 e 35: importação autoral sem PDF/hash/páginas, leitura dos pacotes oficiais antigos, dica ausente/null/vazia/espaços, status revisado declarado e recusa de status oficial indevido, recusa de descrição ausente/tipo desconhecido/resposta inválida/justificativas incompletas, persistência da origem/dica e do progresso após fechar e reabrir o banco em disco.

QuestionUiTest inclui: revelar/ocultar a dica antes da resposta sem chamar correção nem exibir Entenda a resposta; não renderizar o botão para dica nula, vazia ou só espaços; aviso de revisão pendente visível antes de responder. Os testes de banco vazio e de sobreposição das versões anteriores continuam passando. São testes simulados; não houve teste físico em aparelho.

O exemplo JSON dos downloads é idêntico ao fixture validado pelos testes. O exemplo e o prompt ficam fora do APK e não são importados automaticamente.

## Integridade e análise local

A auditoria confirmou pacote pessoal, zero questões embarcadas, variante release sem depuração, permissões esperadas, assinatura válida e certificado igual ao da versão 1.2.0.

ClamAV 1.5.4, base diária 28171 de 01/11/2026: **1 arquivo examinado e 0 detecções**, 48,99 MiB examinados a partir de 7,07 MiB de APK. Execução local sem rede e com arquivo somente para leitura; não houve envio a serviços externos. Não se afirma que a base estivesse atualizada até a data de execução nem que o Microsoft Defender tenha aprovado o arquivo. Isso não garante ausência de malware.

SHA-256:

```text
58cfe9b1c4fbb8c31f5ee80e5bc1b6dc0825ccfc8720034d4d9dcd8a0851f956
```

Relatórios em ../../downloads/review/1.3.0/. As limitações de sobreposição, permissões e manutenção do serviço seguem ANDROID.md.
