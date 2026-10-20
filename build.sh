#!/bin/sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT"
./gradlew assembleDebug
printf 'APK: %s\n' "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
