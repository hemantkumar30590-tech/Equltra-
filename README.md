# Equltra - 10-Band System-wide EQ with YouTube Hook

![Equltra v1.0 PRO](https://img.shields.io/badge/Equltra-v1.0%20PRO-00F0FF?style=for-the-badge)
![Android 9--16](https://img.shields.io/badge/Android-9%2B%20DynamicsProcessing-FF2A85?style=for-the-badge)

**Equltra** is an AMOLED black Material 3 10-band system-wide audio equalizer and DSP suite with real-time YouTube & media session hooking:
- **10-Band Graphic / Parametric Equalizer** (`31Hz`, `62Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz`, `-15dB` to `+15dB`)
- **Studio DSP Modules**: Sub-Harmonic **Bass Boost**, Binaural **Spatial 3D**, Schroeder **Acoustic Reverb** (`Small Room`, `Medium Room`, `Large Hall`, `Plate`), and **Preamp + 64-Bit Soft-Clip Limiter**
- **15 Device-Specific EQ Presets (Oct 2026 Latest)**: AutoEQ & RTINGS tuned curves for Apple AirPods Pro 3, AirPods 4 ANC, Sony WF-1000XM5/XM6, WH-1000XM6, Samsung Galaxy Buds 3 Pro / 4 Pro, Bose QuietComfort Ultra 2nd Gen, Sennheiser Momentum 4, Google Pixel Buds Pro 2, JBL Tour Pro 3, OnePlus Buds Pro 3, Nothing Ear (a), Boat/Noise/Realme Buds, and Over-ear Headphones.
- **9 Built-in Mastering Presets**: `Flat`, `Bass Boost`, `Warm`, `Theater` (Bass 64%, Spatial 82%, Reverb 55%), `Dialogue Boost`, `Action Mode`, `Soft`, `Late Night`, `Vocal Clarity`
- **Custom Preset Import / Export**: Supports `.json`, AutoEQ `.txt`, and `.equltra` files + WhatsApp / Telegram sharing.
- **YouTube & Global System Audio Hook**: Uses Android 9+ `DynamicsProcessing`, `LoudnessEnhancer`, `AudioPlaybackCallback`, and sticky background service (`Equltra Background DSP ACTIVE`).

---

## Download APK
After push, go to Actions tab -> Latest workflow -> Download APK from Artifacts OR go to Releases -> Download Equltra-v1.0.apk

---

## How to get APK in 2 clicks from phone

### Method 1: From GitHub Releases (Phone Browser)
1. **Click 1:** Open your GitHub repository (`https://github.com/hemantkumar30590-tech/Equltra/releases`) on your phone and tap the latest **Equltra v1.0** release.
2. **Click 2:** Tap **`Equltra-v1.0.apk`** under **Assets** to download and install directly on your Android phone (pre-signed with debug key so no keystore setup is required).

### Method 2: Directly inside Google AI Studio (Phone Browser)
1. **Click 1:** Tap the **Settings (Gear icon)** in the top-right toolbar of AI Studio.
2. **Click 2:** Tap **Download APK / Export APK** to download the compiled APK directly to your phone.

---

## GitHub Actions Workflow & Repository Configuration

The automated build pipeline is located at `.github/workflows/build-apk.yml`.

### Required Secrets & Permissions
- **No external secrets are required!** GitHub automatically provides `secrets.GITHUB_TOKEN` during workflow runs.
- The workflow already declares `permissions: contents: write` at the top of `.github/workflows/build-apk.yml` so it can automatically create GitHub Releases and upload `Equltra-v1.0.apk`.
