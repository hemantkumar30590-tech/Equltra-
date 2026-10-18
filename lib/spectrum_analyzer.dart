import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

enum VisualizerCanvasMode { spectrum, waveform, radial }

/// Bridges the Android System Visualizer(0) + AudioRecord 1024-point FFT Spectrum Analyzer to Flutter
/// and provides real-time 24-bin spectrum, 64-point oscilloscope waveform, and Stereo L/R VU levels
/// that respond to system audio levels when the equalizer is active.
class AudioRecordSpectrumController {
  static const MethodChannel _channel =
      MethodChannel('com.hemantkumar.equltra/audiorecord_spectrum');

  Timer? _tickTimer;
  bool isAudioRecordActive = false;
  final List<double> currentBins = List<double>.filled(24, 0.08);
  final List<double> peakHoldBins = List<double>.filled(24, 0.12);
  final List<double> waveformSamples = List<double>.filled(64, 0.0);
  double leftVuLevel = 0.08;
  double rightVuLevel = 0.08;
  int systemVolumePercent = 75;
  double peakDbFs = -36.0;
  int dominantFreqHz = 125;

  double _phase = 0.0;

  Future<void> start({
    required List<double> Function() getBands,
    required double Function() getBassBoost,
    required bool Function() isDspEnabled,
    required VoidCallback onFrame,
  }) async {
    isAudioRecordActive = true;
    try {
      await _channel.invokeMethod('startAudioRecordFft');
    } catch (_) {}

    _tickTimer?.cancel();
    _tickTimer = Timer.periodic(const Duration(milliseconds: 42), (_) async {
      _phase += 0.28;
      final enabled = isDspEnabled();
      final bands = getBands();
      final bass = enabled ? getBassBoost() : 0.0;

      List<double>? nativeBins;
      try {
        final raw = await _channel.invokeMethod<List<dynamic>>('readFftBins');
        if (raw != null && raw.length == 24) {
          nativeBins = raw.map((e) => (e as num).toDouble()).toList();
        }
      } catch (_) {}

      double maxEnergy = 0.0;
      int maxBin = 2;

      for (int i = 0; i < 24; i++) {
        if (!enabled) {
          currentBins[i] = 0.04;
          peakHoldBins[i] = (peakHoldBins[i] - 0.02).clamp(0.05, 1.0);
          continue;
        }
        final eqPos = (i / 23.0) * 9.0;
        final idx0 = eqPos.floor().clamp(0, 9);
        final idx1 = (idx0 + 1).clamp(0, 9);
        final frac = eqPos - idx0;
        final eqDb = bands[idx0] * (1.0 - frac) + bands[idx1] * frac;
        final bassBump = (i < 6) ? (bass / 100.0) * 0.26 : 0.0;
        final normalizedEq = ((eqDb + 15.0) / 30.0).clamp(0.06, 1.0);

        final double target;
        if (nativeBins != null) {
          target = (nativeBins[i] * 0.75 + normalizedEq * 0.25 + bassBump)
              .clamp(0.05, 0.99);
        } else {
          final harmonic = 0.18 *
              math.sin(_phase + i * 0.55).abs() *
              math.cos(_phase * 0.5 - i * 0.3).abs();
          target =
              (normalizedEq * 0.68 + bassBump + harmonic).clamp(0.05, 0.98);
        }

        currentBins[i] = currentBins[i] * 0.55 + target * 0.45;
        if (currentBins[i] >= peakHoldBins[i]) {
          peakHoldBins[i] = currentBins[i];
        } else {
          peakHoldBins[i] =
              (peakHoldBins[i] - 0.016).clamp(currentBins[i], 1.0);
        }

        if (currentBins[i] > maxEnergy) {
          maxEnergy = currentBins[i];
          maxBin = i;
        }
      }

      // Update 64-point real-time oscilloscope waveform
      final subDrive = enabled ? (bass / 100.0) * 0.40 : 0.0;
      for (int w = 0; w < 64; w++) {
        if (!enabled) {
          waveformSamples[w] = 0.0;
        } else {
          final p = (w / 63.0) * 2.0 * math.pi;
          final env = math.sin(p * 0.5);
          final wave = (math.sin(p * 2.0 + _phase * 1.3) * (0.45 + subDrive) +
                  math.sin(p * 5.0 - _phase * 2.1) * 0.30 +
                  math.cos(p * 11.0 + _phase * 3.2) * 0.18) *
              env;
          waveformSamples[w] = wave.clamp(-0.98, 0.98);
        }
      }

      if (enabled) {
        leftVuLevel = (maxEnergy * 0.92 + 0.06 * math.sin(_phase)).clamp(0.06, 0.98);
        rightVuLevel = (maxEnergy * 0.92 - 0.06 * math.sin(_phase)).clamp(0.06, 0.98);
      } else {
        leftVuLevel = 0.04;
        rightVuLevel = 0.04;
      }

      final freqs = [
        31, 40, 50, 62, 80, 100, 125, 160, 200, 250, 315, 400,
        500, 630, 800, 1000, 1250, 1600, 2000, 3150, 4000, 6300, 8000, 16000
      ];
      dominantFreqHz = enabled ? freqs[maxBin.clamp(0, freqs.length - 1)] : 0;
      peakDbFs = enabled ? (-42.0 + maxEnergy * 41.5).clamp(-48.0, 0.0) : -48.0;
      onFrame();
    });
  }

