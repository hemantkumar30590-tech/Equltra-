import 'package:hive_flutter/hive_flutter.dart';

class SoundProfile {
  final String id;
  final String name;
  final String description;
  final List<double> bands; // 10-band gains in dB (-15.0 to +15.0)
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
    this.description = 'Custom user sound profile',
    required this.bands,
    this.bassBoost = 0.0,
    this.reverb = 0.0,
    this.reverbRoom = 'Small Room',
    this.spatial = 0.0,
    this.preampDb = 0.0,
    this.isBuiltIn = false,
    this.createdAtMs = 0,
  });

  SoundProfile copyWith({
    String? id,
    String? name,
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
      description:
          (map['description'] as String?) ?? 'Custom user sound profile',
      bands: parsedBands,
      bassBoost: ((map['bassBoost'] as num?) ?? 0.0).toDouble().clamp(0.0, 100.0),
      reverb: ((map['reverb'] as num?) ?? 0.0).toDouble().clamp(0.0, 100.0),
      reverbRoom: (map['reverbRoom'] as String?) ?? 'Small Room',
      spatial: ((map['spatial'] as num?) ?? 0.0).toDouble().clamp(0.0, 100.0),
      preampDb: ((map['preampDb'] as num?) ?? 0.0).toDouble().clamp(-12.0, 12.0),
      isBuiltIn: (map['isBuiltIn'] as bool?) ?? false,
      createdAtMs: ((map['createdAtMs'] as num?) ?? 0).toInt(),
    );
  }
}

/// Manages built-in studio presets and user-created custom sound profiles using Hive.
class PresetManager {
  static const String presetsBoxName = 'equltra_custom_presets';
  static const String activeStateBoxName = 'equltra_settings';

  late final Box _presetsBox;
  late final Box _stateBox;

  PresetManager._(this._presetsBox, this._stateBox);

  static Future<PresetManager> init() async {
    final presetsBox = Hive.isBoxOpen(presetsBoxName)
        ? Hive.box(presetsBoxName)
        : await Hive.openBox(presetsBoxName);
    final stateBox = Hive.isBoxOpen(activeStateBoxName)
        ? Hive.box(activeStateBoxName)
        : await Hive.openBox(activeStateBoxName);
    return PresetManager._(presetsBox, stateBox);
  }

  static final List<SoundProfile> builtInPresets = [
    const SoundProfile(
      id: 'builtin_flat',
      name: 'Flat',
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

  /// Returns all built-in presets followed by user-saved custom presets.
  List<SoundProfile> getAllPresets() {
    return [...builtInPresets, ...loadCustomPresets()];
  }

  /// Finds a preset by name (case-insensitive) across built-in and custom profiles.
  SoundProfile? findPresetByName(String name) {
    final normalized = name.trim().toLowerCase();
    for (final preset in getAllPresets()) {
      if (preset.name.toLowerCase() == normalized) {
        return preset;
      }
    }
    return null;
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
    final id = 'custom_${cleanName.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]+'), '_')}';
    final profile = SoundProfile(
      id: id,
      name: cleanName,
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

  /// Loads the last active SoundProfile from Hive, or falls back to 'Bass Boost'.
  SoundProfile loadActiveProfile() {
    final presetName = _stateBox.get('preset', defaultValue: 'Bass Boost') as String;
    final rawBands = _stateBox.get('bands') as List<dynamic>?;
    final fallback = findPresetByName(presetName) ?? builtInPresets[1];

    final bands = rawBands != null && rawBands.length == 10
        ? rawBands.map((e) => (e as num).toDouble().clamp(-15.0, 15.0)).toList()
        : List<double>.from(fallback.bands);

    return SoundProfile(
      id: fallback.id,
      name: presetName,
      description: fallback.description,
      bands: bands,
      bassBoost: (_stateBox.get('bassBoost', defaultValue: fallback.bassBoost) as num)
          .toDouble(),
      reverb: (_stateBox.get('reverb', defaultValue: fallback.reverb) as num)
          .toDouble(),
      reverbRoom:
          _stateBox.get('reverbRoom', defaultValue: fallback.reverbRoom) as String,
      spatial: (_stateBox.get('spatial', defaultValue: fallback.spatial) as num)
          .toDouble(),
      preampDb: (_stateBox.get('preampDb', defaultValue: fallback.preampDb) as num)
          .toDouble(),
      isBuiltIn: fallback.isBuiltIn,
    );
  }
}
