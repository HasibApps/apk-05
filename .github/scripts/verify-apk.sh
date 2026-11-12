#!/usr/bin/env bash
#
# verify-apk.sh — runs right after `./gradlew assembleDebug assembleRelease`.
#
#   1. Reads the real package / versionName / versionCode out of the built
#      release APK, so the release tag can never disagree with the APK.
#   2. FAILS the build if the release APK is unsigned or its signature does not
#      verify — Android refuses to install an APK like that.
#   3. Compares it with the previous published release: the same signing key
#      and a higher versionCode mean it installs over the old version without
#      an uninstall, so users keep their data. A problem here only warns.
#   4. Copies the APKs to dist/ under their release names and records what it
#      found in dist/apk-info.env (read by release-notes.sh), the job summary
#      and the step outputs.
#
# Environment:
#   ANDROID_HOME                 Android SDK (preinstalled on GitHub runners)
#   GH_TOKEN, GITHUB_REPOSITORY  optional — used to look up the previous release
set -euo pipefail

OUT=dist
GRADLE_FILE=app/build.gradle.kts

die() {
  echo "::error title=APK verification failed::$*"
  exit 1
}
warn() { echo "::warning title=$1::$2"; }

# ── 1. find the APKs Gradle built ─────────────────────────────────────────────
first_apk() { find "$1" -maxdepth 1 -type f -name '*.apk' 2>/dev/null | sort | head -n 1 || true; }
REL=$(first_apk app/build/outputs/apk/release)
DBG=$(first_apk app/build/outputs/apk/debug)
[ -n "$REL" ] || die "no release APK in app/build/outputs/apk/release"
[ -n "$DBG" ] || die "no debug APK in app/build/outputs/apk/debug"
case "$REL" in
  *-unsigned.apk)
    die "the release APK is unsigned ($(basename "$REL")): the signing keystore was not found, see $GRADLE_FILE"
    ;;
esac

# ── 2. signature ──────────────────────────────────────────────────────────────
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
sdk_tool() { # path of the newest build-tools copy of $1 that actually runs here
  local dir
  if [ -n "$SDK" ]; then
    while IFS= read -r dir; do
      if "$dir/$1" version >/dev/null 2>&1; then
        echo "$dir/$1"
        return 0
      fi
    done < <(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -rV)
  fi
  return 0
}
APKSIGNER=$(sdk_tool apksigner)
AAPT2=$(sdk_tool aapt2)

# These helpers read tool output on stdin.
signer_cert() { awk -F': ' '/ certificate SHA-256 digest: / { print tolower($NF); exit }'; }
pkg_attr() { sed -n "/^package:/{s/.*[[:space:]]$1='\([^']*\)'.*/\1/p;q;}"; }
sdk_attr() { sed -n -E "/^($1):/{s/^[^']*'([^']*)'.*/\1/p;q;}"; }

CERT=""
SCHEMES=""
SIGNATURE="unverified"
if [ -n "$APKSIGNER" ]; then
  echo "::group::apksigner verify $(basename "$REL")"
  if SIGNER=$("$APKSIGNER" verify --verbose --print-certs "$REL" 2>&1); then
    echo "$SIGNER"
    echo "::endgroup::"
  else
    echo "$SIGNER"
    echo "::endgroup::"
    die "the release APK is not correctly signed, Android would refuse to install it"
  fi
  SIGNATURE="verified"
  CERT=$(signer_cert <<<"$SIGNER")
  [[ "$CERT" =~ ^[0-9a-f]{64}$ ]] || CERT=""
  SCHEMES=$(awk '/^Verified using v[0-9.]+ scheme .*: true$/ { printf "%s%s", sep, $3; sep = " + " }' <<<"$SIGNER")
else
  warn "APK signature not checked" "apksigner was not found in the Android SDK (ANDROID_HOME='$SDK'), so the signature of the release APK could not be verified."
fi

# ── 3. what the APK says about itself ─────────────────────────────────────────
APP_ID=""
VERSION_NAME=""
VERSION_CODE=""
MIN_SDK=""
TARGET_SDK=""
if [ -n "$AAPT2" ]; then
  BADGING=$("$AAPT2" dump badging "$REL" 2>/dev/null || true)
  echo "::group::aapt2 dump badging $(basename "$REL")"
  sed -n '1,12p' <<<"$BADGING"
  echo "::endgroup::"
  APP_ID=$(pkg_attr name <<<"$BADGING")
  VERSION_NAME=$(pkg_attr versionName <<<"$BADGING")
  VERSION_CODE=$(pkg_attr versionCode <<<"$BADGING")
  MIN_SDK=$(sdk_attr 'sdkVersion|minSdkVersion' <<<"$BADGING")
  TARGET_SDK=$(sdk_attr targetSdkVersion <<<"$BADGING")
