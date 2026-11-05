# Resultados observados

## Build e ferramentas

Build executado com sucesso no ambiente desta tarefa pelo **wrapper do projeto**:

```sh
cd /workspace/Ingles/android
JAVA_HOME='/workspace/toolchains/jdk-21.0.5+11' \
GRADLE_USER_HOME=/workspace/toolchains/gradle-home \
./gradlew -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  assembleDebug testDebugUnitTest lintDebug
```

Resultado final: `BUILD SUCCESSFUL`, 56 tarefas (13 executadas e 43 atualizadas). O SDK está em `/workspace/toolchains/android-sdk`, indicado por `local.properties` ignorado pelo Git. JDK Temurin 21.0.5+11 completo, Gradle 8.10.2, AGP 8.7.3, Kotlin 2.0.21, SDK Platform 35 e Build Tools 34.0.0. As distribuições do JDK/Gradle foram conferidas contra SHA-256 oficial; as command-line tools do SDK foram conferidas contra SHA-1 do repositório oficial Google. O SDK Manager instalou os demais componentes pelo fluxo oficial, mantendo TLS.

O Java que já existia na imagem não possuía `javac`; foi necessário instalar o JDK completo. Para o ambiente gerenciado, propriedades padrão do proxy foram gravadas em `/workspace/toolchains/gradle-home/gradle.properties`, fora do repositório e sem credenciais. O truststore do sistema foi passado ao Gradle. Os testes propagam propriedades de rede/truststore quando presentes; não desativam verificação TLS. A primeira execução dos testes Robolectric precisou baixar o runtime Android de teste.

## Testes efetivamente executados

**17 testes, 0 falhas, 0 erros e 0 ignorados**, com Robolectric API 28:

- `StudyTest`: 14 testes. Leitura do asset com 25 questões; correspondência dos 25 gabaritos; recusa de importação inválida e sem efeito parcial; IDs duplicados; questões não conferidas e explicações ausentes; reimportação idempotente e preservação de histórico; adição de pacote; correção e bloqueio de resposta duplicada; prioridade de inéditas; revisão de erros; filtro de disciplinas e repetição imediata; opt-in/intervalo/pausa; deduplicação de ciclo e consumo único da notificação após recriar o repositório; fechamento/reabertura de banco Room em disco com histórico e ciclo preservados.
- `QuestionUiTest`: 2 testes Compose. Toque em alternativa chama a correção; resultado incorreto apresenta gabarito, explicação e expansão das justificativas de todas as alternativas, com rolagem da lista.
- `AppStartupTest`: 1 teste Compose com `StudyApplication` e `MainActivity` reais em Robolectric. Importação inicial, criação do ViewModel, abertura da tela inicial e início de estudo com a opção de fechar.

Os testes Compose usam renderização nativa de Robolectric. Não são testes em um telefone físico nem em um emulador Android completo. Os erros iniciais de configuração do runner e dos seletores em listas virtualizadas foram corrigidos; os resultados acima pertencem à execução final com o pacote revisado.

## Análise estática e artefato

`lintDebug` completou sem erros. Há um aviso `DataExtractionRules`, que recomenda regras explícitas de extração de dados para Android 12+. O Manifest usa `allowBackup=false`; não há sincronização/backup oferecido pelo aplicativo. Particularidades de transferência entre aparelhos de fabricantes não foram testadas.

APK: `app/build/outputs/apk/debug/app-debug.apk`. `apksigner verify --verbose` confirmou assinatura APK v2 válida. `aapt dump badging` confirmou pacote `br.com.desbloqueia`, versão 1.0.0, minSdk 26, targetSdk 35. Permissões: notificações, vibração e a permissão interna de receiver criada pelo AndroidX; sem permissão INTERNET.

As extrações e hashes dos 19 PDFs foram preservados. O APK contém somente o pacote editorial de 25 questões da BASA 2013, com 125 justificativas de alternativas. A geração do pacote foi executada a partir do PDF original. `../index.html` permanece sem alterações.

## Limites da validação

`adb devices` não encontrou aparelho conectado; `/dev/kvm` não está disponível. Não foram executados testes físicos de instalação/abertura do APK, desbloqueio, notificações, reinicialização de aparelho, chamadas, alarmes, áudio, vibração, OEMs ou versões Android recentes. Instalação e essas capacidades precisam de validação em aparelhos. A assinatura e a compilação confirmam um APK de debug produzido, não substituem essa validação.

A curadoria integral das demais questões dos 19 PDFs permanece pendente conforme `MATERIALS.md`. Não há questões inéditas geradas ou gabaritos inferidos para preencher lacunas.
