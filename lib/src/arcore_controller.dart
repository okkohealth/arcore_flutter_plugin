import 'package:flutter/services.dart';

typedef StringResultHandler = void Function(String text);
typedef UnsupportedHandler = void Function(String text);

const UTILS_CHANNEL_NAME = 'arcore_flutter_plugin/utils';

class ArCoreController {
  static checkArCoreAvailability() async {
    final bool arcoreAvailable = await MethodChannel(UTILS_CHANNEL_NAME).invokeMethod('checkArCoreApkAvailability');
    return arcoreAvailable;
  }

  static checkIsArCoreInstalled() async {
    final bool arcoreInstalled = await MethodChannel(UTILS_CHANNEL_NAME).invokeMethod('checkIfARCoreServicesInstalled');
    return arcoreInstalled;
  }

  ArCoreController({
    int id,
    this.enableTapRecognizer,
    this.enablePlaneRenderer,
    this.enableUpdateListener,
  }) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    init();
  }

  final bool enableUpdateListener;
  final bool enableTapRecognizer;
  final bool enablePlaneRenderer;
  MethodChannel _channel;
  StringResultHandler onError;
  StringResultHandler onNodeTap;

  init() async {
    try {
      await _channel.invokeMethod<void>('init', {
        'enableTapRecognizer': enableTapRecognizer,
        'enablePlaneRenderer': enablePlaneRenderer,
        'enableUpdateListener': enableUpdateListener,
      });
    } on PlatformException catch (ex) {
      print("platform exception:${ex.message}");
    }
  }

  Future<dynamic> _handleMethodCalls(MethodCall call) async {
    print('_platformCallHandler call ${call.method} ${call.arguments}');
    switch (call.method) {
      case 'onError':
        if (onError != null) {
          onError(call.arguments);
        }
        break;
      default:
        print('Unknowm method ${call.method} ');
    }
    return Future.value();
  }

  void dispose() {
    _channel?.invokeMethod<void>('dispose');
  }

  void resume() {
    _channel?.invokeMethod<void>('resume');
  }
}
