# Downloads — Desbloqueia 1.1.0

- [APK 1.1.0 — janela após desbloquear](desbloqueia-1.1.0.apk)
- [Código-fonte ZIP](desbloqueia-android.zip)
- [Como ativar e limites do Android](../android/docs/ANDROID.md)
- [Testes e validação 1.1.0](../android/docs/UNLOCK_1_1_0.md)
- [Checksums SHA-256](SHA256SUMS)

## Ativação no celular

Atualize sem desinstalar. Abra Desbloqueia → Ajustes → **Autorizar e ativar janela automática**. Autorize notificações e **Exibir sobre outros apps** no Android. Volte, escolha **A cada desbloqueio** e toque **Testar janela agora**. Depois feche a janela, vá à tela inicial do aparelho, bloqueie e desbloqueie.

As versões anteriores preservam o intervalo salvo; troque-o se quiser uma questão em todo desbloqueio. A notificação persistente mantém um botão **Parar**. A janela tem botão de fechar e não obriga a responder. Após reiniciar o aparelho ou encerrar o serviço, reabra o app para retomar. Android Go e fabricantes podem restringir a permissão/serviço. Testes automatizados cobriram APIs 28 e 35, mas a entrega ainda precisa de confirmação no aparelho físico.

O APK 1.1.0 não apresentou detecções no ClamAV 1.5.4, base diária 28171 de 01/11/2026. Isso não garante aprovação pelo Microsoft Defender. A versão inicial 1.0.0 havia recebido um alerta; mantenha o antivírus ativo se houver nova detecção. Relatórios em [review/1.1.0/](review/1.1.0/).

A assinatura de desenvolvimento anterior foi preservada para atualização sem desinstalação. Não é uma versão assinada para publicação na Play Store. O APK é para Android 8+, não um instalador Windows.

Versão anterior: [1.0.1, apenas notificações](desbloqueia-1.0.1.apk). [Revisão histórica 1.0.1](../android/docs/SECURITY_REVIEW.md).
