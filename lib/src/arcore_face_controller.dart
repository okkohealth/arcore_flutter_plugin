import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

import '../arcore_flutter_plugin.dart';

class ArCoreFaceController {
  ArCoreFaceController({
    int id,
  }) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    init();
  }

  MethodChannel _channel;
  StringResultHandler onError;

  init() async {
    try {
      await _channel.invokeMethod<void>('init');
    } on PlatformException catch (ex) {
      print(ex.message);
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

  Future<void> loadMesh({@required Uint8List textureBytes, String skin3DModelFilename}) {
    assert(textureBytes != null);
    return _channel
        .invokeMethod('loadMesh', {'textureBytes': textureBytes, 'skin3DModelFilename': skin3DModelFilename});
  }

  Future<dynamic> getNodePosition() {
    return _channel.invokeMethod('getNodePosition');
  }

  void dispose() {
    _channel?.invokeMethod<void>('dispose');
  }
}
