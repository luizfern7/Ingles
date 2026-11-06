# Validação de flashcards — 1.4.0

## Resultado e compatibilidade

Aplicativo atualizado no mesmo módulo/arquitetura, applicationId br.com.desbloqueia.personal, versionCode 6 e versionName 1.4.0. Preserva o componente de questões/dicas, o serviço de sobreposição, fontes de IA, preferências e estatísticas anteriores. Nenhuma nova permissão ou dependência de rede/IA foi acrescentada. O APK continua sem banco de conteúdo embarcado; os exemplos são arquivos de importação manual fora dele.

## Verificações executadas

O wrapper do projeto concluiu assembleRelease, testDebugUnitTest, lintRelease e testReleaseUnitTest com sucesso.

- **84 execuções de teste debug aprovadas**, 0 falhas, 0 erros e 0 ignorados.
- **76 execuções release aprovadas**, 0 falhas, 0 erros e 0 ignorados. Os testes que usam Activity auxiliar de Compose ficam apenas na debug; o fluxo real da Activity com flashcard é exercitado também na release.
- **Lint release sem problemas**.
- Avisos de compilador limitados a métodos de shadow depreciados usados nos testes Robolectric existentes.

Cenários novos e preservados:

- Todos os percentuais de 0 a 100 em passos de 10: proporção exata em 100 seleções com ambos os grupos elegíveis, alternância padrão, modos exclusivos e ausência de um grupo sem dívida de quota.
- Persistência do crédito do modo misto e da sessão pendente de flashcard ao recriar o repositório; deduplicação do ciclo de desbloqueio e consumo único da sessão.
- Vínculo de assuntos normalizados, prioridade de cartões relacionados a erros, manutenção da quota, cartões sem associação, filtro de disciplinas, cooldown e não repetição imediata quando há alternativas.
- Intervalos Sei de 1/3/7/14/30 dias, repetição em 30 após o teto, Não sei em 10 minutos, contadores, estado memorizado/aprendendo e bloqueio da avaliação antes de virar. Duplicar a avaliação não gera segunda revisão.
- Conteúdo e progresso de flashcards persistidos após fechar e reabrir o banco em disco.
- DataStore: mixed/50 por padrão, preferência de método/percentual gravada em arquivo, leitura de um snapshot por uma nova instância de DataStore e edições concorrentes preservando outros campos.
- JSONs completos antigos, dicas, fontes de IA, formato compacto com índice base zero, flashcards independentes e pacotes mistos; recusa de IDs/conteúdos duplicados no pacote e campos inválidos.
- Classificação explícita de questão antiga sem alterar gabarito/respostas; erros anteriores são vinculados ao assunto e trazem para revisão cartões ainda não revisados após aquele erro.
- Sugestão local não salva automaticamente, contém contexto completo e fica pendente por padrão; conteúdo repetido é ignorado.
- Activity real em Robolectric com acervo só de flashcards: importar, abrir, esconder verso antes de virar, virar e registrar Sei sem precisar de questões.
- Janela real de WindowManager em Robolectric, APIs 28 e 35: evento de desbloqueio abre flashcard mesmo com zero questões; fechar/apagar a tela remove a janela. Os testes anteriores de notificações/serviço/Parar continuam passando.

Esses são testes automatizados locais e simulados. Não houve instalação ou teste físico em aparelhos/OEMs, desbloqueio real, reboot, chamadas, alarmes, áudio ou vibração. As limitações de permissões/serviço continuam descritas em ANDROID.md.

## Migração Room 1 → 2

MIGRATION_1_2 está registrada no builder do app. O teste MigrationTest cria o esquema SQLite da versão 1 com dados nas quatro tabelas originais e abre o banco com Room versão 2, exigindo validação integral do esquema pelo Room.

Em APIs 28 e 35, o teste confirmou a preservação do payload da questão, do registro de resposta correta, da apresentação, do ciclo de desbloqueio e da sessão pendente. Novas tabelas começam vazias; pendingKind assume question para o conteúdo legado. Não há remoção de tabelas antigas, alteração de letras de gabarito, reset de preferências, limpeza de progresso ou fallback destrutivo.

Tabelas novas: flashcards, flash_progress, flash_reviews, flash_presentations, study_mix. Colunas adicionadas: questions.topicId, attempts.topicId e unlock_state.pendingKind, com índices correspondentes. Questões sem assunto_id permanecem sem vínculo.

## Principais arquivos alterados

- Models.kt/FlashModels.kt/Migrations.kt: modelo, DAO, estado de revisão, crédito do misto e migração aditiva.
- PackCodec.kt: normalização/validação dos formatos antigos, compacto e flashcards; fingerprint de duplicidade.
- Repository.kt: importação atômica, classificação, seleção mista, revelação/avaliação idempotente e sessões de desbloqueio de ambos os tipos.
- Preferences.kt: método/proporção no DataStore e edição atômica.
- domain/FlashLearning.kt: seletor proporcional, repetição espaçada e interface de sugestões locais.
- FlashcardScreen.kt, StudyViewModel.kt e StudyApp.kt: cartões, revisão manual, configurações, estatísticas e editor de sugestão.
- UnlockStudyService.kt/OverlayContent.kt, MainActivity.kt e StudyApplication.kt: integração de ambos os conteúdos ao mecanismo existente e registro da migração.

## Integridade e análise antivírus

Assinatura do APK validada, mesmo certificado da versão anterior, pacote preservado e release sem depuração. Auditoria confirmou permissões esperadas e ausência de JSONs de questões/flashcards ou fixtures nos assets do APK (somente perfis ART; há metadado técnico Kotlin fora dos assets).

SHA-256 do APK final:

```text
de43e1bbeaa6edc6565ba4deae516b999932d9f655047425106a3fc28b74b7fa
```

ClamAV 1.5.4, base diária 28171 de 01/11/2026: 1 arquivo examinado, 0 detecções, sem falhas de leitura; 49,46 MiB examinados a partir de 7,14 MiB de APK. Execução sem rede e com APK somente para leitura, sem submissão a terceiros. Não se afirma atualização da base até a data da execução nem aprovação do Microsoft Defender; um resultado negativo não garante ausência de malware.

Relatórios: ../../downloads/review/1.4.0/. Horários dos logs de scanner são UTC.
