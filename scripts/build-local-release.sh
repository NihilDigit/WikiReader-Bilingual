#!/usr/bin/env bash
set -euo pipefail

version_name="${1:-}"

if [[ ! -f release-signing.properties ]]; then
  echo "Missing release-signing.properties. Copy release-signing.properties.example and fill it with the release keystore values." >&2
  exit 1
fi

args=(testDebugUnitTest assembleRelease)
if [[ -n "$version_name" ]]; then
  args+=("-PwikiReaderBilingualVersionName=$version_name")
fi

./gradlew "${args[@]}"
