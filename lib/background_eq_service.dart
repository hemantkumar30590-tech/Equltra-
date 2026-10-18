import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:equalizer/equalizer.dart';
import 'package:bass_boost/bass_boost.dart';
import 'preset_manager.dart';

/// Keeps the Equltra DSP engine and persistent notification running
/// while the user switches to other apps (Spotify, YouTube Music, games, etc.).
class BackgroundEqService with WidgetsBindingObserver {
  static const MethodChannel _channel =
      MethodChannel('com.hemantkumar.equltra/background_service');

  final PresetManager _presetManager;
  Timer? _keepAliveTimer;
  bool _isServiceRunning = false;
  SoundProfile _currentProfile;
  bool _isDspEnabled;

  BackgroundEqService({
    required PresetManager presetManager,
    required SoundProfile initialProfile,
    required bool initialDspEnabled,
  })  : _presetManager = presetManager,
        _currentProfile = initialProfile,
        _isDspEnabled = initialDspEnabled {
    WidgetsBinding.instance.addObserver(this);
    if (_presetManager.isBackgroundModeEnabled()) {
      startBackgroundService(
        profile: _currentProfile,
        dspEnabled: _isDspEnabled,
      );
    }
  }

  bool get isServiceRunning => _isServiceRunning;

  /// Starts the foreground notification service and periodic AudioSession 0 keep-alive.
  Future<void> startBackgroundService({
    required SoundProfile profile,
    required bool dspEnabled,
  }) async {
    _currentProfile = profile;
    _isDspEnabled = dspEnabled;
    _isServiceRunning = true;
    await _presetManager.setBackgroundModeEnabled(true);

    // Notify Android Foreground Service channel (if registered)
    try {
      await _channel.invokeMethod('startForegroundEq', {
        'title': dspEnabled
            ? 'Equltra Background DSP ACTIVE'
            : 'Equltra • DSP Bypassed',
        'subtitle':
            'Bass ${profile.bassBoost.round()}% • Spatial ${profile.spatial.round()}% • Global Session 0',
        'enabled': dspEnabled,
        'preset': profile.name,
      });
    } catch (_) {}

    _startKeepAliveHeartbeat();
  }

  /// Updates the persistent notification and re-applies global audio session parameters.
  Future<void> updateNotificationState({
    required SoundProfile profile,
    required bool dspEnabled,
  }) async {
    _currentProfile = profile;
    _isDspEnabled = dspEnabled;
    if (!_isServiceRunning) return;

    try {
      await _channel.invokeMethod('updateForegroundEq', {
        'title': dspEnabled
            ? 'Equltra Background DSP ACTIVE'
            : 'Equltra • DSP Bypassed',
        'subtitle':
            'Bass ${profile.bassBoost.round()}% • Spatial ${profile.spatial.round()}% • Global Session 0',
        'enabled': dspEnabled,
        'preset': profile.name,
      });
    } catch (_) {}
  }

  /// Stops the background service and dismisses the sticky notification.
  Future<void> stopBackgroundService() async {
    _isServiceRunning = false;
    _keepAliveTimer?.cancel();
    _keepAliveTimer = null;
    await _presetManager.setBackgroundModeEnabled(false);

    try {
      await _channel.invokeMethod('stopForegroundEq');
    } catch (_) {}
  }

  void _startKeepAliveHeartbeat() {
    _keepAliveTimer?.cancel();
    _keepAliveTimer = Timer.periodic(const Duration(seconds: 12), (_) async {
      if (!_isServiceRunning) return;
      await _rebindGlobalSession();
    });
  }

  Future<void> _rebindGlobalSession() async {
    await Equalizer.init(0);
    await BassBoost.init(0);
    await Equalizer.setEnabled(_isDspEnabled);
    await BassBoost.setEnabled(_isDspEnabled);
    await BassBoost.setStrength(
      _isDspEnabled ? (_currentProfile.bassBoost * 10).round() : 0,
    );
    for (int i = 0; i < _currentProfile.bands.length; i++) {
      final db = _isDspEnabled ? _currentProfile.bands[i] : 0.0;
      await Equalizer.setBandLevel(i, (db * 100).round());
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_isServiceRunning) return;
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive ||
        state == AppLifecycleState.hidden) {
      // Re-assert Global Session 0 EQ & BassBoost when user switches to another app
      _rebindGlobalSession();
      updateNotificationState(
        profile: _currentProfile,
        dspEnabled: _isDspEnabled,
      );
    }
  }

  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _keepAliveTimer?.cancel();
  }
}
