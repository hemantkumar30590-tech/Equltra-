import 'dart:convert';
import 'dart:math' as math;
import 'package:hive_flutter/hive_flutter.dart';

class SoundProfile {
  static const int bandCount = 31;
  final String id;
  final String name;
  final String category; // 'Studio', 'Device', 'Imported', 'Custom'
  final String description;
  final List<double> _rawBands; // 31-band (or 10-band interpolated) gains in dB (-15.0 to +15.0)
  final double bassBoost; // 0 to 100
  final double reverb; // 0 to 100
  final String reverbRoom;
  final double spatial; // 0 to 100
  final double preampDb; // -12.0 to +12.0
  final bool isBuiltIn;
  final int createdAtMs;

  const SoundProfile({
    required this.id,
    required this.name,
    this.category = 'Custom',
    this.description = 'Custom user sound profile',
    required List<double> bands,
    this.bassBoost = 0.0,
    this.reverb = 0.0,
    this.reverbRoom = 'Small Room',
    this.spatial = 0.0,
    this.preampDb = 0.0,
    this.isBuiltIn = false,
    this.createdAtMs = 0,
  }) : _rawBands = bands;

  List<double> get bands => interpolateTo31(_rawBands);

  static List<double> interpolateTo31(List<double> src) {
    if (src.isEmpty) return List<double>.filled(bandCount, 0.0);
    if (src.length == bandCount) {
      return src.map((v) => v.clamp(-15.0, 15.0)).toList();
    }
    return List<double>.generate(bandCount, (i) {
      final pos = (i / (bandCount - 1)) * (src.length - 1);
      final lo = pos.floor().clamp(0, src.length - 1);
      final hi = pos.ceil().clamp(0, src.length - 1);
      final t = pos - lo;
      final val = src[lo] * (1.0 - t) + src[hi] * t;
      return double.parse(val.clamp(-15.0, 15.0).toStringAsFixed(1));
    });
  }

  double get bassSummaryDb {
    final b = bands;
    final avg = b.sublist(0, 9).reduce((a, c) => a + c) / 9.0;
    return double.parse(avg.toStringAsFixed(1));
  }

  double get midSummaryDb {
    final b = bands;
    final avg = b.sublist(9, 21).reduce((a, c) => a + c) / 12.0;
    return double.parse(avg.toStringAsFixed(1));
  }

  double get trebleSummaryDb {
    final b = bands;
    final avg = b.sublist(21, 31).reduce((a, c) => a + c) / 10.0;
    return double.parse(avg.toStringAsFixed(1));
  }

  String toExportJson() {
    final map = {
      'name': name,
      'description': description,
      'bands': bands.map((b) => double.parse(b.toStringAsFixed(1))).toList(),
      'bass': bassBoost.round(),
      'spatial': spatial.round(),
      'reverb': reverb.round(),
      'preamp': double.parse(preampDb.toStringAsFixed(1)),
    };
    return const JsonEncoder.withIndent('  ').convert(map);
  }

  SoundProfile copyWith({
    String? id,
    String? name,
    String? category,
    String? description,
    List<double>? bands,
    double? bassBoost,
    double? reverb,
    String? reverbRoom,
    double? spatial,
    double? preampDb,
    bool? isBuiltIn,
    int? createdAtMs,
  }) {
    return SoundProfile(
      id: id ?? this.id,
      name: name ?? this.name,
      category: category ?? this.category,
      description: description ?? this.description,
      bands: bands ?? List<double>.from(this.bands),
      bassBoost: bassBoost ?? this.bassBoost,
      reverb: reverb ?? this.reverb,
      reverbRoom: reverbRoom ?? this.reverbRoom,
      spatial: spatial ?? this.spatial,
      preampDb: preampDb ?? this.preampDb,
      isBuiltIn: isBuiltIn ?? this.isBuiltIn,
      createdAtMs: createdAtMs ?? this.createdAtMs,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'category': category,
      'description': description,
      'bands': bands,
      'bassBoost': bassBoost,
      'reverb': reverb,
      'reverbRoom': reverbRoom,
      'spatial': spatial,
      'preampDb': preampDb,
      'isBuiltIn': isBuiltIn,
      'createdAtMs': createdAtMs,
    };
  }

