#!/usr/bin/env bash
# Builds signed phone and Wear OS release bundles locally with the signing secrets read
# from 1Password, and with --upload sends them through gplay to the Play internal tracks
# (phone to internal, Wear OS to wear:internal).
# Usage: SPOTON_SIGNING_ITEM=op://<vault>/<item> tools/signing/build-release.sh [--upload]
set -euo pipefail

: "${SPOTON_SIGNING_ITEM:?set SPOTON_SIGNING_ITEM to the 1Password item reference}"
command -v op >/dev/null 2>&1 || { echo "error: the 1Password CLI (op) is not installed." >&2; exit 1; }

UPLOAD=false
case "${1:-}" in
  "") ;;
  --upload) UPLOAD=true; command -v gplay >/dev/null 2>&1 || { echo "error: gplay is not installed." >&2; exit 1; } ;;
  *) echo "Usage: tools/signing/build-release.sh [--upload]" >&2; exit 1 ;;
esac

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROPS="$REPO_ROOT/local.properties"
PACKAGE=com.prof18.spoton
MODULES=(app spotonwear)

umask 077
WORK_DIR="$(mktemp -d)"
HAD_PROPS=false

cleanup() {
  if [[ "$HAD_PROPS" == true && -f "$WORK_DIR/backup" ]]; then
    mv -f "$WORK_DIR/backup" "$PROPS"
  else
    rm -f "$PROPS"
  fi
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

if [[ -f "$PROPS" ]]; then
  cp -p "$PROPS" "$WORK_DIR/backup"
  HAD_PROPS=true
fi

read_field() {
  local value
  value="$(op read "$SPOTON_SIGNING_ITEM/$1")"
  printf '%s' "${value//\\/\\\\}"
}

op read --force --out-file "$WORK_DIR/release.jks" "$SPOTON_SIGNING_ITEM/release.jks" >/dev/null
KEY_ALIAS="$(read_field key_alias)"
STORE_PASSWORD="$(read_field store_password)"
KEY_PASSWORD="$(read_field key_password)"

# local.properties also holds sdk.dir: keep every other line and replace only the signing keys.
if [[ "$HAD_PROPS" == true ]]; then
  grep -v -E '^(storeFile|keyAlias|storePassword|keyPassword)=' "$WORK_DIR/backup" > "$WORK_DIR/local.properties" || true
else
  : > "$WORK_DIR/local.properties"
fi
cat >> "$WORK_DIR/local.properties" <<EOF
storeFile=$WORK_DIR/release.jks
keyAlias=$KEY_ALIAS
storePassword=$STORE_PASSWORD
keyPassword=$KEY_PASSWORD
EOF
mv -f "$WORK_DIR/local.properties" "$PROPS"

(umask 022 && cd "$REPO_ROOT" && ./gradlew -q --console=plain :app:bundleRelease :spotonwear:bundleRelease)

for module in "${MODULES[@]}"; do
  aab="$REPO_ROOT/$module/build/outputs/bundle/release/$module-release.aab"
  output="$(jarsigner -verify "$aab" 2>&1 || true)"
  if [[ "$output" != *"jar verified"* ]]; then
    echo "error: $aab is not signed: $output" >&2
    exit 1
  fi
  echo "Signed bundle: $aab"
done

if [[ "$UPLOAD" == true ]]; then
  for module in "${MODULES[@]}"; do
    aab="$REPO_ROOT/$module/build/outputs/bundle/release/$module-release.aab"
    mapping="$REPO_ROOT/$module/build/outputs/mapping/release/mapping.txt"
    version_name="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' "$REPO_ROOT/$module/build.gradle.kts" | head -1)"
    track=internal
    [[ "$module" == spotonwear ]] && track=wear:internal
    mapping_args=()
    [[ -f "$mapping" ]] && mapping_args=(--mapping "$mapping")
    gplay releases upload \
      --package "$PACKAGE" \
      --track "$track" \
      --release-name "$version_name" \
      ${mapping_args[@]+"${mapping_args[@]}"} \
      "$aab"
  done
fi
