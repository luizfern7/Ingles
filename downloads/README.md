# Desbloqueia · Meu acervo 1.4.0 — flashcards

- [Baixar APK 1.4.0](desbloqueia-meu-acervo-1.4.0.apk)
- [JSON misto: questões e flashcards vinculados](exemplo-estudo-misto.json)
- [JSON somente flashcards](exemplo-somente-flashcards.json)
- [Como usar, formato e migração](../android/docs/FLASHCARDS.md)
- [Validação e testes](../android/docs/FLASHCARDS_VALIDATION.md)
- [Prompt para gerar flashcards com IA externa](prompt-gerar-flashcards.txt)
- [Prompt para questões de IA](prompt-gerar-questoes-ia.txt)
- [JSON autoral completo com dica](exemplo-autoral-ia.json)
- [Código-fonte ZIP](desbloqueia-android.zip)
- [Checksums](SHA256SUMS)

Instale por cima da edição Meu acervo, sem desinstalar, para preservar o progresso. Continua sem conteúdo incluído: importe seus JSONs em Ajustes. Em **Métodos de estudo**, o padrão é **Modo misto, 50% questões/50% flashcards**; altere o slider em passos de 10% ou use modos exclusivos. A proporção depende de conteúdo elegível: em percentuais intermediários, se não houver um tipo disponível, usa o outro; não antecipa revisões futuras apenas para cumprir quota.

Associe questões e flashcards pelo mesmo assunto_id estável. Erros priorizam cartões correspondentes na próxima vaga de flashcard, sem mudar a proporção. Vire antes de avaliar Sei/Não sei. Há revisão manual na tela inicial, agenda offline, estatísticas de cartões/assuntos e sugestões locais revisáveis após responder questões manualmente. Nenhuma API externa gera conteúdo no desbloqueio.

A migração Room 1→2 é aditiva e preserva dados antigos; questões sem assunto_id permanecem funcionais e sem vínculo. Mesma assinatura e pacote da edição pessoal. Dicas e formatos JSON anteriores continuam compatíveis.

84 execuções debug e 76 release aprovadas, lint sem problemas; migração e sobreposição testadas em APIs 28 e 35 simuladas. Ainda não houve teste físico de instalação/desbloqueio em aparelhos. ClamAV 1.5.4, base 28171 de 01/11/2026: zero detecções no APK; isso não garante aprovação pelo Defender. Mantenha a proteção se houver alerta.

A janela de desbloqueio exige permissões e serviço ativo, como antes. Após reboot/force-stop ou encerramento pelo fabricante, reabra o aplicativo. O APK usa assinatura de desenvolvimento, não chave de produção da Play Store.

Histórico: [1.3.0](desbloqueia-meu-acervo-1.3.0.apk), [1.2.0](desbloqueia-meu-acervo-1.2.0.apk), [1.1.0](desbloqueia-1.1.0.apk), [1.0.1](desbloqueia-1.0.1.apk).