  factory SoundProfile.fromMap(Map<dynamic, dynamic> map) {
    final rawBands = map['bands'] as List<dynamic>?;
    final parsedBands = rawBands != null && rawBands.length == 10
        ? rawBands.map((e) => (e as num).toDouble().clamp(-15.0, 15.0)).toList()
        : List<double>.filled(10, 0.0);

    return SoundProfile(
      id: (map['id'] as String?) ??
          'custom_${DateTime.now().millisecondsSinceEpoch}',
      name: (map['name'] as String?) ?? 'Custom Profile',
      category: (map['category'] as String?) ?? 'Custom',
      description:
          (map['description'] as String?) ?? 'Custom user sound profile',
      bands: parsedBands,
      bassBoost:
          ((map['bassBoost'] ?? map['bass'] ?? 0.0) as num).toDouble().clamp(0.0, 100.0),
      reverb: ((map['reverb'] as num?) ?? 0.0).toDouble().clamp(0.0, 100.0),
      reverbRoom: (map['reverbRoom'] as String?) ?? 'Small Room',
      spatial:
          ((map['spatial'] as num?) ?? 0.0).toDouble().clamp(0.0, 100.0),
      preampDb:
          ((map['preampDb'] ?? map['preamp'] ?? 0.0) as num).toDouble().clamp(-12.0, 12.0),
      isBuiltIn: (map['isBuiltIn'] as bool?) ?? false,
      createdAtMs: ((map['createdAtMs'] as num?) ?? 0).toInt(),
    );
  }
}

/// Manages built-in studio presets, 15 Oct 2026 Device-Specific EQ presets,
/// and user-created / imported custom sound profiles using Hive.
class PresetManager {
  static const String presetsBoxName = 'equltra_custom_presets';
  static const String importedBoxName = 'equltra_imported_presets';
  static const String activeStateBoxName = 'equltra_settings';

  static const List<double> targetFrequenciesHz = [
    31.0,
    62.0,
    125.0,
    250.0,
    500.0,
    1000.0,
    2000.0,
    4000.0,
    8000.0,
    16000.0,
  ];

  late final Box _presetsBox;
  late final Box _importedBox;
  late final Box _stateBox;

  PresetManager._(this._presetsBox, this._importedBox, this._stateBox);

  static Future<PresetManager> init() async {
    final presetsBox = Hive.isBoxOpen(presetsBoxName)
        ? Hive.box(presetsBoxName)
        : await Hive.openBox(presetsBoxName);
    final importedBox = Hive.isBoxOpen(importedBoxName)
        ? Hive.box(importedBoxName)
        : await Hive.openBox(importedBoxName);
    final stateBox = Hive.isBoxOpen(activeStateBoxName)
        ? Hive.box(activeStateBoxName)
        : await Hive.openBox(activeStateBoxName);
    return PresetManager._(presetsBox, importedBox, stateBox);
  }

