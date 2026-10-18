import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:equalizer/equalizer.dart';
import 'package:bass_boost/bass_boost.dart';
import 'preset_manager.dart';
import 'background_eq_service.dart';
import 'spectrum_analyzer.dart';

late final PresetManager globalPresetManager;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Hive.initFlutter();
  globalPresetManager = await PresetManager.init();
  await Equalizer.init(0);
  await BassBoost.init(0);
  runApp(const ProviderScope(child: EqultraApp()));
}

const List<String> kFreqLabels = [
  '31Hz', '62Hz', '125Hz', '250Hz', '500Hz',
  '1kHz', '2kHz', '4kHz', '8kHz', '16kHz',
];

class EqState {
  final bool enabled;
  final bool backgroundServiceEnabled;
  final int selectedTab; // 0: 10-Band EQ, 1: Bass & DSP, 2: Device Presets, 3: Presets
  final String deviceSearchQuery;
  final String activePreset;
  final List<double> bands;
  final double bassBoost;
  final double reverb;
  final String reverbRoom;
  final double spatial;
  final double preampDb;
  final List<SoundProfile> allPresets;
  final List<SoundProfile> importedPresets;

  const EqState({
    required this.enabled,
    required this.backgroundServiceEnabled,
    this.selectedTab = 0,
    this.deviceSearchQuery = '',
    required this.activePreset,
    required this.bands,
    required this.bassBoost,
    required this.reverb,
    required this.reverbRoom,
    required this.spatial,
    required this.preampDb,
    required this.allPresets,
    required this.importedPresets,
  });

  EqState copyWith({
    bool? enabled,
    bool? backgroundServiceEnabled,
    int? selectedTab,
    String? deviceSearchQuery,
    String? activePreset,
    List<double>? bands,
    double? bassBoost,
    double? reverb,
    String? reverbRoom,
    double? spatial,
    double? preampDb,
    List<SoundProfile>? allPresets,
    List<SoundProfile>? importedPresets,
  }) {
    return EqState(
      enabled: enabled ?? this.enabled,
      backgroundServiceEnabled:
          backgroundServiceEnabled ?? this.backgroundServiceEnabled,
      selectedTab: selectedTab ?? this.selectedTab,
      deviceSearchQuery: deviceSearchQuery ?? this.deviceSearchQuery,
      activePreset: activePreset ?? this.activePreset,
      bands: bands ?? this.bands,
      bassBoost: bassBoost ?? this.bassBoost,
      reverb: reverb ?? this.reverb,
      reverbRoom: reverbRoom ?? this.reverbRoom,
      spatial: spatial ?? this.spatial,
      preampDb: preampDb ?? this.preampDb,
      allPresets: allPresets ?? this.allPresets,
      importedPresets: importedPresets ?? this.importedPresets,
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
            importedPresets: _presetManager.loadImportedPresets(),
          ),
        ) {
    _bgService = BackgroundEqService(
      presetManager: _presetManager,
      initialProfile: _presetManager.loadActiveProfile(),
      initialDspEnabled: state.enabled,
    );
    _syncDsp();
  }

  void selectTab(int index) {
    state = state.copyWith(selectedTab: index);
  }

