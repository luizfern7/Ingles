# Flashcards e estudo misto — 1.4.0

Esta versão atualiza Meu acervo (br.com.desbloqueia.personal), preservando questões, respostas, dicas, origem de IA, configurações e o serviço de sobreposição. Continua sem conteúdo embarcado: os exemplos são downloads para importação manual.

## Como usar

Importe um JSON de questões, flashcards ou ambos em Ajustes. Em **Métodos de estudo**, escolha Apenas questões, Apenas flashcards ou Modo misto. O padrão é misto, 50%/50%; o slider muda a participação dos flashcards de 0 a 100%, em passos de 10%. As preferências ficam no DataStore.

A distribuição usa crédito acumulado determinístico, persistido no Room. Com ambos os grupos elegíveis, 50/50 alterna questão/cartão; em blocos de 10 itens, os percentuais múltiplos de 10 são exatos. Cada seleção registra a apresentação, mesmo se o usuário fechar sem responder. Desbloqueio e estudo manual possuem contadores separados. **Revisar flashcards** é uma ação manual explícita, independente da quota automática.

Entre 10% e 90%, se faltar um tipo de conteúdo elegível (inclusive por disciplina ou agenda), o misto usa o outro e não acumula dívida de quota. Em 0%/100% ou nos modos exclusivos, não troca a modalidade escolhida. Não antecipa revisões futuras apenas para atingir a porcentagem. Se não houver item elegível, nenhuma janela vazia é aberta.

O serviço mantém os mesmos controles de permissão, bloqueio de tela, áudio de chamada, pausa, frequência e Parar. Não há nova permissão nem API de IA. A notificação de fallback abre o tipo correto de sessão. Após reboot/force-stop ou encerramento pelo fabricante, ainda é necessário reabrir o app.

## Associação por assunto e prioridade

`assunto_id` é um identificador global e estável. É normalizado para minúsculas; espaços e hífens viram underscores; caracteres fora de a-z, números e underscores são recusados. Máximo 120 caracteres. Exemplos: sfn_cmn e matematica_juros_compostos. Use o mesmo ID para o mesmo conceito em todos os lotes; não reutilize o ID para conceitos diferentes. A associação não depende de igualdade textual de disciplina, assunto ou subassunto.

Questões podem omitir o ID: continuam funcionando, sem vínculo. Flashcards exigem o ID. Uma questão se associa a todos os cartões do assunto e cada cartão pode reforçar várias questões desse assunto.

Um erro de questão é registrado uma vez por apresentação, com o assunto. Cartões correspondentes com agenda futura são trazidos para agora e passam à frente na próxima vaga de flashcard. A quota não muda. O reforço por erro é consumido quando o cartão é revisado depois do erro. A seleção evita o último cartão se houver outra opção elegível; um cartão único não avaliado tem cooldown de um minuto. Cartões com Não sei vencidos têm precedência sobre novos e cartões já conhecidos. Sem cartões associados, a resposta e o estudo continuam normalmente.

Para classificar uma questão antiga, reimporte o mesmo ID com assunto_id: o vínculo só é acrescentado se a questão ainda não tiver classificação e enunciado/alternativas/gabarito forem idênticos. O conteúdo e as respostas não são substituídos; o assunto também é associado ao histórico antigo. Questões já classificadas não são remapeadas silenciosamente.

## Interface e repetição espaçada

A frente aparece primeiro; **Virar cartão** mostra verso e explicação opcional, com animação que respeita as preferências. **Sei/Não sei** só podem ser usados depois de revelar a resposta; a regra também é verificada no repositório. Cada apresentação gera no máximo uma revisão, mesmo com toques repetidos ou restauração de estado. Som e feedback tátil reutilizam as preferências do app.

Agenda determinística de dois botões:

- Não sei: sequência de acertos volta a zero; revisão em 10 minutos; estado aprendendo.
- Sei consecutivos: intervalos de 1, 3, 7, 14 e 30 dias; após isso, mantém 30 dias.
- A partir de três Sei consecutivos: estado memorizado. Um Não sei volta a aprendendo. Isso mede a sequência declarada pelo usuário, não garante domínio real.

Room armazena apresentação, resposta revelada, histórico de avaliações, acertos/erros, sequência, estado, última e próxima revisão. Os intervalos são durações de 24 horas por dia; a data exibida usa o fuso do aparelho. Eventos de aprendizagem usam timestamps estritamente crescentes para evitar ambiguidade em relógios iguais/retrocedidos.