  static final List<SoundProfile> builtInPresets = [
    const SoundProfile(
      id: 'builtin_flat',
      name: 'Flat',
      category: 'Studio',
      description: 'Transparent 0.0 dB studio reference response',
      bands: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      bassBoost: 0,
      reverb: 0,
      reverbRoom: 'None',
      spatial: 0,
      preampDb: 0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_bass_boost',
      name: 'Bass Boost',
      category: 'Studio',
      description: 'Deep sub-bass shelf +8.5 dB with clean low-mid punch',
      bands: [8.5, 7.5, 5.0, 1.5, -0.5, 0.0, 1.0, 2.0, 2.5, 1.5],
      bassBoost: 78,
      reverb: 12,
      reverbRoom: 'Small Room',
      spatial: 35,
      preampDb: -2.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_warm',
      name: 'Warm',
      category: 'Studio',
      description: 'Lush upper-bass warmth with smooth silk-dome treble roll-off',
      bands: [3.0, 4.5, 4.0, 2.5, 1.0, 0.0, -1.0, -2.0, -2.5, -3.5],
      bassBoost: 42,
      reverb: 22,
      reverbRoom: 'Medium Room',
      spatial: 28,
      preampDb: 0.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_theater',
      name: 'Theater',
      category: 'Studio',
      description: 'Wide cinema soundstage with sub-rumble extension and crisp air',
      bands: [6.5, 5.5, 3.0, -0.5, -1.5, 2.0, 4.0, 5.0, 4.5, 3.5],
      bassBoost: 64,
      reverb: 55,
      reverbRoom: 'Large Hall',
      spatial: 82,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_dialogue_boost',
      name: 'Dialogue Boost',
      category: 'Studio',
      description: 'High-pass sub cut with focused 1kHz–4kHz vocal intelligibility',
      bands: [-4.5, -3.0, -1.0, 2.5, 5.5, 7.5, 6.5, 3.5, 0.5, -2.0],
      bassBoost: 0,
      reverb: 5,
      reverbRoom: 'None',
      spatial: 18,
      preampDb: 2.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_action_mode',
      name: 'Action Mode',
      category: 'Studio',
      description: 'High-impact low-end slam and hyper-detailed transient presence',
      bands: [7.5, 6.5, 4.0, 0.5, -2.0, 1.5, 4.5, 6.5, 7.0, 5.5],
      bassBoost: 85,
      reverb: 38,
      reverbRoom: 'Medium Room',
      spatial: 75,
      preampDb: -1.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_soft',
      name: 'Soft',
      category: 'Studio',
      description: 'Gentle midrange cushion with tamed sibilance and sub-bass',
      bands: [-2.0, -1.0, 0.5, 1.5, 2.0, 1.0, -0.5, -2.5, -3.5, -4.5],
      bassBoost: 15,
      reverb: 25,
      reverbRoom: 'Small Room',
      spatial: 30,
      preampDb: 0.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_late_night',
      name: 'Late Night',
      category: 'Studio',
      description: 'Attenuates heavy sub-bass wall-shake while keeping whispers clear',
      bands: [-7.0, -5.0, -2.0, 2.0, 4.0, 4.5, 3.0, -0.5, -2.5, -4.0],
      bassBoost: 0,
      reverb: 15,
      reverbRoom: 'Small Room',
      spatial: 45,
      preampDb: 3.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'builtin_vocal_clarity',
      name: 'Vocal Clarity',
      category: 'Studio',
      description: 'Articulate lead vocals, acoustic guitars, and harmonic air',
      bands: [-3.5, -2.5, -1.0, 3.0, 6.0, 7.0, 5.5, 3.5, 2.0, 1.0],
      bassBoost: 12,
      reverb: 28,
      reverbRoom: 'Plate',
      spatial: 48,
      preampDb: 1.5,
      isBuiltIn: true,
    ),
  ];

