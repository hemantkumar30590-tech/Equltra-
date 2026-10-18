library equalizer;

import 'dart:async';
import 'package:flutter/services.dart';

class Equalizer {
  static const MethodChannel _channel = MethodChannel('com.hemantkumar.equltra/equalizer');

  static Future<void> init(int audioSessionId) async {
    try {
      await _channel.invokeMethod('init', {'sessionId': audioSessionId});
    } catch (_) {}
  }

  static Future<void> setEnabled(bool enabled) async {
    try {
      await _channel.invokeMethod('enable', {'enabled': enabled});
    } catch (_) {}
  }

  static Future<void> setBandLevel(int bandId, int levelMillibels) async {
    try {
      await _channel.invokeMethod('setBandLevel', {
        'bandId': bandId,
        'level': levelMillibels,
      });
    } catch (_) {}
  }

  static Future<void> setPreset(String presetName) async {
    try {
      await _channel.invokeMethod('setPreset', {'presetName': presetName});
    } catch (_) {}
  }

  static Future<void> openDeviceEqualizer(int audioSessionId) async {
    try {
      await _channel.invokeMethod('openDeviceEqualizer', {'sessionId': audioSessionId});
    } catch (_) {}
  }

  static Future<void> release() async {
    try {
      await _channel.invokeMethod('release');
    } catch (_) {}
  }
}
