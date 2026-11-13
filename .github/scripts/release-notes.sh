#!/usr/bin/env bash
#
# release-notes.sh — writes dist/release-notes.md, the text of the GitHub Release:
#
#   * "what's new"  = the "## v<version>" section of CHANGELOG.md
#   * download and install help
#   * the facts verify-apk.sh found in the APK (dist/apk-info.env)
#
# Environment: GITHUB_REPOSITORY, GITHUB_SHA (GITHUB_SERVER_URL optional)
set -euo pipefail

OUT=dist
# shellcheck disable=SC1091
source "$OUT/apk-info.env"
: "${GITHUB_REPOSITORY:?}" "${GITHUB_SHA:?}"
: "${TAG:?}" "${VERSION_NAME:?}" "${VERSION_CODE:?}" "${APP_ID:?}" "${REL_NAME:?}" "${DBG_NAME:?}"
: "${SIZE:?}" "${DBG_SIZE:?}" "${SHA256:?}" "${UPDATE:?}"

REPO_URL="${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY}"
DL="${REPO_URL}/releases/download/${TAG}"

# The lines below "## v<version> ..." in CHANGELOG.md, up to the next "## " heading.
WHATS_NEW=""
if [ -f CHANGELOG.md ]; then
  WHATS_NEW=$(awk -v tag="$TAG" '/^## / { on = ($2 == tag); next } on' CHANGELOG.md | sed '/./,$!d')
fi

android_version() { # API level -> Android version
  case "$1" in
    21) echo 5.0 ;; 22) echo 5.1 ;; 23) echo 6.0 ;; 24) echo 7.0 ;; 25) echo 7.1 ;;
    26) echo 8.0 ;; 27) echo 8.1 ;; 28) echo 9 ;; 29) echo 10 ;; 30) echo 11 ;;
    31 | 32) echo 12 ;; 33) echo 13 ;; 34) echo 14 ;; 35) echo 15 ;; 36) echo 16 ;;
    *) echo "" ;;
  esac
}
ANDROID="minSdk ${MIN_SDK:-?} · targetSdk ${TARGET_SDK:-?}"
MIN_ANDROID=$(android_version "${MIN_SDK:-}")
[ -z "$MIN_ANDROID" ] || ANDROID="Android ${MIN_ANDROID} বা তার পরের ভার্সন (${ANDROID})"

{
  echo "## 📚 HSC গাইড — ${TAG}"
  echo
  echo "একাদশ-দ্বাদশ (HSC 26) শিক্ষার্থীদের জন্য সম্পূর্ণ গাইড, মূল বই, টেস্ট পেপার ও অফলাইন PDF রিডার।"
  echo

  if [ -n "$WHATS_NEW" ]; then
    echo "### 🆕 এই ভার্সনে নতুন"
    echo
    echo "$WHATS_NEW"
    echo
  fi

  cat <<EOF
### ⬇️ ডাউনলোড

| APK | ফাইল | সাইজ | ব্যবহার |
| --- | --- | --- | --- |
| **Release (সাইন করা)** | [\`${REL_NAME}\`](${DL}/${REL_NAME}) | ${SIZE} | সাধারণ ব্যবহারের জন্য — **এটাই ইনস্টল করুন** |
| **Debug** | [\`${DBG_NAME}\`](${DL}/${DBG_NAME}) | ${DBG_SIZE} | টেস্টিং / ডেভেলপারের জন্য |

### 📲 ইনস্টল করার নিয়ম

1. উপরের **Release** APK ফাইলটি ডাউনলোড করুন।
2. ফাইলটিতে ট্যাপ করুন — "Unknown sources / অজানা উৎস" অনুমতি চাইলে **Allow** দিন।
3. **Install** চাপুন, তারপর **Open**।

EOF

  case "$UPDATE" in
    ok)
      echo "> 🔄 **আপডেট:** ${PREV_TAG}-এর Release APK ইনস্টল করা থাকলে আনইনস্টল করার দরকার নেই — তার উপরেই এই APK ইনস্টল করুন। (Debug APK ইনস্টল করা থাকলে আগে সেটি আনইনস্টল করুন — Debug ও Release-এর সিগনেচার আলাদা।)"
      echo
      ;;
    key-changed | downgrade)
      echo "> ⚠️ **আপডেট:** এই ভার্সন ${PREV_TAG}-এর উপর সরাসরি ইনস্টল হবে না — আগে ${PREV_TAG} আনইনস্টল করুন। আনইনস্টল করলে অ্যাপের ডাটা মুছে যায়, তাই আগে ⚙ → \"ডাটা .json ডাউনলোড\" দিয়ে ব্যাকআপ নিয়ে রাখুন।"
      echo
      ;;
  esac

  cat <<'EOF'
### ✨ ফিচার

- ৭টি বিষয়: বাংলা, ইংরেজি, আইসিটি, পদার্থবিজ্ঞান, রসায়ন, জীববিজ্ঞান, উচ্চতর গণিত
- ১ম ও ২য় পত্র আলাদা ট্যাব, প্রতিটিতে **মূল বই** + **Test Paper** সেকশন
- ফোল্ডার সাপোর্ট: এক ফোল্ডারে অনেক PDF (অধ্যায়ভিত্তিক), কপি ও ডাউনলোড আইকন
- Google Drive লিংক অটো-কনভার্ট করে সরাসরি PDF ডাউনলোড
- অফলাইন লাইব্রেরি: ডাউনলোড করা সব PDF এক জায়গায়, সার্চসহ, প্রথম পৃষ্ঠার থাম্বনেইল
- বিল্ট-ইন PDF রিডার: ২ আঙুলে পিঞ্চ-জুম, ডাবল ট্যাপ, স্লাইডার ও পেজ নম্বর দিয়ে জাম্প
- অ্যাডমিন প্যানেল: ফোল্ডার/PDF যোগ, এডিট, ডিলিট, সব লিংক বদল, ডিফল্ট রিসেট
- JSON এক্সপোর্ট / ইমপোর্ট দিয়ে পুরো লাইব্রেরি ব্যাকআপ ও শেয়ার
- মেটেরিয়াল ৩ ডিজাইন, বাংলা টাইপোগ্রাফি (Hind Siliguri), Edge-to-Edge UI

EOF

  cat <<EOF
### 🔐 APK তথ্য

| | |
| --- | --- |
| Version | ${VERSION_NAME} (versionCode ${VERSION_CODE}) |
| Package | \`${APP_ID}\` |
| Android | ${ANDROID} |
| SHA-256 (\`${REL_NAME}\`) | \`${SHA256}\` |
EOF
  if [ -n "${CERT:-}" ]; then
    echo "| সাইনিং সার্টিফিকেট SHA-256 | \`${CERT}\` |"
  fi
  echo "| Build | GitHub Actions · commit [\`${GITHUB_SHA:0:7}\`](${REPO_URL}/commit/${GITHUB_SHA}) |"
  echo
  echo "---"
  if [ -n "${PREV_TAG:-}" ]; then
    echo "**Full Changelog**: ${REPO_URL}/compare/${PREV_TAG}...${TAG}"
  else
    echo "**Full Changelog**: ${REPO_URL}/commits/${TAG}"
  fi
} >"$OUT/release-notes.md"

echo "Wrote $OUT/release-notes.md ($(wc -l <"$OUT/release-notes.md") lines)"
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "<details><summary>📝 Release notes (${TAG})</summary>"
    echo
    cat "$OUT/release-notes.md"
    echo
    echo "</details>"
  } >>"$GITHUB_STEP_SUMMARY"
fi