fi
if [ -z "$APP_ID" ] || [ -z "$VERSION_NAME" ] || [ -z "$VERSION_CODE" ]; then
  # Fallback: what the Gradle file declares (`versionName = "2.0.0"`).
  gradle_value() { grep -oP "\b$1\s*=\s*\"?\K[^\"\s]+" "$GRADLE_FILE" 2>/dev/null | head -n 1 || true; }
  warn "APK manifest not read" "aapt2 could not read the manifest of the release APK; using the version declared in $GRADLE_FILE."
  APP_ID=${APP_ID:-$(gradle_value applicationId)}
  VERSION_NAME=${VERSION_NAME:-$(gradle_value versionName)}
  VERSION_CODE=${VERSION_CODE:-$(gradle_value versionCode)}
fi
[[ "$VERSION_NAME" =~ ^[0-9][0-9A-Za-z._-]*$ ]] ||
  die "versionName '$VERSION_NAME' cannot be used as a release tag (expected something like 2.0.0)"
[[ "$VERSION_CODE" =~ ^[0-9]+$ ]] || die "could not read versionCode (got '$VERSION_CODE')"
[ -n "$APP_ID" ] || die "could not read the applicationId"

# ── 4. release file names ─────────────────────────────────────────────────────
TAG="v$VERSION_NAME"
REL_NAME="HscGuide-$TAG-release.apk"
DBG_NAME="HscGuide-$TAG-debug.apk"
rm -rf "$OUT"
mkdir -p "$OUT"
cp "$REL" "$OUT/$REL_NAME"
cp "$DBG" "$OUT/$DBG_NAME"
# Version-free copies: releases/latest/download/HscGuide-release.apk always works.
cp "$REL" "$OUT/HscGuide-release.apk"
cp "$DBG" "$OUT/HscGuide-debug.apk"
ls -l "$OUT"

sha() { sha256sum "$1" | cut -d' ' -f1; }
mb() { awk -v b="$(stat -c %s "$1")" 'BEGIN { printf "%.2f MB", b / 1048576 }'; }
SHA256=$(sha "$OUT/$REL_NAME")
SIZE=$(mb "$OUT/$REL_NAME")
DBG_SIZE=$(mb "$OUT/$DBG_NAME")

# ── 5. can users update from the previous release? ────────────────────────────
#   ok           same key, higher versionCode -> installs over the old version
#   same-code    same key, versionCode not raised
#   downgrade    versionCode went down         -> Android refuses the update
#   key-changed  different signing key         -> users must uninstall first
#   unknown      could not compare             none: there is no earlier release
PREV_TAG=""
PREV_CODE=""
UPDATE="none"
if [ -n "${GH_TOKEN:-}" ] && [ -n "${GITHUB_REPOSITORY:-}" ]; then
  RELEASED=$(gh api "repos/$GITHUB_REPOSITORY/releases?per_page=100" \
    --jq '.[] | select((.draft or .prerelease) | not) | .tag_name' 2>/dev/null || true)
  # The published version right below this one, e.g. v1.0.0 for v2.0.0.
  PREV_TAG=$(printf '%s\n' "$RELEASED" "$TAG" | grep -E '^v[0-9]' | sort -uV |
    awk -v t="$TAG" '$0 == t { print prev; exit } { prev = $0 }' || true)
fi
if [ -n "$PREV_TAG" ]; then
  UPDATE="unknown"
  PREV_DIR=$(mktemp -d)
  PREV_APK="$PREV_DIR/HscGuide-$PREV_TAG-release.apk"
  if [ -n "$APKSIGNER" ] && [ -n "$AAPT2" ] && [ -n "$CERT" ] &&
    gh release download "$PREV_TAG" --repo "$GITHUB_REPOSITORY" --dir "$PREV_DIR" \
      --pattern "HscGuide-$PREV_TAG-release.apk" >/dev/null 2>&1 && [ -f "$PREV_APK" ]; then
    PREV_CERT=$("$APKSIGNER" verify --print-certs "$PREV_APK" 2>/dev/null | signer_cert || true)
    PREV_CODE=$("$AAPT2" dump badging "$PREV_APK" 2>/dev/null | pkg_attr versionCode || true)
    echo "Previous release $PREV_TAG: versionCode=${PREV_CODE:-?} signing certificate=${PREV_CERT:-?}"
    if [[ "$PREV_CERT" =~ ^[0-9a-f]{64}$ && "$PREV_CODE" =~ ^[0-9]+$ ]]; then
      if [ "$PREV_CERT" != "$CERT" ]; then
        UPDATE="key-changed"
      elif [ "$VERSION_CODE" -gt "$PREV_CODE" ]; then
        UPDATE="ok"
      elif [ "$VERSION_CODE" -eq "$PREV_CODE" ]; then
        UPDATE="same-code"
      else
        UPDATE="downgrade"
      fi
    else
      PREV_CODE=""
    fi
  fi
  rm -rf "$PREV_DIR"
