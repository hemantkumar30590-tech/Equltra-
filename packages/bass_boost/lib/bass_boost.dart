library bass_boost;

import 'dart:async';
import 'package:flutter/services.dart';

class BassBoost {
  static const MethodChannel _channel = MethodChannel('com.hemantkumar.equltra/bass_boost');

  static Future<void> init(int audioSessionId) async {
    try {
      await _channel.invokeMethod('init', {'sessionId': audioSessionId});
    } catch (_) {}
  }

  static Future<void> setEnabled(bool enabled) async {
    try {
      await _channel.invokeMethod('setEnabled', {'enabled': enabled});
    } catch (_) {}
  }

  static Future<void> setStrength(int strengthPermille) async {
    try {
      await _channel.invokeMethod('setStrength', {
        'strength': strengthPermille.clamp(0, 1000),
      });
    } catch (_) {}
  }

  static Future<void> release() async {
    try {
      await _channel.invokeMethod('release');
    } catch (_) {}
  }
}
