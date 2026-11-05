# Desbloqueia · Meu acervo — 1.2.0

## Edição sem questões incluídas

Esta edição inicia com **zero questões**. Não há JSON de questões nos assets do APK, nem carga inicial de conteúdo no repositório. O usuário inclui todo o conteúdo usando Ajustes → Importar pacote de questões. O formato de importação é o mesmo das versões anteriores, com schema_version 1, questions, alternativas, resposta, explicações e metadados de origem. Continuam os limites de 5 MB/5.000 questões por pacote, a validação antes da transação e a preservação de IDs existentes/histórico.

Na tela inicial vazia, o botão Importar meu primeiro JSON leva ao controle de importação. Depois do primeiro pacote válido, a contagem, as disciplinas e o botão Estudar agora são atualizados automaticamente pelos Flows do Room. O modo automático não é iniciado pela Activity enquanto não existir conteúdo; os botões de ativação e teste orientam a importar primeiro. O repositório também retorna nenhuma sessão quando o banco está vazio, sem reservar apresentações vazias.

## Instalação independente e preservação

applicationId: **br.com.desbloqueia.personal**. Nome no aparelho: **Desbloqueia · Meu acervo**. Namespace de código: br.com.desbloqueia. VersionName 1.2.0, versionCode 4, Android 8+.

A identificação separada garante que um usuário das versões anteriores possa instalar esta edição e começar realmente com banco vazio, sem apagar dados de outra instalação. Não é uma atualização do pacote br.com.desbloqueia; os dois ícones podem coexistir e têm armazenamento, permissões e serviços separados. Se não quiser as janelas da edição anterior, desative o modo automático nela. Futuras atualizações do pacote personal preservam o conteúdo importado e o progresso; não há rotina que apague ou repovoe o banco ao iniciar.

A janela ao desbloquear, notificações, temas, preferências, histórico e estatísticas seguem os mesmos mecanismos da versão 1.1.0. Após importar, o usuário pode autorizar sobreposição/notificações e selecionar A cada desbloqueio. Reinicialização, force-stop e políticas de fabricantes continuam sujeitos às limitações descritas em ANDROID.md.

## Código de teste e conteúdo histórico

O pacote antigo foi movido para **src/test/resources/questions_fixture.json**, exclusivamente como fixture para regressões do importador, correção e seleção. Ele não está no APK, não é carregado pelo app e não aparece no acervo da edição pessoal. Os testes agora precisam importá-lo explicitamente antes dos cenários com conteúdo. As extrações antigas em materials/ e relatórios de versões anteriores são documentação histórica fora do pacote instalado.

O script opcional build_curated_pack.py agora exige um caminho de saída explícito e não grava um pacote nos assets do aplicativo. Não é executado na compilação nem na inicialização.

## Verificações desta entrega

- Compilação release concluída com sucesso; minSdk 26 e targetSdk 35.
- **38 execuções de teste debug aprovadas**, sem falhas, erros ou ignorados.
- **36 execuções de teste release aprovadas**, incluindo banco vazio, importação, Activity inicial e sobreposição. São execuções adicionais de cenários existentes; não representam 36 casos distintos novos.
- **Lint release sem problemas**.
- Novos testes em APIs 28 e 35 verificam primeiro início e reinício sem seed, ausência de sessão/apresentação com banco vazio, importação explícita, preservação do progresso, importação repetida e pacote inválido sem alterar o banco. A Activity real em Robolectric começa vazia, abre o controle de importação e permite estudar depois da importação de uma fixture.
- Auditoria do APK confirmou `br.com.desbloqueia.personal`, release sem depuração, assinatura válida e nenhum pacote de questões ou fixture. Os assets são somente os perfis ART. Há um JSON técnico do Kotlin/Gradle fora dos assets; ele não contém questões.
- **ClamAV 1.5.4: 1 arquivo examinado, 0 detecções**, com base diária 28171 de 01/11/2026. Foram examinados 48,95 MiB de conteúdo expandido a partir de 7,06 MiB de APK. O scanner rodou sem rede, sem submissão a terceiros, com o APK somente para leitura. Não se afirma atualização da base até o dia da execução nem aprovação pelo Microsoft Defender.

Relatórios em `../../downloads/review/1.2.0/`. SHA-256 do APK final:

```text
078e2c35d91535fef3d3c795d17f9d010f6baf8959ef82044c564cea329c94a7
```

Os testes de tela e desbloqueio são simulados em Robolectric; não houve teste físico de instalação ou desbloqueio em um celular.