fi

case "$UPDATE" in
  ok)
    UPDATE_TEXT="✅ ${PREV_TAG}-এর উপর সরাসরি ইনস্টল হবে, আনইনস্টল লাগবে না (একই সাইনিং কী, versionCode ${PREV_CODE} → ${VERSION_CODE})"
    ;;
  same-code)
    UPDATE_TEXT="⚠️ versionCode বাড়েনি (${PREV_TAG}-এও ${PREV_CODE})"
    warn "versionCode not raised" "$TAG has the same versionCode ($VERSION_CODE) as $PREV_TAG. Raise versionCode in $GRADLE_FILE for every new version."
    ;;
  downgrade)
    UPDATE_TEXT="❌ versionCode কমে গেছে (${PREV_CODE} → ${VERSION_CODE}): ${PREV_TAG} থেকে আপডেট হবে না"
    warn "versionCode went down" "$TAG has versionCode $VERSION_CODE but $PREV_TAG has $PREV_CODE, so Android refuses to install it over $PREV_TAG."
    ;;
  key-changed)
    UPDATE_TEXT="❌ সাইনিং কী বদলে গেছে: ${PREV_TAG} আনইনস্টল না করলে ইনস্টল হবে না"
    warn "Signing key changed" "$TAG is signed with a different key than $PREV_TAG: users have to uninstall $PREV_TAG first and lose their data."
    ;;
  unknown)
    UPDATE_TEXT="ℹ️ ${PREV_TAG}-এর সাথে তুলনা করা যায়নি"
    ;;
  *)
    UPDATE_TEXT="— (আগের কোনো রিলিজ নেই)"
    ;;
esac
if [ "$SIGNATURE" = verified ]; then
  SIGNATURE_TEXT="✅ apksigner যাচাই করেছে (${SCHEMES:-?})"
else
  SIGNATURE_TEXT="⚠️ যাচাই করা যায়নি (apksigner পাওয়া যায়নি)"
fi

# ── 6. record the facts ───────────────────────────────────────────────────────
for v in TAG VERSION_NAME VERSION_CODE APP_ID MIN_SDK TARGET_SDK REL_NAME DBG_NAME \
  SIZE DBG_SIZE SHA256 SIGNATURE CERT SCHEMES PREV_TAG PREV_CODE UPDATE; do
  printf '%s=%q\n' "$v" "${!v}"
done >"$OUT/apk-info.env"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "tag=$TAG"
    echo "version_name=$VERSION_NAME"
    echo "version_code=$VERSION_CODE"
    echo "sha256=$SHA256"
    echo "update=$UPDATE"
  } >>"$GITHUB_OUTPUT"
fi

cat >>"${GITHUB_STEP_SUMMARY:-/dev/stdout}" <<EOF
### 📦 HSC Guide ${TAG}

| | |
| --- | --- |
| Version | **${VERSION_NAME}** (versionCode ${VERSION_CODE}) |
| Package | \`${APP_ID}\` |
| Android | minSdk ${MIN_SDK:-?} · targetSdk ${TARGET_SDK:-?} |
| Release APK | \`${REL_NAME}\` · ${SIZE} |
| Debug APK | \`${DBG_NAME}\` · ${DBG_SIZE} |
| SHA-256 (release) | \`${SHA256}\` |
| সাইনিং | ${SIGNATURE_TEXT} |
| সাইনিং সার্টিফিকেট SHA-256 | \`${CERT:-?}\` |
| আপডেট | ${UPDATE_TEXT} |

EOF

echo "::notice title=HSC Guide ${TAG} (versionCode ${VERSION_CODE})::${REL_NAME} · ${SIZE} · sha256 ${SHA256} · signature ${SIGNATURE} (${SCHEMES:-?}) · certificate ${CERT:-?} · update from ${PREV_TAG:-none}: ${UPDATE}"
