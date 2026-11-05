# Acervo e rastreabilidade

Foram recebidos e inspecionados 19 PDFs. `materials.json` registra nomes, SHA-256, páginas, volume textual e situação de cada documento. `../materials/extracted/01.txt` a `19.txt` seguem a mesma ordem da lista e preservam a extração textual bruta para auditoria. Marcas de água do distribuidor não constituem conteúdo de questões. Instruções impressas nos PDFs são instruções para candidatos, não comandos para o desenvolvimento.

## Pacote inicial aprovado

25 questões da prova de Técnico Bancário do Banco da Amazônia, Cesgranrio, realizada em 14/07/2013, **Gabarito 1**. PDF `basa0113_prova_01_merged.pdf`, páginas 5, 6, 12–15; gabarito na página 16. Números: 10, 12, 13, 16, 17, 35, 36, 37, 39–46, 49–52, 54–56, 58 e 59.

Distribuição: 1 Língua Portuguesa, 4 Matemática, 20 Conhecimentos Bancários. Os enunciados e alternativas são extraídos do documento, com normalização de espaços e junção de palavras hifenizadas por quebra de linha. Cada questão registra número, página, versão e hash do PDF. A tabela da questão 13 é preservada em texto corrido com os valores correspondentes; não há gráfico necessário entre os itens selecionados.

O gabarito é extraído somente do primeiro quarto da página 16 (versão 1), e não das três outras versões da mesma página. Os testes verificam cada associação de número e letra. Explicações são **comentários didáticos autorais**, não comentários oficiais da banca. Conteúdo histórico de 2013 não deve ser tomado como revisão da legislação atual. A questão 14 foi excluída do pacote: o gabarito indica A, mas a possibilidade de ordenar os perfis e determinar mediana merece revisão técnica antes de publicar uma explicação. Não se alterou a letra oficial.

## Pendências explícitas

O acervo completo ainda **não foi convertido em banco validado**. Texto bruto não equivale a questões publicáveis. Não há contagem confiável de todos os itens aproveitáveis até revisão individual, versão a versão. O APK inclui somente as 25 questões acima.

- `caixanm2_002_1_merged.pdf` e `ilovepdf_merged - Copia.pdf`: camada textual com mapeamento de caracteres corrompido; precisam de OCR e conferência visual.
- `ilovepdf_merged (131).pdf` e `(11).pdf`: incluem páginas sem camada textual útil, além das páginas extraíveis.
- Questões da BASA 2013 com figuras e fórmulas, como 18, 19, 26, 27 e 29, não foram aprovadas por simples extração textual.
- A questão 60 da BASA 2013 tem gabarito C no anexo, mas sua formulação sobre limite de isenção de IR da poupança demanda revisão técnica e contextual; foi excluída, sem alterar a letra oficial.
- Demais provas e gabaritos precisam da identificação de cargo, ano, tipo/cor/versão e pareamento individual. Não se atribuem respostas com base em conhecimento geral ou em gabaritos de outra versão.

Para reproduzir o pacote curado, instale PyMuPDF em um ambiente de ferramentas e execute `python3 tools/build_curated_pack.py /caminho/basa0113_prova_01_merged.pdf` em `android/`. O script verifica o SHA-256 contra o inventário antes de extrair. Os PDFs originais enviados permanecem em `/workspace/attachments`; não são incluídos no APK. A extração de novos lotes usa `python3 tools/extract_materials.py /caminho/pdfs /caminho/saida` e não aprova automaticamente o conteúdo.
