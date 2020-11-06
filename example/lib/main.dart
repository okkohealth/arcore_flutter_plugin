import 'app.dart';
import 'package:flutter/material.dart';
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart' show ArCoreController;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  print('ARCORE IS AVAILABLE?');
  print("available: ${await ArCoreController.checkArCoreAvailability()}");
  print('AR SERVICES INSTALLED?');
  print("isntalled: ${await ArCoreController.checkIsArCoreInstalled()}");
  runApp(App());
}
