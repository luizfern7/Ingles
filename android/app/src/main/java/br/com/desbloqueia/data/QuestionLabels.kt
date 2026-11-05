package br.com.desbloqueia.data

fun Question.originSummary(): String=if(fonte.tipo=="autoral_ia")
    "Questão autoral gerada por IA\n${fonte.descricao}"
else "${fonte.concurso} · ${fonte.banca} · ${fonte.ano}\n${fonte.cargo} · Questão ${fonte.questao} · ${fonte.versao}"

fun Question.reviewNotice(): String=if(status_validacao=="revisao_pendente")
    "Revisão pendente · A resposta e as explicações fornecidas por IA podem conter erros."
else "Revisão declarada pelo responsável pelo pacote."

fun Question.originDetails(): String=if(fonte.tipo=="autoral_ia")
    "Origem autoral: ${fonte.descricao}\n${reviewNotice()}\nA correção segue o gabarito informado no JSON; não é gabarito oficial de uma banca."
else "Fonte: ${fonte.arquivo}, página ${fonte.pagina}; gabarito na página ${fonte.pagina_gabarito}. Gabarito conferido no documento anexado."
