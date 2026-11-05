# JSON autoral por IA e dicas — versão 1.3.0

O app continua vazio ao instalar e não usa rede ou uma API de IA. As questões, explicações e dicas são importadas de JSON fornecido pelo usuário.

## Obrigatórios

Raiz: `schema_version: 1` e `questions`, array de 1 a 5.000 questões (até 5 MB por arquivo UTF-8).

Cada questão exige `id`, `disciplina`, `assunto`, `enunciado`, `alternativas`, `resposta_correta`, `explicacao`, `explicacoes_alternativas`, `fonte` e `status_validacao`. As strings devem conter texto. O ID é único, de até 160 caracteres. Alternativas são um mapa com 2 a 5 letras A–E, textos não vazios e uma única resposta correta. O mapa de explicações deve ter exatamente as mesmas letras. IDs existentes no banco não são substituídos e não alteram o histórico.

Para autoria de IA, a fonte é somente:

```json
{
  "tipo": "autoral_ia",
  "descricao": "Questão autoral gerada com auxílio de IA"
}
```

Não são exigidos PDF, hash, banca, concurso, cargo, ano, páginas, número de prova ou versão. Não preencha esses campos com informações inventadas. O status é `revisao_pendente` ou `revisado`; o segundo é uma declaração do responsável pelo pacote, não uma verificação feita pelo app. A geração por IA deve usar `revisao_pendente`. O aplicativo avisa que resposta e explicações pendentes podem conter erros, antes e depois da resposta.

## Opcionais

- `autoria_explicacao`: identificação textual de autoria. Se omitida, usa "Explicação fornecida pelo autor do pacote.". Não envie string vazia nesse campo.
- **`dica`**: texto de ajuda, visível somente quando o usuário toca na lâmpada antes de responder. Se ausente, `null`, `""` ou composto apenas por espaços, **não há ícone nem botão**. Não use a string "sem dica" como substituto: ela é texto e seria exibida. A dica pode orientar um método ou conceito, sem indicar a letra correta nem entregar o resultado. A explicação completa fica em `explicacao`, exibida após responder. Abrir ou ocultar a dica não grava resposta nem altera as estatísticas.

A mesma regra vale na tela manual e na janela de desbloqueio, pois compartilham o componente de questão. O estado da dica é específico de cada sessão. O conteúdo é local e não é gerado na hora.

## Pacotes oficiais anteriores

Continuam aceitos. A ausência de `fonte.tipo` significa `oficial`, preservando o formato anterior. Também se pode escrever `tipo: "oficial"`. Nesse caso, permanecem os metadados reais de prova/PDF, as páginas, o SHA-256 de 64 caracteres hexadecimais e o status `gabarito_conferido`. Não misture esse status com `autoral_ia`. O tipo de fonte e o status são validados por questão: um mesmo pacote pode conter questões oficiais e autorais, cada uma corretamente identificada.

O app não verifica a verdade das respostas nem autentica as fontes. Validação estrutural não equivale a revisão pedagógica. Tipos de fonte desconhecidos, status incompatíveis ou campos extras não reconhecidos são recusados. Uma importação inválida não altera parcialmente o banco.

## Arquivos para o usuário

- `examples/autoral-ia.json`: exemplo completo importável com uma questão de porcentagem, marcado como autoral por IA e revisão pendente.
- `PROMPT_GERAR_QUESTOES_IA.txt`: prompt para copiar à IA que produzirá os pacotes, com campos obrigatórios/opcionais e orientações sobre dica.

Esses arquivos ficam fora do APK. Não são importados automaticamente. O usuário pode omitir `dica` inteiramente em qualquer questão, sem precisar modificar o restante do pacote.