  /// 15 Latest Oct 2026 Device-Specific EQ Presets (AutoEQ & RTINGS Tuned)
  static final List<SoundProfile> devicePresets = [
    const SoundProfile(
      id: 'device_airpods_pro_3',
      name: 'Apple AirPods Pro 3',
      category: 'Device',
      description: 'Warm with slight bass boost, vocal clarity (H3 Adaptive EQ)',
      bands: [3.5, 3.0, 2.0, 0.5, 1.0, 2.5, 3.5, 2.0, 1.5, 2.5],
      bassBoost: 48,
      reverb: 14,
      reverbRoom: 'Small Room',
      spatial: 68,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_airpods_4_anc',
      name: 'Apple AirPods 4 ANC',
      category: 'Device',
      description: 'Balanced flat with airy highs & open-fit sub compensation',
      bands: [4.0, 2.5, 0.5, 0.0, 0.0, 0.5, 1.5, 3.0, 4.5, 5.0],
      bassBoost: 38,
      reverb: 10,
      reverbRoom: 'Small Room',
      spatial: 58,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_sony_wf1000xm5',
      name: 'Sony WF-1000XM5',
      category: 'Device',
      description: 'Harman tuned, deep sub-bass 55Hz +3dB & upper-mid clarity',
      bands: [3.5, 3.0, 1.0, -1.5, -1.0, 1.5, 3.5, 4.0, 2.5, 3.0],
      bassBoost: 55,
      reverb: 15,
      reverbRoom: 'Small Room',
      spatial: 45,
      preampDb: -1.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_sony_wf1000xm6',
      name: 'Sony WF-1000XM6',
      category: 'Device',
      description: 'Improved XM5 curve, less mid-bass bloat & tighter transients',
      bands: [3.0, 2.0, -1.5, -2.5, -0.5, 1.5, 3.0, 3.5, 3.0, 2.5],
      bassBoost: 44,
      reverb: 12,
      reverbRoom: 'Small Room',
      spatial: 52,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_sony_wh1000xm6',
      name: 'Sony WH-1000XM6',
      category: 'Device',
      description: 'Studio reference with LDAC optimization & de-mudded 250Hz',
      bands: [2.0, 1.5, -2.0, -3.0, -0.5, 1.0, 2.5, 3.5, 4.0, 3.5],
      bassBoost: 40,
      reverb: 22,
      reverbRoom: 'Medium Room',
      spatial: 62,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_galaxy_buds_3_pro',
      name: 'Samsung Galaxy Buds 3 Pro',
      category: 'Device',
      description: 'Dual driver planar + dynamic curve, boosted presence 2–4kHz',
      bands: [2.5, 2.0, 0.5, -0.5, 0.5, 2.0, 4.5, 4.0, 2.5, 2.0],
      bassBoost: 50,
      reverb: 16,
      reverbRoom: 'Small Room',
      spatial: 60,
      preampDb: -1.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_galaxy_buds_4_pro',
      name: 'Samsung Galaxy Buds 4 Pro',
      category: 'Device',
      description: 'Next-gen acoustic powerhouse curve with ultra-wide treble',
      bands: [4.0, 3.5, 1.5, 0.0, 1.0, 2.5, 4.0, 4.5, 3.5, 4.0],
      bassBoost: 62,
      reverb: 18,
      reverbRoom: 'Medium Room',
      spatial: 72,
      preampDb: -2.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_bose_qc_ultra_2',
      name: 'Bose QuietComfort Ultra 2nd Gen',
      category: 'Device',
      description: 'Immersive audio spatial curve with controlled upper-bass',
      bands: [3.0, 2.5, -1.0, -1.5, 0.5, 2.0, 3.0, 3.5, 4.0, 3.5],
      bassBoost: 52,
      reverb: 32,
      reverbRoom: 'Large Hall',
      spatial: 88,
      preampDb: -1.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_sennheiser_momentum_4',
      name: 'Sennheiser Momentum 4',
      category: 'Device',
      description: 'Audiophile aptX Lossless flat curve with sub-bass taming',
      bands: [-1.5, -1.0, -0.5, 0.5, 1.0, 1.5, 2.0, 1.5, 2.0, 2.5],
      bassBoost: 25,
      reverb: 14,
      reverbRoom: 'Small Room',
      spatial: 42,
      preampDb: 0.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_pixel_buds_pro_2',
      name: 'Google Pixel Buds Pro 2',
      category: 'Device',
      description: 'Google signature clear voice & crisp midrange curve',
      bands: [2.5, 2.0, 0.5, 1.5, 3.0, 4.0, 3.5, 2.0, 1.5, 2.0],
      bassBoost: 36,
      reverb: 10,
      reverbRoom: 'Small Room',
      spatial: 54,
      preampDb: -0.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_jbl_tour_pro_3',
      name: 'JBL Tour Pro 3',
      category: 'Device',
      description: 'Smart case curve with extra sub-bass slam',
      bands: [6.5, 5.5, 3.0, 0.0, -0.5, 1.0, 2.5, 3.5, 4.0, 3.0],
      bassBoost: 76,
      reverb: 20,
      reverbRoom: 'Medium Room',
      spatial: 65,
      preampDb: -2.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_oneplus_buds_pro_3',
      name: 'OnePlus Buds Pro 3',
      category: 'Device',
      description: 'Dynaudio co-tuned bass heavy punchy curve',
      bands: [7.0, 6.0, 3.5, -0.5, -1.0, 1.5, 3.5, 4.5, 4.0, 3.5],
      bassBoost: 80,
      reverb: 18,
      reverbRoom: 'Small Room',
      spatial: 58,
      preampDb: -2.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_nothing_ear_a',
      name: 'Nothing Ear (a)',
      category: 'Device',
      description: 'Style tuned transparent curve with crisp highs & tight kick',
      bands: [3.5, 3.0, 1.0, 0.0, 0.5, 2.0, 3.5, 4.5, 5.0, 4.0],
      bassBoost: 54,
      reverb: 14,
      reverbRoom: 'Plate',
      spatial: 50,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_indian_budget_buds',
      name: 'Boat Airdopes / Noise / Realme Buds',
      category: 'Device',
      description: 'Indian budget buds flat curve (tames boomy 125Hz mid-bass)',
      bands: [-2.0, -3.5, -4.5, -1.5, 1.5, 3.5, 4.5, 3.5, 2.5, 2.0],
      bassBoost: 22,
      reverb: 12,
      reverbRoom: 'Small Room',
      spatial: 46,
      preampDb: 0.5,
      isBuiltIn: true,
    ),
    const SoundProfile(
      id: 'device_over_ear_generic',
      name: 'Over-ear Headphones Generic',
      category: 'Device',
      description: 'Harman over-ear target with natural soundstage air',
      bands: [4.0, 3.5, 1.5, -0.5, 0.0, 1.5, 3.0, 3.5, 2.5, 3.0],
      bassBoost: 48,
      reverb: 26,
      reverbRoom: 'Medium Room',
      spatial: 70,
      preampDb: -1.0,
      isBuiltIn: true,
    ),
  ];

