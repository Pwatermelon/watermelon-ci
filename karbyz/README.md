# Карбыз (Karbyz)

Манифест пайплайна Watermelon CI на татарском синтаксисе — тот же смысл, что YAML-форма.

```
karbyz/
  docs/           SPECIFICATION.md · KEYWORDS.md
  grammar/        karbyz.ebnf
  examples/       .kbz
  kabak/          лексер · парсер · исполнение · IR
  tests/
  run.py
```

Спека в консоли: `/app/docs` (текст из `docs/SPECIFICATION.md`).  
IDE: `/app/ide`. Storefront: `/app/studio`.

```bash
python3 karbyz/run.py test
python3 karbyz/run.py examples/all_constructs.kbz
python3 karbyz/run.py examples/devops_demo.kbz --as-yaml
```