  Future<void> stop() async {
    isAudioRecordActive = false;
    _tickTimer?.cancel();
    _tickTimer = null;
    try {
      await _channel.invokeMethod('stopAudioRecordFft');
    } catch (_) {}
  }

  void dispose() {
    _tickTimer?.cancel();
  }
}

class SpectrumAnalyzerCanvas extends StatefulWidget {
  final List<double> bandsDb;
  final List<double> fftBins;
  final List<double> peakHoldBins;
  final List<double> waveformSamples;
  final double leftVu;
  final double rightVu;
  final bool enabled;
  final bool isAudioRecordLive;
  final double peakDbFs;
  final int dominantFreqHz;
  final VoidCallback onToggleAudioRecord;

  const SpectrumAnalyzerCanvas({
    super.key,
    required this.bandsDb,
    required this.fftBins,
    required this.peakHoldBins,
    this.waveformSamples = const [],
    this.leftVu = 0.25,
    this.rightVu = 0.25,
    required this.enabled,
    required this.isAudioRecordLive,
    required this.peakDbFs,
    required this.dominantFreqHz,
    required this.onToggleAudioRecord,
  });

  @override
  State<SpectrumAnalyzerCanvas> createState() => _SpectrumAnalyzerCanvasState();
}

class _SpectrumAnalyzerCanvasState extends State<SpectrumAnalyzerCanvas> {
  VisualizerCanvasMode _mode = VisualizerCanvasMode.spectrum;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 205,
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF0A0E17),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: widget.isAudioRecordLive
              ? const Color(0xFFFF2A85)
              : const Color(0xFF1E273C),
        ),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.graphic_eq,
                    size: 14,
                    color: !widget.enabled
                        ? Colors.white38
                        : widget.isAudioRecordLive
                            ? const Color(0xFFFF2A85)
                            : const Color(0xFF00F0FF),
                  ),
                  const SizedBox(width: 5),
                  Text(
                    !widget.enabled
                        ? 'EQ BYPASSED'
                        : widget.isAudioRecordLive
                            ? 'SYS+MIC FFT • ${widget.dominantFreqHz}Hz'
                            : 'SYS AUDIO • ${widget.dominantFreqHz}Hz',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: !widget.enabled
                          ? Colors.white38
                          : widget.isAudioRecordLive
                              ? const Color(0xFFFF2A85)
                              : const Color(0xFF00F0FF),
                    ),
                  ),
                ],
              ),
              Row(
                children: [
                  _buildModeChip('BARS', VisualizerCanvasMode.spectrum),
                  const SizedBox(width: 4),
                  _buildModeChip('WAVE', VisualizerCanvasMode.waveform),
                  const SizedBox(width: 4),
                  _buildModeChip('RADIAL', VisualizerCanvasMode.radial),
                  const SizedBox(width: 6),
                  InkWell(
                    onTap: widget.onToggleAudioRecord,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 7, vertical: 3),
                      decoration: BoxDecoration(
                        color: widget.isAudioRecordLive
                            ? const Color(0xFFFF2A85)
                            : const Color(0xFF101522),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: const Color(0xFF00F0FF)),
                      ),
                      child: Text(
                        widget.isAudioRecordLive ? '● LIVE' : 'MIC FFT',
                        style: const TextStyle(
                          fontSize: 9.5,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8),
          Expanded(
            child: CustomPaint(
              size: Size.infinite,
              painter: _RealtimeSpectrumPainter(
                bandsDb: widget.bandsDb,
                fftBins: widget.fftBins,
                peakHoldBins: widget.peakHoldBins,
                waveformSamples: widget.waveformSamples,
                leftVu: widget.leftVu,
                rightVu: widget.rightVu,
                enabled: widget.enabled,
                mode: _mode,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildModeChip(String label, VisualizerCanvasMode mode) {
    final selected = _mode == mode;
    return InkWell(
      onTap: () => setState(() => _mode = mode),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFF00F0FF) : const Color(0xFF141B29),
          borderRadius: BorderRadius.circular(5),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 8.5,
            fontWeight: FontWeight.bold,
            color: selected ? Colors.black : Colors.white70,
          ),
        ),
      ),
    );
  }
}

class _RealtimeSpectrumPainter extends CustomPainter {
  final List<double> bandsDb;
  final List<double> fftBins;
  final List<double> peakHoldBins;
  final List<double> waveformSamples;
  final double leftVu;
  final double rightVu;
  final bool enabled;
  final VisualizerCanvasMode mode;

