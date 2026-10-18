import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:equalizer/equalizer.dart';
import 'package:bass_boost/bass_boost.dart';
import 'preset_manager.dart';
import 'background_eq_service.dart';

late final PresetManager globalPresetManager;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Hive.initFlutter();
  globalPresetManager = await PresetManager.init();
  await Equalizer.init(0);
  await BassBoost.init(0);
  runApp(const ProviderScope(child: VoidEqUltraApp()));
}

const List<String> kFreqLabels = [
  '31Hz', '62Hz', '125Hz', '250Hz', '500Hz',
  '1kHz', '2kHz', '4kHz', '8kHz', '16kHz',
];

class EqState {
  final bool enabled;
  final bool backgroundServiceEnabled;
  final String activePreset;
  final List<double> bands;
  final double bassBoost;
  final double reverb;
  final String reverbRoom;
  final double spatial;
  final double preampDb;
  final List<SoundProfile> allPresets;

  const EqState({
    required this.enabled,
    required this.backgroundServiceEnabled,
    required this.activePreset,
    required this.bands,
    required this.bassBoost,
    required this.reverb,
    required this.reverbRoom,
    required this.spatial,
    required this.preampDb,
    required this.allPresets,
  });

  EqState copyWith({
    bool? enabled,
    bool? backgroundServiceEnabled,
    String? activePreset,
    List<double>? bands,
    double? bassBoost,
    double? reverb,
    String? reverbRoom,
    double? spatial,
    double? preampDb,
    List<SoundProfile>? allPresets,
  }) {
    return EqState(
      enabled: enabled ?? this.enabled,
      backgroundServiceEnabled:
          backgroundServiceEnabled ?? this.backgroundServiceEnabled,
      activePreset: activePreset ?? this.activePreset,
      bands: bands ?? this.bands,
      bassBoost: bassBoost ?? this.bassBoost,
      reverb: reverb ?? this.reverb,
      reverbRoom: reverbRoom ?? this.reverbRoom,
      spatial: spatial ?? this.spatial,
      preampDb: preampDb ?? this.preampDb,
      allPresets: allPresets ?? this.allPresets,
    );
  }
}

class EqNotifier extends StateNotifier<EqState> {
  final PresetManager _presetManager;
  late final BackgroundEqService _bgService;

  EqNotifier(this._presetManager)
      : super(
          EqState(
            enabled: _presetManager.isDspEnabled(),
            backgroundServiceEnabled: _presetManager.isBackgroundModeEnabled(),
            activePreset: _presetManager.loadActiveProfile().name,
            bands: _presetManager.loadActiveProfile().bands,
            bassBoost: _presetManager.loadActiveProfile().bassBoost,
            reverb: _presetManager.loadActiveProfile().reverb,
            reverbRoom: _presetManager.loadActiveProfile().reverbRoom,
            spatial: _presetManager.loadActiveProfile().spatial,
            preampDb: _presetManager.loadActiveProfile().preampDb,
            allPresets: _presetManager.getAllPresets(),
          ),
        ) {
    _bgService = BackgroundEqService(
      presetManager: _presetManager,
      initialProfile: _presetManager.loadActiveProfile(),
      initialDspEnabled: state.enabled,
    );
    _syncDsp();
  }

  void toggleEnabled(bool value) {
    state = state.copyWith(enabled: value);
    _persistCurrentState();
    Equalizer.setEnabled(value);
    BassBoost.setEnabled(value);
  }

  Future<void> toggleBackgroundService(bool enabled) async {
    state = state.copyWith(backgroundServiceEnabled: enabled);
    if (enabled) {
      await _bgService.startBackgroundService(
        profile: _currentProfileSnapshot(),
        dspEnabled: state.enabled,
      );
    } else {
      await _bgService.stopBackgroundService();
    }
  }

  void setBand(int index, double gainDb) {
    final updated = List<double>.from(state.bands);
    updated[index] = gainDb.clamp(-15.0, 15.0);
    state = state.copyWith(bands: updated, activePreset: 'Custom');
    _persistCurrentState();
    Equalizer.setBandLevel(index, (gainDb * 100).round());
  }

  void applyProfile(SoundProfile profile) {
    state = state.copyWith(
      activePreset: profile.name,
      bands: List<double>.from(profile.bands),
      bassBoost: profile.bassBoost,
      reverb: profile.reverb,
      reverbRoom: profile.reverbRoom,
      spatial: profile.spatial,
      preampDb: profile.preampDb,
    );
    _persistCurrentState();
    _syncDsp();
  }

  Future<void> saveCustomProfile(String name, String description) async {
    final saved = await _presetManager.saveCustomPreset(
      name: name,
      description: description,
      bands: state.bands,
      bassBoost: state.bassBoost,
      reverb: state.reverb,
      reverbRoom: state.reverbRoom,
      spatial: state.spatial,
      preampDb: state.preampDb,
    );
    state = state.copyWith(
      activePreset: saved.name,
      allPresets: _presetManager.getAllPresets(),
    );
    _persistCurrentState();
  }

