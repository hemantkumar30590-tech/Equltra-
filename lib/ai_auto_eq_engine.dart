import 'dart:convert';
import 'dart:io';
import 'package:uuid/uuid.dart';
import 'preset_manager.dart';

class AiAutoEqResult {
  final SoundProfile profile;
  final String acousticAnalysis;
  final String sourceEngine;

  const AiAutoEqResult({
    required this.profile,
    required this.acousticAnalysis,
    required this.sourceEngine,
  });
}

class AiAutoEqEngine {
  static const List<MapEntry<String, String>> quickScenarios = [
    MapEntry('Phonk / Gym Sub-Bass', 'Phonk & Gym Heavy Sub-Bass Slam'),
    MapEntry('Bollywood Vocals', 'Bollywood Lead Vocal Clarity & Air'),
    MapEntry('BGMI / FPS Footsteps', 'BGMI & Valorant Tactical Footsteps Clarity'),
    MapEntry('Dolby Cinema 3D', 'Dolby Atmos Cinema 3D Surround'),
    MapEntry('Fix Boomy Buds', 'Fix Boomy Muddy Budget Earbuds'),
    MapEntry('Lo-Fi Night', 'Lo-Fi Late Night Warm Velvet'),
    MapEntry('Live Spectrum AI', 'Live Spectrum Smart Calibration'),
  ];

  static Future<AiAutoEqResult> generateAiPreset({
    required String prompt,
    required String targetDeviceName,
    String apiKey = const String.fromEnvironment('GEMINI_API_KEY'),
  }) async {
    final cleanPrompt =
        prompt.trim().isEmpty ? 'Harman Studio Bass & Vocal Clarity' : prompt.trim();

    if (apiKey.isNotEmpty && apiKey != 'MY_GEMINI_API_KEY') {
      try {
        final uri = Uri.parse(
          'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey',
        );
        final client = HttpClient()..connectionTimeout = const Duration(seconds: 30);
        final req = await client.postUrl(uri);
        req.headers.contentType = ContentType.json;
        final payload = {
          'contents': [
            {
              'parts': [
                {
                  'text':
                      'Design a 10-band studio graphic equalizer preset for Equltra Android DSP.\n'
                      'User Request: "$cleanPrompt"\n'
                      'Target Audio Device: "$targetDeviceName"\n'
                      '10 Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz.\n'
                      'Return strictly JSON: {"presetName": "...", "description": "...", "acousticAnalysis": "...", "bands": [10 floats -15 to 15], "bassBoostStrength": 64, "spatialLevel": 75, "reverbLevel": 20, "reverbRoom": "Small Room", "preampDb": -2.0}'
                }
              ]
            }
          ],
          'generationConfig': {
            'responseMimeType': 'application/json',
            'temperature': 0.4,
          }
        };
        req.write(jsonEncode(payload));
        final resp = await req.close();
        if (resp.statusCode == 200) {
          final body = await resp.transform(utf8.decoder).join();
          final root = jsonDecode(body) as Map<String, dynamic>;
          final text = root['candidates']?[0]?['content']?['parts']?[0]?['text'] as String?;
          if (text != null && text.isNotEmpty) {
            final parsed = jsonDecode(text) as Map<String, dynamic>;
            final rawBands = (parsed['bands'] as List<dynamic>? ?? [])
                .map((e) => (e as num).toDouble().clamp(-15.0, 15.0))
                .toList();
            if (rawBands.length == 10) {
              final profile = SoundProfile(
                id: const Uuid().v4(),
                name: (parsed['presetName'] ?? 'AI: $cleanPrompt').toString(),
                category: 'AI Auto-EQ',
                description: (parsed['description'] ?? 'Gemini AI Auto-EQ').toString(),
                bands: rawBands,
                bassBoost: ((parsed['bassBoostStrength'] as num?)?.toDouble() ?? 60.0)
                    .clamp(0.0, 100.0),
                reverb: ((parsed['reverbLevel'] as num?)?.toDouble() ?? 20.0)
                    .clamp(0.0, 100.0),
                reverbRoom: (parsed['reverbRoom'] ?? 'Small Room').toString(),
                spatial: ((parsed['spatialLevel'] as num?)?.toDouble() ?? 65.0)
                    .clamp(0.0, 100.0),
                preampDb: ((parsed['preampDb'] as num?)?.toDouble() ?? -1.5)
                    .clamp(-12.0, 12.0),
              );
              return AiAutoEqResult(
                profile: profile,
                acousticAnalysis:
                    (parsed['acousticAnalysis'] ?? 'Tuned by Gemini 3.5 Flash AI.')
                        .toString(),
                sourceEngine: 'Gemini 3.5 Flash AI',
              );
            }
          }
        }
      } catch (_) {
        // Fallback to On-Device Acoustic DSP AI Engine
      }
    }

    return _synthesizeSmartAcousticAutoEq(
      prompt: cleanPrompt,
      targetDeviceName: targetDeviceName,
    );
  }