  /// Loads all custom sound profiles stored in Hive, sorted newest first.
  List<SoundProfile> loadCustomPresets() {
    final List<SoundProfile> result = [];
    for (final key in _presetsBox.keys) {
      final raw = _presetsBox.get(key);
      if (raw is Map) {
        result.add(SoundProfile.fromMap(raw));
      }
    }
    result.sort((a, b) => b.createdAtMs.compareTo(a.createdAtMs));
    return result;
  }

  /// Loads all imported presets stored in Hive ("My Imports"), sorted newest first.
  List<SoundProfile> loadImportedPresets() {
    final List<SoundProfile> result = [];
    for (final key in _importedBox.keys) {
      final raw = _importedBox.get(key);
      if (raw is Map) {
        result.add(SoundProfile.fromMap(raw));
      }
    }
    result.sort((a, b) => b.createdAtMs.compareTo(a.createdAtMs));
    return result;
  }

  /// Returns all built-in presets followed by user-saved custom presets.
  List<SoundProfile> getAllPresets() {
    return [...builtInPresets, ...loadCustomPresets()];
  }

  /// Finds a preset by name (case-insensitive) across built-in, device, imported, and custom profiles.
  SoundProfile? findPresetByName(String name) {
    final normalized = name.trim().toLowerCase();
    for (final preset in [
      ...builtInPresets,
      ...devicePresets,
      ...loadImportedPresets(),
      ...loadCustomPresets(),
    ]) {
      if (preset.name.toLowerCase() == normalized) {
        return preset;
      }
    }
    return null;
  }

