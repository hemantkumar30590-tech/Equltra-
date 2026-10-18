import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../constants.dart';

class GetApkScreen extends StatelessWidget {
  const GetApkScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF101522),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: const Color(0xFF00F0FF), width: 1.5),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.download, color: Color(0xFF00F0FF), size: 24),
                    SizedBox(width: 10),
                    Text(
                      'Direct APK Download & Export',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(color: Color(0xFFFF2A85)),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    onPressed: () {
                      Clipboard.setData(
                        const ClipboardData(
                          text: AppConstants.GITHUB_RELEASES_URL,
                        ),
                      );
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                            'Copied https://github.com/hemantkumar30590-tech/Equltra-/releases',
                          ),
                        ),
                      );
                    },
                    child: const Text(
                      'OPEN GITHUB RELEASES PAGE',
                      style: TextStyle(
                        color: Color(0xFFFF2A85),
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Option A — Direct from AI Studio Toolbar (Phone):',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF00F0FF),
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  "1. Tap the Settings (Gear) icon at the top-right of AI Studio.\n"
                  "2. Tap 'Download APK' / 'Generate APK' to download the signed APK directly to your phone.",
                  style: TextStyle(fontSize: 13, color: Colors.white),
                ),
                const SizedBox(height: 12),
                const Text(
                  'Option B — Direct from GitHub Releases:',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFFF2A85),
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  "1. Push this project to GitHub (hemantkumar30590-tech/Equltra-) using the GitHub icon in AI Studio.\n"
                  "2. Open https://github.com/hemantkumar30590-tech/Equltra-/releases -> Tap 'Equltra-v1.0.apk'.",
                  style: TextStyle(fontSize: 13, color: Colors.white),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF101522),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: const Color(0xFF1E273C)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.build, color: Color(0xFF00E676), size: 20),
                        SizedBox(width: 8),
                        Text(
                          '.github/workflows/build-apk.yml',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    IconButton(
                      icon: const Icon(Icons.copy, color: Color(0xFF00F0FF)),
                      onPressed: () {
                        Clipboard.setData(
                          const ClipboardData(
                            text: AppConstants.GITHUB_RELEASES_URL,
                          ),
                        );
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Copied Releases URL!'),
                          ),
                        );
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                const Text(
                  '• Repository: hemantkumar30590-tech/Equltra-\n'
                  '• Releases URL: https://github.com/hemantkumar30590-tech/Equltra-/releases\n'
                  '• Pre-configured with permissions: contents: write (uses automatic GITHUB_TOKEN).\n'
                  '• Uploads Equltra-v1.0.apk to GitHub Actions Artifacts and GitHub Releases automatically on every push to main.',
                  style: TextStyle(fontSize: 13, color: Colors.white70),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