  Future<void> deleteCustomProfile(SoundProfile profile) async {
    if (profile.isBuiltIn) return;
    await _presetManager.deleteCustomPreset(profile.id);
    state = state.copyWith(
      allPresets: _presetManager.getAllPresets(),
    );
  }

  void setBassBoost(double value) {
    state = state.copyWith(bassBoost: value);
    _persistCurrentState();
    BassBoost.setStrength((value * 10).round());
  }

  void setReverb(double value) {
    state = state.copyWith(reverb: value);
    _persistCurrentState();
  }

  void setSpatial(double value) {
    state = state.copyWith(spatial: value);
    _persistCurrentState();
  }

  void setPreamp(double value) {
    state = state.copyWith(preampDb: value);
    _persistCurrentState();
  }

  SoundProfile _currentProfileSnapshot() {
    return SoundProfile(
      id: 'active',
      name: state.activePreset,
      bands: state.bands,
      bassBoost: state.bassBoost,
      reverb: state.reverb,
      reverbRoom: state.reverbRoom,
      spatial: state.spatial,
      preampDb: state.preampDb,
    );
  }

  void _persistCurrentState() {
    final snapshot = _currentProfileSnapshot();
    _presetManager.saveActiveState(
      profile: snapshot,
      enabled: state.enabled,
    );
    if (state.backgroundServiceEnabled) {
      _bgService.updateNotificationState(
        profile: snapshot,
        dspEnabled: state.enabled,
      );
    }
  }

  void _syncDsp() {
    Equalizer.setEnabled(state.enabled);
    BassBoost.setEnabled(state.enabled);
    BassBoost.setStrength((state.bassBoost * 10).round());
    for (int i = 0; i < state.bands.length; i++) {
      Equalizer.setBandLevel(i, (state.bands[i] * 100).round());
    }
  }

  @override
  void dispose() {
    _bgService.dispose();
    super.dispose();
  }
}

final eqProvider = StateNotifierProvider<EqNotifier, EqState>(
  (ref) => EqNotifier(globalPresetManager),
);

class VoidEqUltraApp extends StatelessWidget {
  const VoidEqUltraApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'VoidEQ Equltra',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        scaffoldBackgroundColor: Colors.black,
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF00F0FF),
          secondary: Color(0xFFFF2A85),
          surface: Color(0xFF0A0E17),
        ),
      ),
      home: const EqultraHomeScreen(),
    );
  }
}

class EqultraHomeScreen extends ConsumerWidget {
  const EqultraHomeScreen({super.key});

