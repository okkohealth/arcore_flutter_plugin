package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class ArCoreFaceView(activity:Activity,context: Context, messenger: BinaryMessenger, id: Int) : BaseArCoreView(activity, context, messenger, id) {

    private val TAG: String = ArCoreFaceView::class.java.name
    private var faceRegionsRenderable: ModelRenderable? = null
    private var faceMeshTexture: Texture? = null
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()
    private var faceSceneUpdateListener: Scene.OnUpdateListener

    private var translationX = 0.0f
    private var translationY = 0.0f
    private var translationZ = 0.0f
    private var rotationX = 0.0f
    private var rotationY = 0.0f
    private var rotationZ = 0.0f
    private var cameraX = 0.0f
    private var cameraY = 0.0f
    private var cameraZ = 0.0f

    init {
        faceSceneUpdateListener = Scene.OnUpdateListener { frameTime ->
            run {

                val frame = arSceneView?.getArFrame()
                val cameraPose = frame?.getAndroidSensorPose()
                cameraPose?.let {
                    cameraX = cameraPose.tx()
                    cameraY = cameraPose.ty()
                    cameraZ = cameraPose.tz()
                }


                val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)

                faceList?.let {
                    // Make new AugmentedFaceNodes for any new faces.
                    for (face in faceList) {
                        val center = face.getCenterPose()
                        translationX = center.tx()//  buffer.get(435)
                        translationY = center.ty()//  buffer.get(436)
                        translationZ = center.tz()//  buffer.get(437)
                        val eulers = quaternionToEuler(center.qw(),center.qx(),center.qy(),center.qz());
                        rotationX = eulers[0]
                        rotationY = eulers[1]
                        rotationZ = eulers[2]
                        if (!faceNodeMap.containsKey(face)) {
                            val faceNode = AugmentedFaceNode(face)
                            faceNode.setParent(arSceneView?.scene)
                            faceNode.faceRegionsRenderable = faceRegionsRenderable
                            faceNode.faceMeshTexture = faceMeshTexture
                            faceNodeMap[face] = faceNode
                        }
                    }

                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    val iter = faceNodeMap.iterator()
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        val face = entry.key
                        if (face.trackingState == TrackingState.STOPPED) {
                            val faceNode = entry.value
                            faceNode.setParent(null)
                            iter.remove()
                        }
                    }
                }
            }
        }
    }


    fun quaternionToEuler(qw: Float, qx: Float, qy: Float, qz: Float): FloatArray {
        val x:Float
        val y:Float
        val z:Float

        // roll (x-axis rotation)
        val sinr_cosp = 2 * (qw * qx + qy * qz);
        val cosr_cosp = 1 - 2 * (qx * qx + qy * qy);
        x = kotlin.math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        val sinp = 2 * (qw * qy - qz * qx);
        if (kotlin.math.abs(sinp) >= 1)
            y = java.lang.Math.copySign(kotlin.math.PI / 2, sinp.toDouble()).toFloat(); // use 90 degrees if out of range
        else
            y = kotlin.math.asin(sinp);

        // yaw (z-axis rotation)
        val siny_cosp = 2 * (qw * qz + qx * qy);
        val cosy_cosp = 1 - 2 * (qy * qy + qz * qz);
        z = kotlin.math.atan2(siny_cosp, cosy_cosp);

        return floatArrayOf(x,y,z)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if(isSupportedDevice){
            when (call.method) {
                "init" -> {
                    arScenViewInit(call, result)
                }
                "loadMesh" -> {
                    val map = call.arguments as HashMap<*, *>
                    val textureBytes = map["textureBytes"] as ByteArray
                    val skin3DModelFilename = map["skin3DModelFilename"] as? String
                    loadMesh(textureBytes, skin3DModelFilename)
                }
                "getNodePosition" -> {
                    result.success("{\"x\":$translationX,\"y\":$translationY,\"z\":$translationZ,\"rx\":$rotationX,\"ry\":$rotationY,\"rz\":$rotationZ}")
                }
                "dispose" -> {
                    Log.i(TAG, " updateMaterials")
                    dispose()
                }
                else -> {
                    result.notImplemented()
                }
            }
        }else{
            Log.i(TAG,"Impossible call " + call.method + " method on unsupported device")
            result.error("Unsupported Device","",null)
        }
    }

    fun loadMesh(textureBytes: ByteArray?, skin3DModelFilename: String?) {
        if (skin3DModelFilename != null) {
            // Load the face regions renderable.
            // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
            ModelRenderable.builder()
                    .setSource(activity, Uri.parse(skin3DModelFilename))
                    .build()
                    .thenAccept { modelRenderable ->
                        faceRegionsRenderable = modelRenderable
                        modelRenderable.isShadowCaster = false
                        modelRenderable.isShadowReceiver = false
                    }
        }

        // Load the face mesh texture.
        Texture.builder()
                //.setSource(activity, Uri.parse("fox_face_mesh_texture.png"))
                .setSource(BitmapFactory.decodeByteArray(textureBytes, 0, textureBytes!!.size))
                .build()
                .thenAccept { texture -> faceMeshTexture = texture }
    }

    private fun arScenViewInit(call: MethodCall, result: MethodChannel.Result) {
        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        arSceneView?.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST
        arSceneView?.scene?.addOnUpdateListener(faceSceneUpdateListener)

        result.success(null)
    }

    override fun onResume() {
        if (arSceneView == null) {
            return
        }

        if (arSceneView?.session == null) {

            // request camera permission if not already requested
            if (!ArCoreUtils.hasCameraPermission(activity)) {
                ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
            }

            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = ArCoreUtils.createArSession(activity, installRequested, true)
                if (session == null) {
                    installRequested = false
                    return
                } else {
                    val config = Config(session)
                    config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    session.configure(config)
                    arSceneView?.setupSession(session)
                }
            } catch (e: UnavailableException) {
                ArCoreUtils.handleSessionException(activity, e)
            }
        }

        try {
            arSceneView?.resume()
        } catch (ex: CameraNotAvailableException) {
            ArCoreUtils.displayError(activity, "Unable to get camera", ex)
            activity.finish()
            return
        }

    }

    override fun onDestroy() {
        arSceneView?.scene?.removeOnUpdateListener(faceSceneUpdateListener)
        super.onDestroy()
    }

}