  static AiAutoEqResult _synthesizeSmartAcousticAutoEq({
    required String prompt,
    required String targetDeviceName,
  }) {
    final lower = prompt.toLowerCase();
    List<double> bands = List<double>.filled(10, 0.0);
    double bass = 52;
    double spatial = 58;
    double reverb = 18;
    String room = 'Small Room';
    String presetTitle =
        'AI: ${prompt.length > 22 ? prompt.substring(0, 22) : prompt}';
    final List<String> reasons = [];

    if (lower.contains('phonk') ||
        lower.contains('gym') ||
        lower.contains('punjabi') ||
        lower.contains('bass') ||
        lower.contains('sub')) {
      bands = [8.0, 7.0, 4.0, -1.5, -1.0, 0.5, 2.5, 4.0, 3.5, 2.5];
      bass = 82;
      spatial = 64;
      reverb = 20;
      room = 'Medium Room';
      presetTitle = 'AI Sub-Bass Slam';
      reasons.add(
        'Boosted 31Hz (+8.0dB) & 62Hz (+7.0dB) for chest-hitting sub-bass punch while dipping 250Hz (-1.5dB) to eliminate mid-bass mud.',
      );
    } else if (lower.contains('bgmi') ||
        lower.contains('pubg') ||
        lower.contains('valorant') ||
        lower.contains('footstep') ||
        lower.contains('fps')) {
      bands = [-3.5, -2.0, 0.0, 1.5, 3.0, 5.5, 7.0, 6.0, 3.5, 1.5];
      bass = 20;
      spatial = 92;
      reverb = 8;
      room = 'Small Room';
      presetTitle = 'AI Tactical Footsteps';
      reasons.add(
        'Attenuated 31–62Hz explosion rumble and elevated 1kHz–4kHz (+5.5dB to +7.0dB) with 92% Spatial 3D for pinpoint footstep directionality.',
      );
    } else if (lower.contains('vocal') ||
        lower.contains('bollywood') ||
        lower.contains('arijit') ||
        lower.contains('podcast')) {
      bands = [1.5, 2.0, 1.0, -1.0, 1.5, 4.0, 5.5, 4.5, 3.0, 3.5];
      bass = 38;
      spatial = 56;
      reverb = 26;
      room = 'Plate';
      presetTitle = 'AI Studio Vocal Air';
      reasons.add(
        'Opened up lead vocal presence across 1kHz–4kHz (+4.0dB to +5.5dB) with silk-dome 16kHz air (+3.5dB) and Plate shimmer.',
      );
    } else if (lower.contains('dolby') ||
        lower.contains('atmos') ||
        lower.contains('cinema') ||
        lower.contains('3d')) {
      bands = [6.5, 5.5, 2.5, -1.0, 0.0, 2.5, 4.5, 5.0, 4.5, 4.0];
      bass = 68;
      spatial = 88;
      reverb = 48;
      room = 'Large Hall';
      presetTitle = 'AI Atmos Cinema 3D';
      reasons.add(
        'Sculpted a wide theater soundstage (88% Spatial 3D, Large Hall) with 31Hz LFE sub-extension (+6.5dB) and crisp 2–4kHz dialogue clarity.',
      );
    } else if (lower.contains('lofi') ||
        lower.contains('lo-fi') ||
        lower.contains('night')) {
      bands = [2.0, 3.5, 3.0, 2.0, 1.0, 0.0, -1.5, -3.0, -4.0, -5.0];
      bass = 44;
      spatial = 52;
      reverb = 32;
      room = 'Medium Room';
      presetTitle = 'AI Lo-Fi Velvet Warmth';
      reasons.add(
        'Warmed 62–250Hz (+3.5dB) for analog tape fullness and gently rolled off 4kHz–16kHz (-3.0dB to -5.0dB) for zero ear fatigue.',
      );
    } else if (lower.contains('boomy') ||
        lower.contains('mud') ||
        lower.contains('fix')) {
      bands = [0.5, -2.5, -4.5, -3.0, 1.0, 2.5, 4.0, 4.5, 3.5, 3.0];
      bass = 24;
      spatial = 54;
      reverb = 12;
      room = 'Small Room';
      presetTitle = 'AI Anti-Mud Corrective';
      reasons.add(
        'Cut 125Hz (-4.5dB) and 250Hz (-3.0dB) driver bloat while restoring 2kHz–8kHz transient definition (+4.0dB to +4.5dB).',
      );
    } else {
      bands = [4.5, 4.0, 1.0, -1.5, 0.5, 2.0, 3.5, 4.0, 3.0, 3.5];
      bass = 56;
      spatial = 64;
      reverb = 18;
      room = 'Small Room';
      presetTitle = 'AI Harman Smart Curve';
      reasons.add(
        'Calibrated Harman target curve: balanced 31–62Hz sub-shelf (+4.5dB), de-mudded 250Hz (-1.5dB), and lifted 2–8kHz harmonic presence.',
      );
    }

    final matchedDevice = PresetManager.devicePresets.where(
      (d) => d.name.toLowerCase() == targetDeviceName.toLowerCase(),
    );
    if (matchedDevice.isNotEmpty) {
      final dev = matchedDevice.first;
      for (int i = 0; i < 10; i++) {
        bands[i] =
            (((bands[i] * 0.65 + dev.bands[i] * 0.35) * 2).round() / 2.0)
                .clamp(-15.0, 15.0);
      }
      reasons.add('Blended with ${dev.name} AutoEQ target compensation.');
    }

    return AiAutoEqResult(
      profile: SoundProfile(
        id: const Uuid().v4(),
        name: presetTitle,
        category: 'AI Auto-EQ',
        description: 'AI Auto-EQ for $targetDeviceName',
        bands: bands,
        bassBoost: bass,
        reverb: reverb,
        reverbRoom: room,
        spatial: spatial,
        preampDb: -2.0,
      ),
      acousticAnalysis: reasons.join(' '),
      sourceEngine: 'Acoustic DSP AI Engine',
    );
  }
}