  Future<void> _showSavePresetDialog(
    BuildContext context,
    EqNotifier notifier,
  ) async {
    final nameCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF101522),
        title: const Text('Save Custom Sound Profile'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameCtrl,
              decoration: const InputDecoration(
                labelText: 'Profile Name (e.g. Sub Bass X)',
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: descCtrl,
              decoration: const InputDecoration(
                labelText: 'Description (optional)',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              await notifier.saveCustomProfile(nameCtrl.text, descCtrl.text);
              if (ctx.mounted) Navigator.of(ctx).pop();
            },
            child: const Text('Save to Hive'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final eq = ref.watch(eqProvider);
    final notifier = ref.read(eqProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: const Text(
          'VoidEQ Equltra • 10-Band DSP',
          style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF00F0FF)),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.bookmark_add, color: Color(0xFF00F0FF)),
            tooltip: 'Save Custom Preset',
            onPressed: () => _showSavePresetDialog(context, notifier),
          ),
          Switch(
            value: eq.enabled,
            activeColor: const Color(0xFF00F0FF),
            onChanged: notifier.toggleEnabled,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Background Service & Notification Toggle Card
            Container(
              margin: const EdgeInsets.only(bottom: 14),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: const Color(0xFF0A0E17),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: eq.backgroundServiceEnabled
                      ? const Color(0xFF00E676)
                      : const Color(0xFF1E273C),
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.notifications_active,
                    color: eq.backgroundServiceEnabled
                        ? const Color(0xFF00E676)
                        : Colors.white54,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Keep Equalizer Active in Background',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                          ),
                        ),
                        Text(
                          eq.backgroundServiceEnabled
                              ? 'Foreground notification & Global Session 0 active across apps'
                              : 'Paused when switching away from VoidEQ Equltra',
                          style: const TextStyle(
                            fontSize: 11,
                            color: Colors.white70,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Switch(
                    value: eq.backgroundServiceEnabled,
                    activeColor: const Color(0xFF00E676),
                    onChanged: notifier.toggleBackgroundService,
                  ),
                ],
              ),
            ),
            Container(
              height: 140,
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFF0A0E17),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFF1E273C)),
              ),
              child: CustomPaint(
                painter: _EqCurvePainter(eq.bands, eq.enabled),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'SOUND PROFILES (${eq.allPresets.length})',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: Colors.white70,
                  ),
                ),
                TextButton.icon(
                  onPressed: () => _showSavePresetDialog(context, notifier),
                  icon: const Icon(Icons.add, size: 16, color: Color(0xFF00F0FF)),
                  label: const Text(
                    'SAVE PROFILE',
                    style: TextStyle(fontSize: 12, color: Color(0xFF00F0FF)),
                  ),
                ),
              ],
            ),
            SizedBox(
              height: 44,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: eq.allPresets.length,
                itemBuilder: (context, idx) {
                  final profile = eq.allPresets[idx];
                  final selected =
                      eq.activePreset.toLowerCase() == profile.name.toLowerCase();
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: InputChip(
                      label: Text(profile.name),
                      selected: selected,
                      selectedColor: const Color(0xFF00F0FF),
                      labelStyle: TextStyle(
                        color: selected ? Colors.black : Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                      onSelected: (_) => notifier.applyProfile(profile),
                      onDeleted: profile.isBuiltIn
                          ? null
                          : () => notifier.deleteCustomProfile(profile),
                      deleteIconColor:
                          selected ? Colors.black87 : const Color(0xFFFF2A85),
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 240,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: 10,
                itemBuilder: (context, index) {
                  final gain = eq.bands[index];
                  return Container(
                    width: 56,
                    margin: const EdgeInsets.only(right: 8),
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    decoration: BoxDecoration(
                      color: const Color(0xFF0A0E17),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFF1E273C)),
                    ),
                    child: Column(
                      children: [
                        Text(
                          '${gain >= 0 ? '+' : ''}${gain.toStringAsFixed(1)}',
                          style: const TextStyle(fontSize: 11, color: Colors.white70),
                        ),
                        Expanded(
                          child: RotatedBox(
                            quarterTurns: 3,
                            child: Slider(
                              value: gain,
                              min: -15.0,
                              max: 15.0,
                              activeColor: const Color(0xFF00F0FF),
                              onChanged: eq.enabled
                                  ? (val) => notifier.setBand(index, val)
                                  : null,
                            ),
                          ),
                        ),
                        Text(
                          kFreqLabels[index],
                          style: const TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF00F0FF),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 20),
            _buildMacroSlider(
              label: 'Bass Boost (${eq.bassBoost.round()}%)',
              value: eq.bassBoost,
              min: 0,
              max: 100,
              color: const Color(0xFFFF2A85),
              onChanged: notifier.setBassBoost,
            ),
            _buildMacroSlider(
              label: 'Spatial 3D Surround (${eq.spatial.round()}%)',
              value: eq.spatial,
              min: 0,
              max: 100,
              color: const Color(0xFF00F0FF),
              onChanged: notifier.setSpatial,
            ),
            _buildMacroSlider(
              label: 'Studio Reverb (${eq.reverb.round()}%)',
              value: eq.reverb,
              min: 0,
              max: 100,
              color: const Color(0xFF8A3FFC),
              onChanged: notifier.setReverb,
            ),
            _buildMacroSlider(
              label: 'Preamp Gain (${eq.preampDb.toStringAsFixed(1)} dB)',
              value: eq.preampDb,
              min: -12,
              max: 12,
              color: const Color(0xFF00E676),
              onChanged: notifier.setPreamp,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMacroSlider({
    required String label,
    required double value,
    required double min,
    required double max,
    required Color color,
    required ValueChanged<double> onChanged,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFF0A0E17),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF1E273C)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
          Slider(
            value: value,
            min: min,
            max: max,
            activeColor: color,
            onChanged: onChanged,
          ),
        ],
      ),
    );
  }
}

class _EqCurvePainter extends CustomPainter {
  final List<double> bands;
  final bool enabled;

  _EqCurvePainter(this.bands, this.enabled);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = enabled ? const Color(0xFF00F0FF) : Colors.grey
      ..strokeWidth = 3.0
      ..style = PaintingStyle.stroke;

    final path = Path();
    final midY = size.height / 2.0;
    for (int i = 0; i < bands.length; i++) {
      final x = (i / (bands.length - 1)) * size.width;
      final norm = (bands[i].clamp(-15.0, 15.0)) / 15.0;
      final y = midY - norm * (midY * 0.8);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
      canvas.drawCircle(
        Offset(x, y),
        4.0,
        Paint()..color = const Color(0xFFFF2A85),
      );
    }
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _EqCurvePainter oldDelegate) =>
      oldDelegate.bands != bands || oldDelegate.enabled != enabled;
}
