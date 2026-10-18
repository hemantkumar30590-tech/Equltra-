import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:equalizer/equalizer.dart';
import 'package:bass_boost/bass_boost.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Hive.initFlutter();
  await Hive.openBox('equltra_settings');
  await Equalizer.init(0);
  await BassBoost.init(0);
  runApp(const ProviderScope(child: VoidEqUltraApp()));
}

const List<String> kFreqLabels = [
  '31Hz', '62Hz', '125Hz', '250Hz', '500Hz',
  '1kHz', '2kHz', '4kHz', '8kHz', '16kHz',
];

const Map<String, List<double>> kStudioPresets = {
  'Flat': [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  'Bass Boost': [8.5, 7.5, 5.0, 1.5, -0.5, 0.0, 1.0, 2.0, 2.5, 1.5],
  'Warm': [3.0, 4.5, 4.0, 2.5, 1.0, 0.0, -1.0, -2.0, -2.5, -3.5],
  'Theater': [6.5, 5.5, 3.0, -0.5, -1.5, 2.0, 4.0, 5.0, 4.5, 3.5],
  'Dialogue Boost': [-4.5, -3.0, -1.0, 2.5, 5.5, 7.5, 6.5, 3.5, 0.5, -2.0],
  'Action Mode': [7.5, 6.5, 4.0, 0.5, -2.0, 1.5, 4.5, 6.5, 7.0, 5.5],
  'Soft': [-2.0, -1.0, 0.5, 1.5, 2.0, 1.0, -0.5, -2.5, -3.5, -4.5],
  'Late Night': [-7.0, -5.0, -2.0, 2.0, 4.0, 4.5, 3.0, -0.5, -2.5, -4.0],
  'Vocal Clarity': [-3.5, -2.5, -1.0, 3.0, 6.0, 7.0, 5.5, 3.5, 2.0, 1.0],
};

class EqState {
  final bool enabled;
  final String activePreset;
  final List<double> bands;
  final double bassBoost;
  final double reverb;
  final double spatial;
  final double preampDb;

  const EqState({
    required this.enabled,
    required this.activePreset,
    required this.bands,
    required this.bassBoost,
    required this.reverb,
    required this.spatial,
    required this.preampDb,
  });

  EqState copyWith({
    bool? enabled,
    String? activePreset,
    List<double>? bands,
    double? bassBoost,
    double? reverb,
    double? spatial,
    double? preampDb,
  }) {
    return EqState(
      enabled: enabled ?? this.enabled,
      activePreset: activePreset ?? this.activePreset,
      bands: bands ?? this.bands,
      bassBoost: bassBoost ?? this.bassBoost,
      reverb: reverb ?? this.reverb,
      spatial: spatial ?? this.spatial,
      preampDb: preampDb ?? this.preampDb,
    );
  }
}

class EqNotifier extends StateNotifier<EqState> {
  final Box _box = Hive.box('equltra_settings');

  EqNotifier()
      : super(
          EqState(
            enabled: true,
            activePreset: 'Bass Boost',
            bands: List<double>.from(kStudioPresets['Bass Boost']!),
            bassBoost: 78,
            reverb: 20,
            spatial: 45,
            preampDb: 0.0,
          ),
        ) {
    _loadFromHive();
  }

  void _loadFromHive() {
    final preset = _box.get('preset', defaultValue: 'Bass Boost') as String;
    final rawBands = _box.get('bands') as List<dynamic>?;
    final bands = rawBands != null && rawBands.length == 10
        ? rawBands.map((e) => (e as num).toDouble()).toList()
        : List<double>.from(kStudioPresets[preset] ?? kStudioPresets['Flat']!);
    state = state.copyWith(
      enabled: _box.get('enabled', defaultValue: true) as bool,
      activePreset: preset,
      bands: bands,
      bassBoost: (_box.get('bassBoost', defaultValue: 78.0) as num).toDouble(),
      reverb: (_box.get('reverb', defaultValue: 20.0) as num).toDouble(),
      spatial: (_box.get('spatial', defaultValue: 45.0) as num).toDouble(),
      preampDb: (_box.get('preampDb', defaultValue: 0.0) as num).toDouble(),
    );
    _syncDsp();
  }

  void toggleEnabled(bool value) {
    state = state.copyWith(enabled: value);
    _box.put('enabled', value);
    Equalizer.setEnabled(value);
    BassBoost.setEnabled(value);
  }

  void setBand(int index, double gainDb) {
    final updated = List<double>.from(state.bands);
    updated[index] = gainDb.clamp(-15.0, 15.0);
    state = state.copyWith(bands: updated, activePreset: 'Custom');
    _box.put('bands', updated);
    _box.put('preset', 'Custom');
    Equalizer.setBandLevel(index, (gainDb * 100).round());
  }

  void selectPreset(String name) {
    final presetBands = kStudioPresets[name];
    if (presetBands == null) return;
    final copied = List<double>.from(presetBands);
    state = state.copyWith(activePreset: name, bands: copied);
    _box.put('preset', name);
    _box.put('bands', copied);
    _syncDsp();
  }

  void setBassBoost(double value) {
    state = state.copyWith(bassBoost: value);
    _box.put('bassBoost', value);
    BassBoost.setStrength((value * 10).round());
  }

  void setReverb(double value) {
    state = state.copyWith(reverb: value);
    _box.put('reverb', value);
  }

  void setSpatial(double value) {
    state = state.copyWith(spatial: value);
    _box.put('spatial', value);
  }

  void setPreamp(double value) {
    state = state.copyWith(preampDb: value);
    _box.put('preampDb', value);
  }

  void _syncDsp() {
    Equalizer.setEnabled(state.enabled);
    BassBoost.setEnabled(state.enabled);
    BassBoost.setStrength((state.bassBoost * 10).round());
    for (int i = 0; i < state.bands.length; i++) {
      Equalizer.setBandLevel(i, (state.bands[i] * 100).round());
    }
  }
}

final eqProvider = StateNotifierProvider<EqNotifier, EqState>((ref) => EqNotifier());

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
            // Frequency Response Curve Visualizer
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
            // Preset Chips (All 9 Presets)
            SizedBox(
              height: 42,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: kStudioPresets.keys.map((preset) {
                  final selected = eq.activePreset == preset;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(preset),
                      selected: selected,
                      selectedColor: const Color(0xFF00F0FF),
                      labelStyle: TextStyle(
                        color: selected ? Colors.black : Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                      onSelected: (_) => notifier.selectPreset(preset),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: 16),
            // 10-Band Vertical Sliders
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
            // Bass Boost, Reverb, Spatial & Preamp Controls
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
