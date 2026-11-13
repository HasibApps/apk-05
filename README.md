<div align="center">

<img src="docs/banner.png" alt="HSC Guide — একাদশ-দ্বাদশ শ্রেণির সম্পূর্ণ গাইড" width="100%">

# 📚 HSC গাইড · v3.0.0

**একাদশ-দ্বাদশ (HSC 26) শিক্ষার্থীদের জন্য সম্পূর্ণ গাইড — মূল বই, টেস্ট পেপার ও অফলাইন PDF রিডার**

[![Build & Release APK](https://github.com/HasibApps/apk-05/actions/workflows/build.yml/badge.svg)](https://github.com/HasibApps/apk-05/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/HasibApps/apk-05?label=release&color=00C875)](https://github.com/HasibApps/apk-05/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/HasibApps/apk-05/total?color=00B8D9)](https://github.com/HasibApps/apk-05/releases)
[![APK Size](https://img.shields.io/badge/APK-6.8%20MB-00C875)](https://github.com/HasibApps/apk-05/releases/latest)
[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

### [⬇️ APK v3.0.0 ডাউনলোড করুন](https://github.com/HasibApps/apk-05/releases/download/v3.0.0/HscGuide-v3.0.0-release.apk)

<sub>Release (সাইন করা) · Android 7.0+ · GitHub Actions-এ বিল্ড ও সাইন করা</sub>

</div>

---

## 📲 ডাউনলোড

| ধরন | ফাইল | সাইজ | বিবরণ |
| :-- | :-- | :-- | :-- |
| 🟢 **Release (সাইন করা)** | **[HscGuide-v3.0.0-release.apk](https://github.com/HasibApps/apk-05/releases/download/v3.0.0/HscGuide-v3.0.0-release.apk)** | 6.84 MB | সাধারণ ব্যবহারের জন্য — **এটাই ইনস্টল করুন** |
| 🟡 **Debug** | [HscGuide-v3.0.0-debug.apk](https://github.com/HasibApps/apk-05/releases/download/v3.0.0/HscGuide-v3.0.0-debug.apk) | 8.41 MB | টেস্টিং / ডেভেলপারদের জন্য |
| 🔁 **সবসময় সর্বশেষ** | [HscGuide-release.apk](https://github.com/HasibApps/apk-05/releases/latest/download/HscGuide-release.apk) | — | সর্বশেষ রিলিজের সাইন করা APK |
| 📄 **সব ভার্সন** | [Releases পেজ](https://github.com/HasibApps/apk-05/releases) | — | পুরোনো ও নতুন সব ভার্সন |

> নির্দিষ্ট কোনো ভার্সন: `https://github.com/HasibApps/apk-05/releases/download/v<ভার্সন>/HscGuide-v<ভার্সন>-release.apk`

## 🔐 APK তথ্য (v3.0.0)

| | |
| :-- | :-- |
| **অ্যাপের নাম** | HSC গাইড (HSC Guide) |
| **Version** | `3.0.0` · versionCode **4** |
| **Package / Application ID** | `com.hasib.guide` |
| **Release ফাইল** | `HscGuide-v3.0.0-release.apk` · 6.84 MB |
| **SHA-256** | `6bbba0a80251c219c7ec5d907abdab9cba9c5c7080b5a6f86142f4892a48ccc1` |
| **সাইনিং** | ✅ apksigner-এ যাচাই করা — v2 + v3 স্কিম |
| **সাইনিং সার্টিফিকেট** | `CN=HSC Guide, O=HasibApps, C=BD`<br>SHA-256: `4ca662acc8a1af9808848f8e685180cbe7ec37cf0e760c113f25ab6fd13a65a2` |
| **কীস্টোর** | `signing/hscguide-release.p12` (PKCS#12, alias `hscguide`) — CI-তে একই কী দিয়ে সাইন হয় |
| **Min SDK** | 24 — Android 7.0 Nougat |
| **Target / Compile SDK** | 36 / 36.1 — Android 16 |
| **অনুমতি** | `INTERNET`, `ACCESS_NETWORK_STATE` |
| **বিল্ড** | GitHub Actions · commit [`53691d5`](https://github.com/HasibApps/apk-05/commit/53691d598d4aa15f96074e772f421f5387e49458) |

<details>
<summary><b>✅ ইনস্টল করে যাচাই করবেন কীভাবে</b></summary>

```bash
# ডাউনলোড করা ফাইলের SHA-256 মিলিয়ে দেখুন
sha256sum HscGuide-v3.0.0-release.apk
# 6bbba0a80251c219c7ec5d907abdab9cba9c5c7080b5a6f86142f4892a48ccc1

# সিগনেচার ও সার্টিফিকেট যাচাই (Android SDK build-tools দরকার)
apksigner verify --verbose --print-certs HscGuide-v3.0.0-release.apk
```

</details>

## 📥 ইনস্টল করার নিয়ম

1. উপরের **Release APK** লিংক থেকে ফাইলটি ডাউনলোড করুন।
2. ফাইলটিতে ট্যাপ করুন — "Install unknown apps / অজানা উৎস" অনুমতি চাইলে আপনার ব্রাউজারকে **Allow** দিন।
   *Settings → Apps → Special access → Install unknown apps*
3. **Install** চাপুন, তারপর **Open**।
4. প্রথমবার খুললেই অ্যাপ নিজে থেকেই ৭টি বিষয়ের ডিফল্ট ডাটা তৈরি করে নেবে — অ্যাডমিন প্যানেলে গিয়ে লিংক বদলাতে পারবেন।

> 🔄 **আপডেট (v1.x / v2.x → v3.0.0):** আগের Release APK ইনস্টল থাকলে **আনইনস্টল করতে হবে না** — একই প্যাকেজ (`com.hasib.guide`) ও একই সাইনিং কী, তাই সরাসরি ইনস্টল হবে। তবু ভেতরের ডাটা নতুন ফরম্যাটে সাজানো, তাই গুরুত্বপূর্ণ কাস্টম লিংক থাকলে ⚙ → **ডাটা (.json) ডাউনলোড** দিয়ে ব্যাকআপ রেখে দিন।
>
> ⚠️ Debug APK ইনস্টল করা থাকলে আগে সেটি আনইনস্টল করে নিন — Debug ও Release-এর সিগনেচার আলাদা।

## 🆕 v3.0.0-এ নতুন

- 🎨 **সম্পূর্ণ নতুন মেটেরিয়াল ৩ ডিজাইন** — সাদা কার্ড, গ্রেডিয়েন্ট টাইল, প্রতিটি বিষয়ের নিজস্ব আইকন ও রঙ।
- 🧭 **ট্যাব নেভিগেশন** — নিচে **বিষয়সমূহ** / **অফলাইন PDF**, উপরে গ্রেডিয়েন্ট হেডার।
- 📑 **পত্রভিত্তিক সেকশন** — ১ম ও ২য় পত্র আলাদা ট্যাব, প্রতিটিতে **মূল বই** ও **Test Paper**; ফোল্ডার এক্সপ্যান্ড করে অধ্যায়ভিত্তিক PDF।
- 🖼️ **নতুন অফলাইন লাইব্রেরি** — থাম্বনেইল, তারিখ ও ফাইল সাইজসহ এক তালিকা, সাথে সার্চ।
- 📖 **নতুন PDF রিডার** — পিঞ্চ-জুম, ডাবল ট্যাপ, স্লাইডার ও পেজ নম্বর দিয়ে জাম্প।
- 🛠️ **নতুন অ্যাডমিন প্যানেল** — চিপ/ট্যাব ভিত্তিক এডিটর, ফোল্ডার ও PDF অ্যাড/এডিট/ডিলিট, ডিফল্ট রিসেট।
- 🔗 **লিংক কপি/শেয়ার** — প্রতিটি PDF-এর পাশে কপি আইকন।
- ⬆️ **নতুন বিল্ড স্ট্যাক** — AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10, compileSdk 36.1, minSdk 24।

সব ভার্সনের পরিবর্তন: [CHANGELOG.md](CHANGELOG.md)

---

## 🖼️ স্ক্রিনশট

<div align="center">

| হোম — ৭টি বিষয় | বিষয় পেজ — মূল বই ও Test Paper | হেডার ও পত্র ট্যাব |
| :--: | :--: | :--: |
| <img src="docs/screenshots/01-home-subjects.png" width="240"> | <img src="docs/screenshots/02-paper-sections.png" width="240"> | <img src="docs/screenshots/03-subject-header.jpg" width="240"> |

</div>

---

## ✨ ফিচার

| ফিচার | বিবরণ |
| :-- | :-- |
| 📚 **৭টি বিষয়** | বাংলা, ইংরেজি, আইসিটি, পদার্থবিজ্ঞান, রসায়ন, জীববিজ্ঞান, উচ্চতর গণিত |
| 📗 **১ম ও ২য় পত্র** | প্রতিটি বিষয়ে আলাদা পত্র ট্যাব, পত্র বদলালে কনটেন্ট বদলায় |
| 🗂️ **মূল বই + Test Paper** | দুইটি আলাদা সেকশন, ফোল্ডারের ভেতরে অধ্যায়ভিত্তিক PDF (নেস্টেড) |
| ☁️ **Google Drive সাপোর্ট** | Drive শেয়ার লিংক অটো-কনভার্ট করে সরাসরি ডাউনলোড |
| 📴 **সম্পূর্ণ অফলাইন** | একবার ডাউনলোড হলে ইন্টারনেট ছাড়াই পড়া যায় |
| 🖼️ **অটো থাম্বনেইল** | PDF-এর প্রথম পৃষ্ঠা থেকে থাম্বনেইল তৈরি হয় |
| 🔍 **বিল্ট-ইন PDF রিডার** | পিঞ্চ-জুম (২ আঙুল), ডাবল ট্যাপ, স্লাইডার, নির্দিষ্ট পেজে জাম্প |
| 🔗 **লিংক কপি** | যেকোনো PDF-এর Drive লিংক এক ট্যাপে কপি করে শেয়ার |
| 🔐 **অ্যাডমিন প্যানেল** | পাসওয়ার্ড দিয়ে আনলক — ফোল্ডার/PDF যোগ, এডিট, ডিলিট, ডিফল্ট রিসেট |
| 💾 **JSON ব্যাকআপ** | পুরো কনটেন্ট `.json` হিসেবে এক্সপোর্ট ও ইমপোর্ট |
| 🎨 **মেটেরিয়াল ৩ UI** | Edge-to-Edge, Hind Siliguri বাংলা ফন্ট, বিষয়ভিত্তিক গ্রেডিয়েন্ট |

## 🧩 অ্যাপ সম্পর্কে

**HSC গাইড** হলো HSC (উচ্চ মাধ্যমিক) শিক্ষার্থীদের জন্য একটি হালকা ও দ্রুত অফলাইন PDF লাইব্রেরি।
বিষয়ভিত্তিক মূল বই ও টেস্ট পেপার Google Drive লিংক থেকে ডাউনলোড করে ফোনে সংরক্ষণ করা যায়,
তারপর অ্যাপের ভেতরের রিডার দিয়েই পড়া যায় — কোনো তৃতীয় পক্ষের PDF অ্যাপ বা ইন্টারনেট লাগে না।

v3.0.0-তে অ্যাপটি নতুন করে ডিজাইন করা হয়েছে: বিষয় কার্ড, পত্র ট্যাব, ফোল্ডারভিত্তিক সেকশন,
আলাদা অফলাইন লাইব্রেরি ও সম্পূর্ণ নতুন অ্যাডমিন এডিটর। অ্যাডমিন মোডে নিজের লাইব্রেরি নিজেই
গুছিয়ে নিতে পারবেন, আর `.json` ফাইলের মাধ্যমে এক ফোনের কনটেন্ট অন্য ফোনে বা বন্ধুদের সাথে শেয়ার করা যায়।

## 🛠️ টেক স্ট্যাক

| | |
| :-- | :-- |
| **Language** | Kotlin 2.2.10 (AGP-এর বিল্ট-ইন Kotlin সাপোর্ট) |
| **Build System** | Gradle 9.3.1 · Android Gradle Plugin 9.1.1 · JDK 17 |
| **UI** | Material 3 (`Theme.Material3.Light.NoActionBar`), ViewBinding, RecyclerView, ConstraintLayout |
| **PDF** | Android `PdfRenderer` + কাস্টম জুমেবল RecyclerView (কোনো ভারী লাইব্রেরি নেই) |
| **Networking** | OkHttp 4.10 |
| **Storage** | Internal app storage + Moshi/JSON (`guide_database.json`, `downloaded_pdfs.json`) |
| **CI/CD** | GitHub Actions → signed APK → যাচাই → automatic GitHub Release |

## 🗂️ প্রজেক্ট স্ট্রাকচার

```
apk-05/
├── .github/
│   ├── workflows/build.yml         # CI: build + sign + verify + release
│   └── scripts/
│       ├── verify-apk.sh           # APK-র সিগনেচার, ভার্সন ও আপডেট যাচাই
│       └── release-notes.sh        # CHANGELOG.md থেকে রিলিজ নোট তৈরি
├── app/
│   ├── build.gradle.kts            # versioning (3.0.0 / 4) + release signing
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/
│       │   ├── MainActivity.kt          # হোম: বিষয় গ্রিড + অফলাইন লাইব্রেরি
│       │   ├── SubjectActivity.kt       # পত্র ট্যাব, মূল বই / Test Paper
│       │   ├── PdfViewerActivity.kt     # জুমেবল PDF রিডার
│       │   ├── AdminPanelActivity.kt    # অ্যাডমিন এডিটর
│       │   ├── SettingsController.kt    # সেটিংস + JSON ইমপোর্ট/এক্সপোর্ট
│       │   ├── data/                    # মডেল, রিপোজিটরি, ডিফল্ট ডাটা
│       │   └── pdf/                     # ZoomablePdfRecyclerView
│       └── res/                    # layout, drawable, font, theme
├── docs/
│   ├── banner.png
│   └── screenshots/                # README-র স্ক্রিনশট
├── signing/hscguide-release.p12    # ডেমো রিলিজ কীস্টোর (public)
├── CHANGELOG.md
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

## ⚙️ GitHub Actions দিয়ে বিল্ড

প্রতিটি `main` (বা `arena/**`) ব্রাঞ্চে push হলে — অথবা ম্যানুয়ালি **Run workflow** — অটোমেটিকভাবে:

1. JDK 17 + Gradle 9.3.1 + Android SDK (API 36.1) সেটআপ
2. `assembleDebug` + `assembleRelease` (রিলিজ কী দিয়ে সাইন করা)
3. **APK যাচাই** — `apksigner` দিয়ে সিগনেচার, APK-র ভেতরের ভার্সন, এবং আগের রিলিজের উপর সরাসরি আপডেট হবে কি না
   (সিগনেচার ঠিক না থাকলে বিল্ড ফেইল করে, রিলিজ হয় না)
4. APK গুলো **Artifacts** হিসেবে আপলোড (৯০ দিন সংরক্ষিত)
5. APK-র `versionName` থেকে ট্যাগ (যেমন `v3.0.0`) নিয়ে **GitHub Release** তৈরি ও APK আপলোড —
   রিলিজ নোটের "নতুন কী" অংশ আসে [`CHANGELOG.md`](CHANGELOG.md) থেকে

### 🚀 নতুন ভার্সন ছাড়ার নিয়ম

1. [`app/build.gradle.kts`](app/build.gradle.kts)-এ `versionCode` এক বাড়ান এবং `versionName` বদলান (যেমন `5` ও `"3.1.0"`)।
2. [`CHANGELOG.md`](CHANGELOG.md)-তে `## v3.1.0` নামে একটি অংশ যোগ করে কী বদলেছে লিখুন।
3. `main` ব্রাঞ্চে push / merge করুন — বিল্ড, যাচাই ও Release অটোমেটিক হয়ে যাবে।

**ম্যানুয়ালি চালাতে:** Actions → *Build & Release APK* → **Run workflow** (`publish_release` চালু রাখুন)।
ট্যাগ তৈরি হয় ঠিক যে commit বিল্ড হয়েছে তার উপরে, তাই ব্রাঞ্চ থেকেও রিলিজ বের করা যায়।

### লোকালি বিল্ড

```bash
git clone https://github.com/HasibApps/apk-05.git
cd apk-05
./gradlew assembleRelease
# আউটপুট: app/build/outputs/apk/release/app-release.apk
```

> **সাইনিং:** `signing/hscguide-release.p12` হলো একটি পাবলিক ডেমো কীস্টোর (টাইপ PKCS#12, alias `hscguide`, পাসওয়ার্ড `hscguide2026`)।
> নিজের কী ব্যবহার করতে চাইলে `signing.properties` ফাইলে অথবা
> `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`
> এনভায়রনমেন্ট ভ্যারিয়েবল দিয়ে পথ ও পাসওয়ার্ড বদলান। কী বদলালে পুরোনো ইনস্টল করা অ্যাপের
> উপর আপডেট আর ইনস্টল হবে না — ব্যবহারকারীকে আগে আনইনস্টল করতে হবে।

## 🔐 অ্যাডমিন

অ্যাপের ⚙ আইকন → **অ্যাডমিন প্যানেল** → পাসওয়ার্ড দিন। পাসওয়ার্ড কোডে প্লেইন টেক্সটে নেই,
SHA-256 হ্যাশ হিসেবেই যাচাই হয় (`AdminPanelActivity` / `SettingsController`)।

## 🤝 কন্ট্রিবিউট

Issue খুলুন বা Pull Request পাঠান — যেকোনো সাজেশন স্বাগতম।

## 📄 লাইসেন্স

এই প্রজেক্টটি শিক্ষামূলক উদ্দেশ্যে তৈরি। অ্যাপে যুক্ত PDF গুলোর স্বত্ব তাদের নিজ নিজ প্রকাশকের।
ব্যক্তিগত পড়াশোনার বাইরে বাণিজ্যিক ব্যবহার নিষিদ্ধ।

---

<div align="center">

**© 2026 HSC 26 · The App Create By Hasib Bro · 01831410991**

⭐ ভালো লাগলে রিপোতে একটি **Star** দিন!

</div>
