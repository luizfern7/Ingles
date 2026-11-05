# Revisão pontual — versão 1.0.1

## Motivo e conclusão limitada

O usuário relatou que o Microsoft Defender bloqueou o APK de debug 1.0.0 como `Trojan:Script/Ulthar.A!ml`. A revisão do código e do pacote não encontrou execução de comandos externos no código próprio, permissão de internet, scripts embutidos ou executáveis Windows. Isso não substitui análise antivírus nem comprova falso positivo.

Foi possível executar **ClamAV 1.5.4 localmente**, sem enviar os arquivos a terceiros. Os dois arquivos — original e revisado — tiveram **1 arquivo examinado e 0 detecções**, sem erro de execução. A base diária é a **28171, de 1º de novembro de 2026**, incluída na imagem oficial. Não se afirma que essa fosse a base mais recente no momento da análise. As bases main, daily e bytecode tiveram a verificação de assinatura do `sigtool --info` aprovada. A análise final do revisado leu 7,05 MiB e examinou 48,94 MiB de conteúdo expandido.

**Não foi possível repetir a análise no Microsoft Defender neste ambiente Linux. A detecção do Defender permanece não esclarecida.** Um resultado negativo no ClamAV não a refuta. Se o Defender continuar bloqueando a versão 1.0.1, mantenha o bloqueio e encaminhe o arquivo para revisão da Microsoft; não desative a proteção para instalar.

## Alterações de escopo reduzido

1. **Distribuição sem componentes de desenvolvimento:** APK gerado pela variante `release`, com `debuggable=false`. As dependências `debugImplementation` de inspeção Compose e de suporte de testes não participam do APK distribuído. O Manifest final contém somente a Activity `br.com.desbloqueia.MainActivity`; as telas e funcionalidades permanecem as mesmas. Não foi adicionada ofuscação.
2. **Privacidade de backup:** regras explícitas de exclusão de dados de backup para Android anterior ao 12 e para backup/transferência no Android 12+, mantendo `allowBackup=false`. O aviso de lint da primeira versão foi resolvido.
3. **Consistência do histórico:** respostas agora recebem timestamps estritamente crescentes dentro de uma transação, mesmo quando o relógio retrocede ou duas tentativas recebem o mesmo milissegundo. Isso impede ambiguidade na contagem de itens a revisar. Um teste de regressão cobre erro seguido de acerto nesses casos. Não houve alteração de schema do Room nem exclusão de dados.
4. **Atualização identificável:** versionName 1.0.1 e versionCode 2. Foi mantido o certificado de desenvolvimento anterior para permitir atualização do APK sem desinstalar e apagar o histórico. Isso não equivale a uma assinatura de produção da Play Store.

Essas mudanças têm objetivos de distribuição, privacidade e correção. Não foram feitas alterações cosméticas ou técnicas para ocultar conteúdo de antivírus. Não se atribui a nenhuma delas a resolução da detecção.

## Verificações do APK final

- Compilação `assembleRelease` concluída com sucesso.
- `testDebugUnitTest`: **18 testes aprovados**, sem falhas, erros ou ignorados.
- `testReleaseUnitTest`, selecionando `StudyTest` e `AppStartupTest`: **16 execuções aprovadas**, cobrindo o banco e a Activity real também na variante release. São execuções adicionais de testes existentes, não 16 cenários novos.
- `lintRelease`: **zero avisos e zero erros**.
- Assinatura APK validada pelo `apksigner`; certificado SHA-256 idêntico ao da versão 1.0.0.
- Permissões finais: `POST_NOTIFICATIONS`, `VIBRATE` e a permissão interna de receiver do AndroidX. **Sem INTERNET**, acessibilidade, instalação de outros APKs ou sobreposição.
- Componentes Jetpack mesclados: serviço interno não exportado `androidx.room.MultiInstanceInvalidationService`, provider interno do AndroidX Startup e receiver de perfis AndroidX protegido pela permissão `android.permission.DUMP`. Nenhum serviço permanente de monitoramento foi acrescentado.
- Bibliotecas nativas: somente `libandroidx.graphics.path.so` e `libdatastore_shared_counter.so`, em quatro ABIs, provenientes das dependências AndroidX. Os assets são o pacote de questões e os perfis ART `baseline.prof`/`baseline.profm` gerados pela compilação release.
- Pacote de 25 questões preservado, com 125 justificativas de alternativas.

A auditoria estrutural é reproduzível com `tools/audit_apk.py`. Esse script é uma checagem de integridade/estrutura, **não** um scanner de malware.

## Identificação dos arquivos e evidência

Original 1.0.0:

```text
0043741a2a65103791ec8f34bd05762973b8e7fb9f50ebd8c7774e06f623c774
```

Revisado 1.0.1:

```text
a6af05c497daeb51bb4fcd343c91320396e597438a9bfefffb87467d7808e05f
```

Os relatórios ficam em `../../downloads/review/`; `../../downloads/SHA256SUMS` permite conferir os downloads atuais. O APK original sinalizado foi retirado da pasta de downloads da branch atual; o histórico Git anterior permanece preservado.

Imagem oficial de análise: `clamav/clamav@sha256:6d87c712e3391e13c1ba6e05dfb074d4b1ddb5a6834b23bb9da990d2a4ef042a`. Os containers rodaram sem rede, sem capabilities, sem privilégios novos, com o arquivo somente para leitura. Foram usados `--scan-archive=yes --allmatch --max-filesize=100M --max-scansize=200M --alert-exceeds-max=yes`. Os relatórios incluem horários UTC da execução. Não houve submissão do APK a serviços de análise externos.

Não foram executados testes em aparelho físico. Continuam válidas as limitações do receptor dinâmico de desbloqueio e da curadoria parcial descritas em `ANDROID.md` e `MATERIALS.md`.

## Comandos de validação utilizados

Em `android/`, com JDK completo, SDK e proxy/truststore do ambiente configurados:

```sh
./gradlew assembleRelease testDebugUnitTest lintRelease \
  testReleaseUnitTest \
  --tests br.com.desbloqueia.StudyTest \
  --tests br.com.desbloqueia.AppStartupTest
python3 tools/audit_apk.py app/build/outputs/apk/release/app-release.apk \
  --sdk /caminho/do/android-sdk \
  --previous /caminho/do/apk-original.apk \
  --output /caminho/dos/relatorios
```

O seletor `--tests` se aplica à tarefa `testReleaseUnitTest`; a suíte debug completa também foi executada. Neste ambiente, `JAVA_HOME=/workspace/toolchains/jdk-21.0.5+11`, `GRADLE_USER_HOME=/workspace/toolchains/gradle-home` e `-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts` foram utilizados. Esses caminhos locais não são requisitos para compilar em outro computador.
