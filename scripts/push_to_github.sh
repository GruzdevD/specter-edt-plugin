#!/usr/bin/env bash
set -e

TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-$1}}"

TAG="${2:-v0.3.0}"

if [ -z "$TOKEN" ]; then
  echo "Ошибка: токен GitHub не найден ни в переменных окружения (GITHUB_TOKEN, GH_TOKEN), ни в аргументах скрипта."
  echo "Использование: ./scripts/push_to_github.sh <ВАШ_GITHUB_TOKEN> [ТЕГ]"
  exit 1
fi

echo "==> Отправка коммитов и тега ${TAG} в GitHub репозиторий GruzdevD/specter-edt-plugin..."
git push "https://GruzdevD:${TOKEN}@github.com/GruzdevD/specter-edt-plugin.git" master --tags

echo "==> Успешно отправлено! GitHub Actions запустит workflow сборки и публикации релиза ${TAG}."
