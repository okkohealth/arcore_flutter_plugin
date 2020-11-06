import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';

class AugmentedFacesScreen extends StatefulWidget {
  const AugmentedFacesScreen({Key key}) : super(key: key);

  @override
  _AugmentedFacesScreenState createState() => _AugmentedFacesScreenState();
}

class _AugmentedFacesScreenState extends State<AugmentedFacesScreen> {
  ArCoreFaceController arCoreFaceController;
  Timer blinkTimer;
  Map<String, dynamic> left = {};
  double distance = 0;
  @override
  void initState() {
    blinkTimer = Timer.periodic(Duration(milliseconds: 10), getNodePosition);
    super.initState();
  }

  void getNodePosition(Timer timer) async {
    String nodePosition = await arCoreFaceController.getNodePosition();
    setState(() {
      left = jsonDecode(nodePosition);
      distance = (sqrt(pow(left["x"], 2) + pow(left["y"], 2) + pow(left["z"], 2)) * 100) - 5;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Augmented Faces'),
        ),
        body: Stack(
          fit: StackFit.expand,
          children: [
            ArCoreFaceView(
              onArCoreViewCreated: _onArCoreViewCreated,
              enableAugmentedFaces: true,
            ),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: left.entries
                  .map((e) => Container(padding: EdgeInsets.all(10), color: Colors.white, child: Text("$e")))
                  .toList()
                  .followedBy(
                      [Container(padding: EdgeInsets.all(10), color: Colors.white, child: Text("$distance"))]).toList(),
            ),
          ],
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreFaceController controller) {
    arCoreFaceController = controller;
  }

  @override
  void dispose() {
    arCoreFaceController.dispose();
    blinkTimer?.cancel();
    super.dispose();
  }
}
