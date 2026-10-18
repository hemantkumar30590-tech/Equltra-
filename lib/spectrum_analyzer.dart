import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Bridges the Android AudioRecord 1024-point FFT Spectrum Analyzer to Flutter
/// and provides real-time 24-bin spectrum + Peak-Hold visual feedback for EQ adjustments.
class AudioRecordSpectrumController {
  static const MethodChannel _channel =
      MethodChannel('com.hemantkumar.equltra/audiorecord_spectrum');

  Timer? _tickTimer;
  bool isAudioRecordActive = false;
  final List<double> currentBins = List<double>.filled(24, 0.08);
  final List<double> peakHoldBins = List<double>.filled(24, 0.12);
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
    _tickTimer = Timer.periodic(const Duration(milliseconds: 45), (_) async {
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
        final eqPos = (i / 23.0) * 9.0;
        final idx0 = eqPos.floor().clamp(0, 9);
        final idx1 = (idx0 + 1).clamp(0, 9);
        final frac = eqPos - idx0;
        final eqDb = enabled
            ? (bands[idx0] * (1.0 - frac) + bands[idx1] * frac)
            : 0.0;
        final bassBump = (enabled && i < 6) ? (bass / 100.0) * 0.26 : 0.0;
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

      final freqs = [
        31, 40, 50, 62, 80, 100, 125, 160, 200, 250, 315, 400,
        500, 630, 800, 1000, 1250, 1600, 2000, 3150, 4000, 6300, 8000, 16000
      ];
      dominantFreqHz = freqs[maxBin.clamp(0, freqs.length - 1)];
      peakDbFs = (-42.0 + maxEnergy * 41.5).clamp(-48.0, 0.0);
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

class SpectrumAnalyzerCanvas extends StatelessWidget {
  final List<double> bandsDb;
  final List<double> fftBins;
  final List<double> peakHoldBins;
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
    required this.enabled,
    required this.isAudioRecordLive,
    required this.peakDbFs,
    required this.dominantFreqHz,
    required this.onToggleAudioRecord,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 185,
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF0A0E17),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFF1E273C)),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.mic,
                    size: 14,
                    color: isAudioRecordLive
                        ? const Color(0xFFFF2A85)
                        : Colors.white54,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    isAudioRecordLive
                        ? 'AUDIORECORD 1024-FFT • DOM ${dominantFreqHz}Hz'
                        : 'INTERNAL DSP SPECTRUM',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: isAudioRecordLive
                          ? const Color(0xFF00F0FF)
                          : Colors.white70,
                    ),
                  ),
                ],
              ),
              InkWell(
                onTap: onToggleAudioRecord,
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: isAudioRecordLive
                        ? const Color(0xFFFF2A85)
                        : const Color(0xFF101522),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: const Color(0xFF00F0FF)),
                  ),
                  child: Text(
                    isAudioRecordLive ? 'MIC FFT ON' : 'START MIC FFT',
                    style: const TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Expanded(
            child: CustomPaint(
              size: Size.infinite,
              painter: _RealtimeSpectrumPainter(
                bandsDb: bandsDb,
                fftBins: fftBins,
                peakHoldBins: peakHoldBins,
                enabled: enabled,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _RealtimeSpectrumPainter extends CustomPainter {
  final List<double> bandsDb;
  final List<double> fftBins;
  final List<double> peakHoldBins;
  final bool enabled;

  _RealtimeSpectrumPainter({
    required this.bandsDb,
    required this.fftBins,
    required this.peakHoldBins,
    required this.enabled,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final midY = h / 2.0;

    // Draw 24 FFT spectrum bars + Peak-Hold caps
    final binCount = fftBins.length;
    final spacing = w / binCount;
    final barW = spacing * 0.64;

    for (int i = 0; i < binCount; i++) {
      final amp = enabled ? fftBins[i].clamp(0.04, 1.0) : 0.05;
      final barH = amp * h * 0.88;
      final x = i * spacing + (spacing - barW) / 2.0;
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

      // Floating Peak-Hold cap
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

    // Draw 10-band EQ spline response curve on top
    final curvePaint = Paint()
      ..color = enabled ? const Color(0xFF00F0FF) : Colors.grey
      ..strokeWidth = 2.5
      ..style = PaintingStyle.stroke;

    final path = Path();
    for (int i = 0; i < bandsDb.length; i++) {
      final x = (i / (bandsDb.length - 1)) * w;
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