  void setDeviceSearchQuery(String query) {
    state = state.copyWith(deviceSearchQuery: query);
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

  Future<SoundProfile?> importProfileFromRaw(
    String rawContent, {
    String fallbackName = 'Imported Preset',
  }) async {
    final parsed = PresetManager.parseImportedContent(
      rawContent,
      fallbackName: fallbackName,
    );
    if (parsed == null) return null;
    final saved = await _presetManager.saveImportedPreset(parsed);
    state = state.copyWith(
      activePreset: saved.name,
      bands: List<double>.from(saved.bands),
      bassBoost: saved.bassBoost,
      reverb: saved.reverb,
      reverbRoom: saved.reverbRoom,
      spatial: saved.spatial,
      preampDb: saved.preampDb,
      importedPresets: _presetManager.loadImportedPresets(),
    );
    _syncDsp();
    return saved;
  }

  String exportCurrentAsJson() {
    return _currentProfileSnapshot().toExportJson();
  }

  Future<void> deleteCustomProfile(SoundProfile profile) async {
    if (profile.isBuiltIn) return;
    await _presetManager.deleteCustomPreset(profile.id);
    state = state.copyWith(
      allPresets: _presetManager.getAllPresets(),
      importedPresets: _presetManager.loadImportedPresets(),
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

class EqultraApp extends StatelessWidget {
  const EqultraApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Equltra',
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

  Future<void> _pickAndImportFile(
    BuildContext context,
    EqNotifier notifier,
  ) async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['json', 'txt', 'equltra'],
      withData: true,
    );
    if (result == null || result.files.isEmpty) return;
    final picked = result.files.single;
    String content = '';
    if (picked.bytes != null) {
      content = String.fromCharCodes(picked.bytes!);
    } else if (picked.path != null) {
      content = await File(picked.path!).readAsString();
    }
    final imported = await notifier.importProfileFromRaw(
      content,
      fallbackName: picked.name,
    );
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            imported != null
                ? "Imported '${imported.name}' to My Imports!"
                : 'Invalid preset file (.json, AutoEQ .txt, or .equltra required)',
          ),
        ),
      );
    }
  }

  Future<void> _exportAndShareJson(
    BuildContext context,
    EqNotifier notifier,
    String presetName,
  ) async {
    final jsonContent = notifier.exportCurrentAsJson();
    final safeName = presetName.replaceAll(RegExp(r'[^a-zA-Z0-9_-]'), '_');
    final tempDir = await getTemporaryDirectory();
    final file = File('${tempDir.path}/$safeName.json');
    await file.writeAsString(jsonContent);
    await Share.shareXFiles(
      [XFile(file.path)],
      text: 'Equltra 10-Band Preset: $presetName',
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final eq = ref.watch(eqProvider);
    final notifier = ref.read(eqProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: Row(
          children: [
            const Text(
              'Equltra',
              style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF00F0FF)),
            ),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: const Color(0xFF00F0FF),
                borderRadius: BorderRadius.circular(4),
              ),
              child: const Text(
                'v1.0 PRO',
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
            ),
          ],
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
      bottomNavigationBar: NavigationBar(
        backgroundColor: const Color(0xFF090C14),
        selectedIndex: eq.selectedTab,
        onDestinationSelected: notifier.selectTab,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.equalizer),
            label: '10-Band EQ',
          ),
          NavigationDestination(
            icon: Icon(Icons.speaker),
            label: 'Bass & DSP',
          ),
          NavigationDestination(
            icon: Icon(Icons.headphones),
            label: 'Device Presets',
          ),
          NavigationDestination(
            icon: Icon(Icons.library_music),
            label: 'Presets',
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: _buildCurrentTab(context, eq, notifier),
      ),
    );
  }

  Widget _buildCurrentTab(
    BuildContext context,
    EqState eq,
    EqNotifier notifier,
  ) {
    switch (eq.selectedTab) {
      case 1:
        return SingleChildScrollView(
          child: Column(
            children: [
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
        );
      case 2:
        return _buildDevicePresetsScreen(context, eq, notifier);
      case 3:
        return _buildPresetsAndImportsScreen(context, eq, notifier);
      case 0:
      default:
        return _buildEqualizerTab(context, eq, notifier);
    }
  }

  Widget _buildEqualizerTab(
    BuildContext context,
    EqState eq,
    EqNotifier notifier,
  ) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SpectrumAnalyzerCanvas(
            bandsDb: eq.bands,
            fftBins: List<double>.generate(
              24,
              (i) => (((eq.bands[(i * 9 ~/ 23).clamp(0, 9)] + 15.0) / 30.0) *
                      0.78 +
                  0.08)
                  .clamp(0.06, 0.96),
            ),
            peakHoldBins: List<double>.generate(
              24,
              (i) => (((eq.bands[(i * 9 ~/ 23).clamp(0, 9)] + 15.0) / 30.0) *
                      0.85 +
                  0.12)
                  .clamp(0.08, 0.99),
            ),
            enabled: eq.enabled,
            isAudioRecordLive: eq.enabled,
            peakDbFs: -6.5 + (eq.preampDb * 0.4),
            dominantFreqHz: 125,
            onToggleAudioRecord: () => notifier.toggleEnabled(!eq.enabled),
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
        ],
      ),
    );
  }

  Widget _buildDevicePresetsScreen(
    BuildContext context,
    EqState eq,
    EqNotifier notifier,
  ) {
    final filtered = PresetManager.devicePresets.where((d) {
      if (eq.deviceSearchQuery.trim().isEmpty) return true;
      final q = eq.deviceSearchQuery.toLowerCase();
      return d.name.toLowerCase().contains(q) ||
          d.description.toLowerCase().contains(q);
    }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          onChanged: notifier.setDeviceSearchQuery,
          decoration: InputDecoration(
            hintText: 'Search your earbuds...',
            prefixIcon: const Icon(Icons.search, color: Color(0xFF00F0FF)),
            filled: true,
            fillColor: const Color(0xFF101522),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Expanded(
          child: ListView.builder(
            itemCount: filtered.length,
            itemBuilder: (context, idx) {
              final device = filtered[idx];
              final isSelected =
                  eq.activePreset.toLowerCase() == device.name.toLowerCase();
              return Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFF101522),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: isSelected
                        ? const Color(0xFF00F0FF)
                        : const Color(0xFF1E273C),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.headphones, color: Color(0xFF00F0FF)),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                device.name,
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                ),
                              ),
                              Text(
                                device.description,
                                style: const TextStyle(
                                  fontSize: 11,
                                  color: Colors.white70,
                                ),
                              ),
                            ],
                          ),
                        ),
                        FilledButton(
                          onPressed: () => notifier.applyProfile(device),
                          child: Text(isSelected ? 'APPLIED' : 'APPLY'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'BASS ${device.bassSummaryDb >= 0 ? '+' : ''}${device.bassSummaryDb}dB • '
                          'MID ${device.midSummaryDb >= 0 ? '+' : ''}${device.midSummaryDb}dB • '
                          'TREBLE ${device.trebleSummaryDb >= 0 ? '+' : ''}${device.trebleSummaryDb}dB',
                          style: const TextStyle(
                            fontSize: 11,
                            color: Color(0xFF00F0FF),
                          ),
                        ),
                        Text(
                          'BASS ${device.bassBoost.round()}% • 3D ${device.spatial.round()}%',
                          style: const TextStyle(
                            fontSize: 11,
                            color: Color(0xFFFF2A85),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildPresetsAndImportsScreen(
    BuildContext context,
    EqState eq,
    EqNotifier notifier,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                onPressed: () => _pickAndImportFile(context, notifier),
                icon: const Icon(Icons.file_open),
                label: const Text('Import'),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () =>
                    _exportAndShareJson(context, notifier, eq.activePreset),
                icon: const Icon(Icons.ios_share, color: Color(0xFFFF2A85)),
                label: const Text(
                  'Export',
                  style: TextStyle(color: Color(0xFFFF2A85)),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Text(
          'MY IMPORTS (${eq.importedPresets.length})',
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Color(0xFFFF2A85),
          ),
        ),
        const SizedBox(height: 8),
        Expanded(
          child: ListView(
            children: [
              ...eq.importedPresets.map(
                (imp) => ListTile(
                  title: Text(imp.name),
                  subtitle: Text(imp.description),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete, color: Color(0xFFFF2A85)),
                    onPressed: () => notifier.deleteCustomProfile(imp),
                  ),
                  onTap: () => notifier.applyProfile(imp),
                ),
              ),
              const Divider(),
              ...eq.allPresets.map(
                (p) => ListTile(
                  title: Text(p.name),
                  subtitle: Text(p.description),
                  trailing: p.isBuiltIn
                      ? null
                      : IconButton(
                          icon: const Icon(Icons.delete, color: Color(0xFFFF2A85)),
                          onPressed: () => notifier.deleteCustomProfile(p),
                        ),
                  onTap: () => notifier.applyProfile(p),
                ),
              ),
            ],
          ),
        ),
      ],
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