A tela inicial oferece **Revisar flashcards** mesmo sem desbloquear. Estatísticas preservam as medidas de questões e acrescentam cartões estudados, memorizados, disponíveis para revisão, avaliações Sei/Não sei, dificuldades por assunto e evolução nos últimos sete dias com atividade. As métricas são recalculadas a partir do histórico persistido. Cartões futuros aguardam a agenda; não são reapresentados só para manter a quota.

## JSON e compatibilidade

Raiz com schema_version 1 ou 2 e arrays questions e/ou flashcards. Pelo menos um item; limite combinado 5.000 itens e 5 MB. IDs duplicados no mesmo grupo do pacote são recusados. IDs existentes preservam conteúdo/progresso, com a exceção limitada de classificação descrita acima. Flashcards também têm fingerprint de assunto/frente/verso normalizados para ignorar conteúdo repetido mesmo com outro ID. IDs de questões e cartões ficam em tabelas distintas; prefira prefixos q e f.

Pacotes completos antigos, fontes oficiais, autoral_ia e dica opcional continuam compatíveis. Questões podem acrescentar assunto_id e subassunto. No formato completo, alternativas continua mapa A–E e resposta_correta é letra. No formato compacto, alternativas é uma lista com 2 a 5 textos e resposta_correta é índice **base zero** (1 significa a segunda opção). O texto geral explicacao é obrigatório. Se o compacto não trouxer análise individual, o app informa a ausência; não inventa justificativas para os distratores. Fonte omitida no compacto é registrada como conteúdo autoral de origem não informada, com revisão pendente. É recomendável fornecer fonte e comentários completos quando disponíveis.

Flashcard exige: id, disciplina, assunto_id, frente e verso. Opcionais: explicacao, assunto, subassunto, status_validacao, fonte e derivado_de. O status padrão é revisao_pendente; revisado é declaração do responsável pelo pacote. Não inclua progresso nem datas de revisão no JSON: são controlados pelo aplicativo. Para fonte oficial, os mesmos metadados/validação do formato anterior são aplicados. A qualidade e a verdade do conteúdo não são verificadas automaticamente.

Exemplos: examples/estudo-misto.json e examples/somente-flashcards.json. Não estão no APK e não são importados automaticamente.

## Sugestões locais e extensão futura

Depois de responder manualmente uma questão, **Sugerir flashcard desta questão** prepara uma extração local do enunciado completo, opções, gabarito e explicação. Não há inferência por IA. O usuário edita frente, verso e explicação e informa um assunto_id explícito se a questão ainda não tiver um. O ID de uma questão já classificada é mantido. Nada é salvo ao apenas abrir ou cancelar a sugestão.

**Salvar pendente** é o padrão. A opção de declarar revisado exige que o usuário marque explicitamente que conferiu o conteúdo. Não há classificação automática de uma resposta gerada como validada. Se a questão antiga não tinha assunto e a sugestão é salva, o vínculo é acrescentado com as mesmas proteções da reimportação. Conteúdo repetido não cria novas cópias nem reseta o progresso.

A interface FlashcardSuggestionProvider permite substituir a preparação por outro mecanismo futuramente. Não há geração durante o desbloqueio; o botão de preparação aparece apenas no estudo manual, após a resposta. A janela de desbloqueio somente seleciona conteúdo já armazenado.

## Migração e arquivos

Room passa de versão 1 para **2** via MIGRATION_1_2. Apenas são acrescentados topicId em questions/attempts, pendingKind em unlock_state, índices e as tabelas flashcards, flash_progress, flash_reviews, flash_presentations e study_mix. Não há DROP, limpeza de progresso ou fallback destrutivo. O tipo padrão question preserva notificações pendentes anteriores. Preferências antigas são mantidas; método e proporção novos recebem mixed/50 quando ausentes.

Principais arquivos: data/FlashModels.kt, PackCodec.kt, Migrations.kt, Repository.kt, Preferences.kt; domain/FlashLearning.kt; ui/FlashcardScreen.kt, StudyViewModel.kt e StudyApp.kt; integração em unlock/UnlockStudyService.kt e OverlayContent.kt. A aplicação registra a migração em StudyApplication.kt. As permissões e o applicationId não mudam.

Os resultados finais e relatórios desta entrega ficam em FLASHCARDS_VALIDATION.md e ../../downloads/review/1.4.0/.
