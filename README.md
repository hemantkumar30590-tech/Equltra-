# VoidEQ Equltra — AMOLED 10-Band Audio Equalizer & Studio DSP

**VoidEQ Equltra** is a hardware-grade AMOLED black Material 3 audio equalizer and DSP suite featuring:
- **10-Band Graphic / Parametric Equalizer** (`31Hz`, `62Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz`, `-15dB` to `+15dB`)
- **Studio DSP Modules**: Sub-Harmonic **Bass Boost**, Binaural **Spatial 3D**, Schroeder **Acoustic Reverb** (`Small Room`, `Medium Room`, `Large Hall`, `Plate`), and **Preamp + 64-Bit Soft-Clip Limiter**
- **9 Built-in Mastering Presets**:
  1. `Flat`
  2. `Bass Boost`
  3. `Warm`
  4. `Theater`
  5. `Dialogue Boost`
  6. `Action Mode`
  7. `Soft`
  8. `Late Night`
  9. `Vocal Clarity`
- **Built-in Live Synth Test Loop** + **System Audio Session (Session 0) Hardware AudioFX Binding**

---

## Download APK
After push, go to Actions tab -> Latest workflow -> Download APK from Artifacts OR go to Releases -> Download Equltra-v1.0.apk

---

## How to get APK in 2 clicks from phone

### Method 1: From GitHub Releases (Phone Browser)
1. **Click 1:** Open your GitHub repository (`https://github.com/hemantkumar30590-tech/Equltra/releases`) on your phone and tap the latest ** VoidEQ Equltra v1.0** release.
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
- If your repository's default workflow permissions are restricted, verify on your phone:
  1. Go to **Repo Settings** -> **Actions** -> **General**.
  2. Scroll to **Workflow permissions** and ensure **Read and write permissions** is selected, then tap **Save**.
