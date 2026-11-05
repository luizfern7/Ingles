#!/usr/bin/env python3
"""Inventaria PDFs fornecidos e extrai texto bruto (não valida questões/gabaritos)."""
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
a.output.mkdir(parents=True,exist_ok=True)
rows=[]
for i,pdf in enumerate(sorted(a.source.rglob('*.pdf')),1):
    target=a.output/f'{i:02}.txt'
    subprocess.run(['pdftotext','-layout',str(pdf),str(target)],check=True)
    text=target.read_text()
    rows.append(dict(file=pdf.name,sha256=hashlib.sha256(pdf.read_bytes()).hexdigest(),pages=text.count('\f'),characters=len(text),text=target.name,status='pendente_revisao'))
(a.output/'inventory.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
print(f'{len(rows)} PDFs extraídos. Necessária conferência visual e associação à versão correta do gabarito.')
