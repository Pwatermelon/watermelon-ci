# Карбыз (Karbyz)

Манифест пайплайна Watermelon CI на татарском синтаксисе — тот же смысл, что YAML-форма, другая запись.

```
karbyz/
  docs/           # спецификация и словарь
  grammar/        # нормативная РБНФ
  examples/       # референсные манифесты / сценарии (.kbz)
  kabak/          # разбор → модель · исполнение · YAML-эквивалент для компилятора
  tests/
  run.py
```

В продукте: `/docs/karbyz`, в демо — **Манифесты → Карбыз** (Compile → Jenkinsfile, как у YAML).

```bash
python3 karbyz/run.py test
python3 karbyz/run.py examples/all_constructs.kbz
python3 karbyz/run.py examples/devops_demo.kbz --as-yaml   # тот же манифест в YAML-записи
```
