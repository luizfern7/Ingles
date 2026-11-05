# Desbloqueia · Meu acervo

Aplicativo Android nativo em Kotlin/Compose para transformar pequenas pausas em estudo. Este projeto está em `android/`, independente da página de flashcards `../index.html`, que foi preservada.

## Versão 1.3.0 — autoria por IA e dica opcional

Aceita `fonte: {"tipo":"autoral_ia","descricao":"..."}` com `status_validacao: "revisao_pendente"` ou `"revisado"`, sem exigir PDF, hash ou dados de concurso. Questões pendentes são identificadas na tela; a correção segue o gabarito fornecido no JSON, sem verificação por uma IA dentro do aplicativo.

O campo opcional `dica` mostra uma lâmpada com **Ver dica** antes da resposta. Ausente, nulo, vazio ou só espaços: não mostra ícone nem botão. Os pacotes oficiais anteriores continuam compatíveis. Veja [JSON_AUTORAL_IA.md](docs/JSON_AUTORAL_IA.md), o [exemplo importável](docs/examples/autoral-ia.json) e o [prompt para gerar pacotes](docs/PROMPT_GERAR_QUESTOES_IA.txt).

É uma atualização da edição pessoal, mantendo `br.com.desbloqueia.personal`, o banco, as preferências e o histórico. Continua sem questões embarcadas.

## Edição pessoal introduzida em 1.2.0 — banco inicial vazio

Nenhuma questão acompanha o APK. O primeiro uso mostra **Importar meu primeiro JSON**; todo o conteúdo é escolhido e importado pelo usuário. Depois da importação, estudo manual, explicações, estatísticas e janela ao desbloquear funcionam como antes.

Nome no celular: **Desbloqueia · Meu acervo**, pacote `br.com.desbloqueia.personal`. É uma instalação separada da edição anterior para garantir banco inicial vazio sem apagar o progresso antigo. Abra o novo ícone; se mantiver a edição antiga, desative o modo automático nela se não quiser suas janelas.

Veja [MEU_ACERVO_1_2_0.md](docs/MEU_ACERVO_1_2_0.md) para o comportamento sem conteúdo e a validação desta entrega. O formato do JSON continua compatível com os pacotes anteriores. Nenhuma rotina preenche o banco automaticamente.

## Modo de desbloqueio introduzido na versão 1.1.0

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

Abra **Desbloqueia · Meu acervo** no aparelho e importe seu JSON. O app antigo permanece separado. O APK release usa uma chave de desenvolvimento; não é uma versão assinada para publicação na Play Store. Por ter identificação própria, esta edição não substitui a instalação anterior. O aplicativo não exige ativar lembretes para estudar. Para ativá-los, use Ajustes e autorize notificações e Exibir sobre outros apps. O serviço exige uma notificação visível de controle. A função pode ser desligada no mesmo local.

No ambiente desta tarefa, veja `docs/MEU_ACERVO_1_2_0.md` para os resultados atuais e `docs/VALIDATION.md` para o registro histórico das ferramentas. Não há configuração de proxy com credenciais no código. Gradle/Java podem precisar das propriedades padrão de proxy e do truststore corporativo em ambientes gerenciados; preserve a verificação TLS.

## Arquitetura

Um módulo `app` com pacotes de responsabilidades distintas:

- `data`: modelo serializável, validação de pacote, entidades/DAO Room, transações do repositório e preferências DataStore.
- `domain`: política de seleção e elegibilidade de lembretes, separadas do Android.
- `ui`: ViewModel, estado salvo, telas Compose e feedback local. Resposta fica bloqueada após correção; novas tentativas só em nova sessão.
- `unlock`: serviço de estudo em primeiro plano, receptor dinâmico, janela Compose e notificações de controle. Ciclos e sessões são persistidos em Room.

Não há migração destrutiva. Na versão 1, o banco é criado com schema 1; futuras alterações de schema precisam fornecer migração explícita. Importar pacotes não apaga questões nem histórico. O histórico e as preferências permanecem após fechamento, reinício ou atualização do app; desinstalação ou limpeza dos dados pelo Android apaga o armazenamento local. O backup automático do Android está desabilitado, com regras explícitas para backup e transferência de dados nas versões antigas e recentes.

## Conteúdo e importação

O APK não inclui banco de questões. Use a tela inicial → **Importar meu primeiro JSON** → **Importar pacote de questões**, ou acesse esse controle em Ajustes. O formato continua sendo um objeto com `schema_version: 1` e um array `questions` não vazio para pacotes importáveis.

Cada item mantém os campos `id`, `disciplina`, `assunto`, `enunciado`, `alternativas`, `resposta_correta`, `explicacao`, `explicacoes_alternativas`, `fonte`, `status_validacao` e `autoria_explicacao`. Alternativas e explicações são mapas A–E (2 a 5 opções), com a resposta entre as letras presentes. Para origem oficial, Fonte inclui concurso, banca, cargo, ano, arquivo, SHA-256 real e páginas/número/versão, com status `gabarito_conferido`. Para `autoral_ia`, bastam tipo e descricao, com status `revisao_pendente` ou `revisado`; não invente dados de prova. O campo `dica` é opcional e não aparece na tela quando ausente/nulo/vazio. Disciplina e assunto são livres, sem lista fixa.

O importador recusa versão desconhecida, IDs duplicados dentro do pacote, status incompatíveis com o tipo de fonte, textos obrigatórios vazios, hash malformado em fonte oficial e explicações incompletas. Limites: 5 MB e 5.000 questões por pacote. IDs que já existem no banco são ignorados para preservar a interpretação do histórico. Uma importação inválida não altera o banco. Arquivos vazios não adicionam conteúdo e são recusados.

As questões que antes vinham embarcadas agora ficam apenas em `app/src/test/resources/questions_fixture.json`, fora do APK. Os materiais e relatórios das primeiras versões são históricos e não representam conteúdo incluído nesta edição.

## Validação em aparelho

[ANDROID.md](docs/ANDROID.md) descreve a estratégia e as limitações do sistema. Antes de distribuir, instale em aparelhos/emuladores reais e confira:

1. Primeiro uso sem internet; responder certo/errado, analisar alternativas, concluir e continuar.
2. Reabrir e girar a tela durante e após uma resposta; verificar histórico e ausência de contagem duplicada.
3. Ativar/desativar notificações, testar desbloqueio e eventos duplicados, intervalo, pausa e processo encerrado. Após force-stop, abrir o app novamente.
4. Tema do sistema, tela pequena, escala de fonte ampliada, leitor de tela, sons/vibração opcionais, volume zero e modo silencioso.
5. Importação válida/inválida e repetida, mantendo o progresso.
6. Chamadas e alarmes: nenhuma interface automática ou som deve interromper a utilização do aparelho.

Os testes locais não substituem essa validação física.