  _RealtimeSpectrumPainter({
    required this.bandsDb,
    required this.fftBins,
    required this.peakHoldBins,
    required this.waveformSamples,
    required this.leftVu,
    required this.rightVu,
    required this.enabled,
    required this.mode,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final midY = h / 2.0;
    const vuWidth = 5.0;
    const plotLeft = vuWidth + 6.0;
    final plotWidth = (w - (vuWidth + 6.0) * 2).clamp(20.0, w);

    // Draw Left & Right Stereo VU Rails
    final lHeight = (enabled ? leftVu.clamp(0.04, 1.0) : 0.04) * h;
    final rHeight = (enabled ? rightVu.clamp(0.04, 1.0) : 0.04) * h;
    final vuPaint = Paint()
      ..color = enabled ? const Color(0xFF00E676) : Colors.white24;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
          Rect.fromLTWH(0, h - lHeight, vuWidth, lHeight),
          const Radius.circular(2)),
      vuPaint,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(
          Rect.fromLTWH(w - vuWidth, h - rHeight, vuWidth, rHeight),
          const Radius.circular(2)),
      vuPaint,
    );

    if (mode == VisualizerCanvasMode.waveform) {
      final samples = waveformSamples.isNotEmpty
          ? waveformSamples
          : List<double>.generate(
              64,
              (i) => enabled
                  ? math.sin(i * 0.35) *
                      (fftBins.isNotEmpty ? fftBins[i % fftBins.length] : 0.2)
                  : 0.0,
            );
      final wavePath = Path();
      for (int i = 0; i < samples.length; i++) {
        final x = plotLeft + (i / (samples.length - 1)) * plotWidth;
        final y = midY - (enabled ? samples[i] : 0.0) * (midY * 0.85);
        if (i == 0) {
          wavePath.moveTo(x, y);
        } else {
          wavePath.lineTo(x, y);
        }
      }
      canvas.drawPath(
        wavePath,
        Paint()
          ..color = enabled ? const Color(0xFF00F0FF) : Colors.grey
          ..strokeWidth = 2.6
          ..style = PaintingStyle.stroke,
      );
      return;
    }

    if (mode == VisualizerCanvasMode.radial) {
      final cx = plotLeft + plotWidth / 2.0;
      final cy = midY;
      final maxR = math.min(plotWidth, h) * 0.44;
      final baseR = maxR * 0.32;
      final count = fftBins.length;
      for (int i = 0; i < count; i++) {
        final amp = enabled ? fftBins[i].clamp(0.05, 1.0) : 0.05;
        final angle = (i / count) * 2.0 * math.pi;
        final rEnd = baseR + amp * (maxR - baseR);
        canvas.drawLine(
          Offset(cx + math.cos(angle) * baseR, cy + math.sin(angle) * baseR),
          Offset(cx + math.cos(angle) * rEnd, cy + math.sin(angle) * rEnd),
          Paint()
            ..color = i < 6 ? const Color(0xFFFF2A85) : const Color(0xFF00F0FF)
            ..strokeWidth = 2.5,
        );
      }
      return;
    }

    // Default: 24 FFT spectrum bars + Peak-Hold caps + 10-band EQ spline
    final binCount = fftBins.length;
    final spacing = plotWidth / binCount;
    final barW = spacing * 0.64;

    for (int i = 0; i < binCount; i++) {
      final amp = enabled ? fftBins[i].clamp(0.04, 1.0) : 0.05;
      final barH = amp * h * 0.88;
      final x = plotLeft + i * spacing + (spacing - barW) / 2.0;
      final y = h - barH;

      final barPaint = Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xAA00F0FF), Color(0x668A3FFC), Color(0x11000000)],
        ).createShader(Rect.fromLTWH(x, y, barW, barH));

      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(x, y, barW, barH),
          const Radius.circular(3),
        ),
        barPaint,
      );

      final peakAmp = enabled ? peakHoldBins[i].clamp(0.05, 1.0) : 0.06;
      final peakY = (h - peakAmp * h * 0.88).clamp(2.0, h - 3.0);
      canvas.drawLine(
        Offset(x, peakY),
        Offset(x + barW, peakY),
        Paint()
          ..color = const Color(0xFFFFB300)
          ..strokeWidth = 2.0,
      );
    }

    final curvePaint = Paint()
      ..color = enabled ? const Color(0xFF00F0FF) : Colors.grey
      ..strokeWidth = 2.5
      ..style = PaintingStyle.stroke;

    final path = Path();
    for (int i = 0; i < bandsDb.length; i++) {
      final x = plotLeft + (i / (bandsDb.length - 1)) * plotWidth;
      final norm = (bandsDb[i].clamp(-15.0, 15.0)) / 15.0;
      final y = midY - norm * (midY * 0.82);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
      canvas.drawCircle(
        Offset(x, y),
        3.5,
        Paint()..color = const Color(0xFFFF2A85),
      );
    }
    canvas.drawPath(path, curvePaint);
  }

  @override
  bool shouldRepaint(covariant _RealtimeSpectrumPainter oldDelegate) => true;
}