  /// Parses .json, .equltra, or AutoEQ .txt format into a SoundProfile.
  static SoundProfile? parseImportedContent(
    String rawContent, {
    String fallbackName = 'Imported Preset',
  }) {
    final trimmed = rawContent.trim();
    if (trimmed.isEmpty) return null;

    final cleanFallback = fallbackName
        .split('.')
        .first
        .replaceAll('_', ' ')
        .trim();

    if (trimmed.startsWith('{')) {
      try {
        final decoded = jsonDecode(trimmed) as Map<String, dynamic>;
        final name = (decoded['name'] as String?)?.trim().isNotEmpty == true
            ? (decoded['name'] as String).trim()
            : cleanFallback;
        final rawBands = decoded['bands'] as List<dynamic>?;
        final bands = List<double>.filled(10, 0.0);
        if (rawBands != null) {
          for (int i = 0; i < math.min(10, rawBands.length); i++) {
            bands[i] = (rawBands[i] as num).toDouble().clamp(-15.0, 15.0);
          }
        }
        final bass =
            ((decoded['bass'] ?? decoded['bassBoost'] ?? 50) as num).toDouble();
        final spatial =
            ((decoded['spatial'] ?? decoded['spatialLevel'] ?? 45) as num)
                .toDouble();
        final reverb =
            ((decoded['reverb'] ?? decoded['reverbLevel'] ?? 15) as num)
                .toDouble();
        final preamp =
            ((decoded['preamp'] ?? decoded['preampDb'] ?? 0.0) as num)
                .toDouble();

        return SoundProfile(
          id: 'import_${DateTime.now().millisecondsSinceEpoch}',
          name: name,
          category: 'Imported',
          description: (decoded['description'] as String?) ??
              'Imported .json / .equltra profile',
          bands: bands,
          bassBoost: bass.clamp(0.0, 100.0),
          reverb: reverb.clamp(0.0, 100.0),
          spatial: spatial.clamp(0.0, 100.0),
          preampDb: preamp.clamp(-12.0, 12.0),
          isBuiltIn: false,
          createdAtMs: DateTime.now().millisecondsSinceEpoch,
        );
      } catch (_) {}
    }

    // Parse AutoEQ .txt format ("Preamp: -4.2 dB", "Filter N: ON PK Fc ... Hz Gain ... dB", "GraphicEQ:")
    final bands = List<double>.filled(10, 0.0);
    double preampDb = 0.0;
    bool matched = false;

    final preampRegex =
        RegExp(r'Preamp:\s*([+-]?\d+(?:\.\d+)?)\s*dB', caseSensitive: false);
    final filterRegex = RegExp(
      r'Filter\s+\d+:\s*ON\s+\w+\s+Fc\s+(\d+(?:\.\d+)?)\s*Hz\s+Gain\s+([+-]?\d+(?:\.\d+)?)\s*dB',
      caseSensitive: false,
    );

    for (final line in trimmed.split('\n')) {
      final l = line.trim();
      final pm = preampRegex.firstMatch(l);
      if (pm != null) {
        preampDb = (double.tryParse(pm.group(1) ?? '0') ?? 0.0).clamp(-12.0, 12.0);
      }
      final fm = filterRegex.firstMatch(l);
      if (fm != null) {
        final fc = double.tryParse(fm.group(1) ?? '');
        final gain = double.tryParse(fm.group(2) ?? '');
        if (fc != null && gain != null && fc > 10.0) {
          matched = true;
          int bestIdx = 0;
          double bestDist = double.infinity;
          for (int b = 0; b < 10; b++) {
            final dist =
                (math.log(fc) - math.log(targetFrequenciesHz[b])).abs();
            if (dist < bestDist) {
              bestDist = dist;
              bestIdx = b;
            }
          }
          bands[bestIdx] = (bands[bestIdx] + gain).clamp(-15.0, 15.0);
        }
      }
    }

    if (!matched) return null;

    return SoundProfile(
      id: 'import_${DateTime.now().millisecondsSinceEpoch}',
      name: cleanFallback.isEmpty ? 'AutoEQ Imported' : cleanFallback,
      category: 'Imported',
      description: 'AutoEQ parametric .txt imported curve',
      bands: bands,
      bassBoost: 48.0,
      reverb: 14.0,
      spatial: 50.0,
      preampDb: preampDb,
      isBuiltIn: false,
      createdAtMs: DateTime.now().millisecondsSinceEpoch,
    );
  }

  /// Saves an imported sound profile to Hive ("My Imports").
  Future<SoundProfile> saveImportedPreset(SoundProfile profile) async {
    final id = profile.id.startsWith('import_')
        ? profile.id
        : 'import_${DateTime.now().millisecondsSinceEpoch}';
    final updated = profile.copyWith(
      id: id,
      category: 'Imported',
      isBuiltIn: false,
      createdAtMs: DateTime.now().millisecondsSinceEpoch,
    );
    await _importedBox.put(id, updated.toMap());
    await saveActiveState(profile: updated, enabled: isDspEnabled());
    return updated;
  }

  /// Deletes an imported sound profile from Hive.
  Future<void> deleteImportedPreset(String idOrName) async {
    if (_importedBox.containsKey(idOrName)) {
      await _importedBox.delete(idOrName);
      return;
    }
    final targetKey = _importedBox.keys.cast<dynamic>().firstWhere(
      (key) {
        final val = _importedBox.get(key);
        return val is Map &&
            (val['id'] == idOrName ||
                (val['name'] as String?)?.toLowerCase() ==
                    idOrName.toLowerCase());
      },
      orElse: () => null,
    );
    if (targetKey != null) {
      await _importedBox.delete(targetKey);
    }
  }

