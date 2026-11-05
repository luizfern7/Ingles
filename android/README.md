# Desbloqueia · Concursos

Aplicativo Android nativo em Kotlin/Compose para transformar pequenas pausas em estudo. Este projeto está em `android/`, independente da página de flashcards `../index.html`, que foi preservada.

## Versão 1.1.0 — questão na tela após desbloquear

Agora há uma janela sobre os outros apps, ativada pelo usuário com a permissão **Exibir sobre outros apps** e um foreground service com notificação de parar. Abra Ajustes → Autorizar e ativar janela automática → A cada desbloqueio → Testar janela agora. Depois vá à tela inicial do aparelho, bloqueie e desbloqueie. A janela permite responder e ler explicações sem abrir o app manualmente. Pode ser fechada sem responder.

A atualização preserva os intervalos anteriores de 15/30/60 minutos; selecione A cada desbloqueio se quiser uma questão em cada ciclo. Depois de reiniciar o telefone ou encerrar o serviço, reabra o aplicativo. Veja [ANDROID.md](docs/ANDROID.md) e [UNLOCK_1_1_0.md](docs/UNLOCK_1_1_0.md) para permissões, estratégia, testes e limites.

## Histórico da revisão 1.0.1

Uma detecção `Trojan:Script/Ulthar.A!ml` foi relatada pelo Microsoft Defender para o APK de debug 1.0.0. A revisão e os ajustes pontuais estão em [SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md). A nova versão é compilada como release, sem depuração, ferramentas de inspeção Compose ou Activity de suporte a testes no pacote distribuído. Isso não demonstra que o alerta da Microsoft tenha sido resolvido.

O APK release continua assinado com a chave de desenvolvimento anterior para preservar compatibilidade de atualização. Não é uma versão com chave de produção da Play Store. Em outra máquina, a chave debug gerada pode ser diferente; a correspondência de certificados foi conferida somente entre os APKs produzidos neste ambiente. A revisão não adiciona rede, serviços permanentes ou permissões extras.

## Funcionalidades

- Questões offline com resultado imediato, análise de todas as alternativas, fonte e explicação didática autoral.
- Room com histórico por tentativa, sessões idempotentes e importação transacional de JSON; IDs existentes não são substituídos.
- Seleção por disciplina, itens inéditos, erros de revisão e tempo desde a última apresentação. Evita repetição imediata se houver outro item elegível.
- Início, resolução, estatísticas por disciplina e configurações em Jetpack Compose/Material 3, com temas claro, escuro e do sistema.
- Preferências DataStore: sons locais, feedback tátil, animações, disciplinas, a cada desbloqueio ou intervalo de 15/30/60 minutos e pausa de uma hora.
- Janela de estudo após desbloquear via permissão de sobreposição, mantida por um serviço em primeiro plano iniciado pelo usuário, com notificação persistente para parar. Há fallback para notificação caso a janela seja recusada. Nenhuma Activity é iniciada automaticamente do background.
- Estudo manual funciona independentemente dos lembretes. Não há permissão de internet, contas nem backend.

## Compilar e instalar

Requisitos: JDK completo 17 ou 21, Android SDK Platform 35 e Build Tools 34.0.0 (versão padrão do AGP 8.7.3), acesso aos repositórios oficiais na primeira compilação. O wrapper fixa Gradle 8.10.2 e seu checksum.

Em `android/`, configure `ANDROID_HOME` para o SDK ou crie `local.properties` com `sdk.dir=/caminho/do/sdk` (arquivo ignorado pelo Git). Aceite as licenças do SDK e instale as ferramentas, se necessário:

```sh
sdkmanager --licenses
sdkmanager 'platforms;android-35' 'build-tools;34.0.0' 'platform-tools'
./gradlew assembleRelease testDebugUnitTest lintRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Abra **Desbloqueia · Concursos** no aparelho. O APK release desta versão é assinado com a chave de desenvolvimento para manter continuidade com a versão anterior, não é uma versão de produção para publicação. O aplicativo não exige ativar lembretes para estudar. Para ativá-los, use Ajustes e autorize notificações e Exibir sobre outros apps. O serviço exige uma notificação visível de controle. A função pode ser desligada no mesmo local.

No ambiente desta tarefa, veja `docs/VALIDATION.md` para caminhos e resultados efetivamente observados. Não há configuração de proxy com credenciais no código. Gradle/Java podem precisar das propriedades padrão de proxy e do truststore corporativo em ambientes gerenciados; preserve a verificação TLS.

## Arquitetura

Um módulo `app` com pacotes de responsabilidades distintas:

- `data`: modelo serializável, validação de pacote, entidades/DAO Room, transações do repositório e preferências DataStore.
- `domain`: política de seleção e elegibilidade de lembretes, separadas do Android.
- `ui`: ViewModel, estado salvo, telas Compose e feedback local. Resposta fica bloqueada após correção; novas tentativas só em nova sessão.
- `unlock`: serviço de estudo em primeiro plano, receptor dinâmico, janela Compose e notificações de controle. Ciclos e sessões são persistidos em Room.

Não há migração destrutiva. Na versão 1, o banco é criado com schema 1; futuras alterações de schema precisam fornecer migração explícita. Importar pacotes não apaga questões nem histórico. O histórico e as preferências permanecem após fechamento, reinício ou atualização do app; desinstalação ou limpeza dos dados pelo Android apaga o armazenamento local. O backup automático do Android está desabilitado, com regras explícitas para backup e transferência de dados nas versões antigas e recentes.

## Conteúdo e importação

O pacote inicial contém **25 questões conferidas** da BASA/Cesgranrio 2013, não a conversão integral dos 19 PDFs. Veja [MATERIALS.md](docs/MATERIALS.md) e [materials.json](docs/materials.json) para curadoria, páginas, hashes e pendências de OCR/validação. A extração textual bruta dos 19 documentos está em `materials/extracted/`, fora dos assets do APK.

`app/src/main/assets/questions.json` é um exemplo completo do formato aceito:

```json
{"schema_version": 1, "questions": []}
```

O array deve ser não vazio em pacotes reais. Cada item tem os campos em português exigidos no pedido, `fonte` estruturada com páginas e SHA-256, `status_validacao: "gabarito_conferido"` e `autoria_explicacao`. Alternativas e explicações são mapas com chaves A–E (2 a 5 opções). A resposta deve existir entre elas. O importador recusa versão desconhecida, IDs duplicados, questões não conferidas, textos vazios, hash malformado ou explicações incompletas. Limites: 5 MB e 5.000 questões por pacote. Validação de estrutura não substitui a revisão editorial: quem cria o pacote deve conferir o gabarito na prova correta. IDs existentes são ignorados para preservar a interpretação do histórico.

Use Ajustes → Importar pacote de questões, via seletor de arquivos do Android. O APK já inclui o pacote inicial, sem necessidade de importação manual. Arquivos inválidos mostram um erro e não alteram o banco.

## Validação em aparelho

[ANDROID.md](docs/ANDROID.md) descreve a estratégia e as limitações do sistema. Antes de distribuir, instale em aparelhos/emuladores reais e confira:

1. Primeiro uso sem internet; responder certo/errado, analisar alternativas, concluir e continuar.
2. Reabrir e girar a tela durante e após uma resposta; verificar histórico e ausência de contagem duplicada.
3. Ativar/desativar notificações, testar desbloqueio e eventos duplicados, intervalo, pausa e processo encerrado. Após force-stop, abrir o app novamente.
4. Tema do sistema, tela pequena, escala de fonte ampliada, leitor de tela, sons/vibração opcionais, volume zero e modo silencioso.
5. Importação válida/inválida e repetida, mantendo o progresso.
6. Chamadas e alarmes: nenhuma interface automática ou som deve interromper a utilização do aparelho.

Os testes locais não substituem essa validação física.