  /// Saves or overwrites a custom sound profile in Hive.
  Future<SoundProfile> saveCustomPreset({
    required String name,
    String description = 'User saved 10-band sound profile',
    required List<double> bands,
    required double bassBoost,
    required double reverb,
    String reverbRoom = 'Small Room',
    required double spatial,
    required double preampDb,
  }) async {
    final cleanName = name.trim().isEmpty ? 'My Sound Profile' : name.trim();
    final id =
        'custom_${cleanName.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]+'), '_')}';
    final profile = SoundProfile(
      id: id,
      name: cleanName,
      category: 'Custom',
      description: description.trim().isEmpty
          ? 'User saved 10-band sound profile'
          : description.trim(),
      bands: List<double>.from(bands.map((b) => b.clamp(-15.0, 15.0))),
      bassBoost: bassBoost.clamp(0.0, 100.0),
      reverb: reverb.clamp(0.0, 100.0),
      reverbRoom: reverbRoom,
      spatial: spatial.clamp(0.0, 100.0),
      preampDb: preampDb.clamp(-12.0, 12.0),
      isBuiltIn: false,
      createdAtMs: DateTime.now().millisecondsSinceEpoch,
    );
    await _presetsBox.put(id, profile.toMap());
    await saveActiveState(profile: profile, enabled: isDspEnabled());
    return profile;
  }

  /// Deletes a custom sound profile from Hive by ID or name.
  Future<void> deleteCustomPreset(String idOrName) async {
    await deleteImportedPreset(idOrName);
    if (_presetsBox.containsKey(idOrName)) {
      await _presetsBox.delete(idOrName);
      return;
    }
    final targetKey = _presetsBox.keys.cast<dynamic>().firstWhere(
      (key) {
        final val = _presetsBox.get(key);
        return val is Map &&
            (val['id'] == idOrName ||
                (val['name'] as String?)?.toLowerCase() ==
                    idOrName.toLowerCase());
      },
      orElse: () => null,
    );
    if (targetKey != null) {
      await _presetsBox.delete(targetKey);
    }
  }

  /// Persists the currently active EQ frequency settings and DSP values in Hive.
  Future<void> saveActiveState({
    required SoundProfile profile,
    required bool enabled,
  }) async {
    await _stateBox.put('enabled', enabled);
    await _stateBox.put('preset', profile.name);
    await _stateBox.put('bands', profile.bands);
    await _stateBox.put('bassBoost', profile.bassBoost);
    await _stateBox.put('reverb', profile.reverb);
    await _stateBox.put('reverbRoom', profile.reverbRoom);
    await _stateBox.put('spatial', profile.spatial);
    await _stateBox.put('preampDb', profile.preampDb);
  }

  bool isDspEnabled() {
    return _stateBox.get('enabled', defaultValue: true) as bool;
  }

  bool isBackgroundModeEnabled() {
    return _stateBox.get('backgroundModeEnabled', defaultValue: true) as bool;
  }

  Future<void> setBackgroundModeEnabled(bool enabled) async {
    await _stateBox.put('backgroundModeEnabled', enabled);
  }

  bool isAiProUnlocked() {
    return _stateBox.get('aiProUnlocked', defaultValue: false) as bool;
  }

  Future<void> setAiProUnlocked(bool unlocked) async {
    await _stateBox.put('aiProUnlocked', unlocked);
  }

  bool verifyOwnerProAccess(String input) {
    final normalized = input.trim().toLowerCase();
    const validOwnerKeys = {
      'hemantkumar30590@gmail.com',
      'hemantkumar30590-tech',
      'hemantkumar30590',
      'hemant30590',
      '30590',
    };
    return validOwnerKeys.contains(normalized);
  }

  /// Loads the last active SoundProfile from Hive, or falls back to 'Bass Boost'.
  SoundProfile loadActiveProfile() {
    final presetName =
        _stateBox.get('preset', defaultValue: 'Bass Boost') as String;
    final rawBands = _stateBox.get('bands') as List<dynamic>?;
    final fallback = findPresetByName(presetName) ?? builtInPresets[1];

    final bands = rawBands != null && rawBands.length == 10
        ? rawBands.map((e) => (e as num).toDouble().clamp(-15.0, 15.0)).toList()
        : List<double>.from(fallback.bands);

    return SoundProfile(
      id: fallback.id,
      name: presetName,
      category: fallback.category,
      description: fallback.description,
      bands: bands,
      bassBoost:
          (_stateBox.get('bassBoost', defaultValue: fallback.bassBoost) as num)
              .toDouble(),
      reverb: (_stateBox.get('reverb', defaultValue: fallback.reverb) as num)
          .toDouble(),
      reverbRoom:
          _stateBox.get('reverbRoom', defaultValue: fallback.reverbRoom)
              as String,
      spatial: (_stateBox.get('spatial', defaultValue: fallback.spatial) as num)
          .toDouble(),
      preampDb:
          (_stateBox.get('preampDb', defaultValue: fallback.preampDb) as num)
              .toDouble(),
      isBuiltIn: fallback.isBuiltIn,
    );
  }
}
