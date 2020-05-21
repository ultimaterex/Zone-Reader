package com.serubii.zonereader.helpers.analyzers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.serubii.zonereader.SerubiiApp
import com.serubii.zonereader.helpers.FireXToolkit
import com.serubii.zonereader.helpers.mrz.MRZHelper
import com.serubii.zonereader.helpers.path_MRZ
import com.serubii.zonereader.helpers.preference_code_mrz
import com.serubii.zonereader.ui.activities.ResultsActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** Handles the detection of MRZ for a CameraX ImageAnalysis role
 *
 * @param context Context
 *
 * @property TAG String
 *
 * @property lastAnalyzedTimestamp Used for keeping track of time during detection
 *
 * @property uriString String Saved Image
 * @property croppedUriString String Cropped Saved Image
 *
 * @property stopDetection Boolean Used to stop Detection
 * @property analysisCounter Int Used to count amount of analysis frames performed
 *
 * @property detector FirebaseVisionTextRecognizer
 * @property successListener OnSuccessListener<FirebaseVisionText>
 * @property failureListener OnFailureListener
 * @constructor
 * @author Rex Serubii

 */
class MRZAnalyzer(context: Context) : ImageAnalysis.Analyzer {
    private val TAG = this.javaClass.simpleName

    private var mrzHelper: MRZHelper = MRZHelper(context)


    private var lastAnalyzedTimestamp = 0L
    private var uriString: String = ""
    private var croppedUriString: String = ""

    private var stopDetection = false
    private var analysisCounter = 0

    private val detector = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

    private val successListener = OnSuccessListener<FirebaseVisionText> {


        // TODO: Remove Deprecated usage of method
//        val text_by_line = handleFirebaseResultsLines(it)
        val textByBlock = handleFirebaseResultsBlocks(it)


        // TODO: Deprecate  this for FirebaseResultsHandler
//        for (block in it.textBlocks) {
//            val blockText = block.text
//            Log.wtf("MRZAnalyzer", "Found: $blockText")
//
//
//            for (line in block.lines) {
//                val lineText = line.text
//                text_by_line.add(lineText)
//
//            }

        try {
            stopDetection = true

            // TODO: Remove Deprecated usage of method
//            val infoMap = MH.processData(text_by_line)
            val infoMap = mrzHelper.process(textByBlock)

            if (infoMap?.get("ID_TYPE") != null) {
                Log.wtf("MRZAnalyzer", "MRZ analysis Success.")

                Log.i(TAG, "infomap $infoMap")

                for (key in infoMap.keys) {
                    Log.i("Hashmap Debug", key + " = \"" + infoMap[key] + "\"")
                }

                val bundle: Bundle = processData(infoMap, Uri.parse("uriString"))


                val intent = Intent(context, ResultsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtras(bundle)
                stopDetection = true

                context.startActivity(intent)
                (context as Activity).finish()

            } else {
                stopDetection = false
                Log.e(TAG, "InvalidMRZHandlerException: Can't Process this dataset")
                throw IllegalStateException("InvalidMRZHandlerException")
            }


        } catch (e: Exception) {
            stopDetection = false
        }


    }


    private val failureListener = OnFailureListener { e ->
        Log.wtf("FaceAnalyzer", "Face analysis failure.", e)
        stopDetection = false
    }

    /** This gets called every time a frame is being analyzed
     *
     * @param image ImageProxy
     * @param rotationDegrees Int
     */
    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (image == null) return
        image.image ?: return
        // skip first cycle
        val currentTimestamp = System.currentTimeMillis()

        if (!stopDetection) {
            if (currentTimestamp - lastAnalyzedTimestamp >=
                    TimeUnit.SECONDS.toMillis(2)
            ) {

                analysisCounter++
                lastAnalyzedTimestamp = currentTimestamp
                if (analysisCounter > 2) {

                    Log.d(TAG, "analysisCounter: $analysisCounter")
                    val tk = FireXToolkit()
                    tk.deleteEnv(
                            SerubiiApp.applicationContext(),
                            path_MRZ
                    )



                    Log.w(TAG, "Of type: $rotationDegrees")
                    val rawBitmap = image.toBitmap()
                    val imageExposed = tk.rotateBitmap(rawBitmap, rotationDegrees)

                    val uri = tk.saveBitmapToTemporaryFile(
                            imageExposed,
                            preference_code_mrz,
                            SerubiiApp.applicationContext(),
                            path_MRZ
                    )
                    uriString = uri.toString()
//                    Log.wtf(
//                        TAG, "________________________\n" +
//                                "Saving profile image as $uri\n" +
//                                "________________________"
//                    )

                    val firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageExposed)

                    var result = detector.processImage(firebaseVisionImage)
                            .addOnSuccessListener(successListener)

                            .addOnFailureListener(failureListener)



                }
            } else {
                return
            }
        }


    }

    /** Extension Method that converts a [ImageProxy] to a Bitmap
     *
     * @receiver ImageProxy
     * @return Bitmap
     */
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /** This takes a Hashmap and puts it's contents into a bundle
     *
     * @param info HashMap<String, String>
     * @param uri Uri
     * @return Bundle
     */
    private fun processData(
            info: HashMap<String, String>,
            uri: Uri
    ): Bundle { // send data through a bundle
        val bundle = Bundle()
        bundle.putString("ID_TYPE", info["ID_TYPE"])
        bundle.putString("ISSUE_COUNTRY", info["ISSUE_COUNTRY"])
        bundle.putString("DOCUMENT_NUMBER", info["DOCUMENT_NUMBER"])
        bundle.putString("OPTIONAL_INFO", info["OPTIONAL_INFO"])
        bundle.putString("DOB", info["DOB"])
        bundle.putString("GENDER", info["GENDER"])
        bundle.putString("EXPIRATION", info["EXPIRATION"])
        bundle.putString("NATIONALITY", info["NATIONALITY"])
        bundle.putString("SURNAME", info["SURNAME"])
        bundle.putString("GIVEN_NAME", info["GIVEN_NAME"])
        bundle.putString("INITIAL", uri.toString())
        bundle.putString("FRONT", "")
        bundle.putString("BACK", "")
        bundle.putString("MRZ", info["MRZ"])
        for (key in bundle.keySet()) {
            Log.d("Bundle Debug to send", key + " = \"" + bundle[key] + "\"")
        }
        return bundle
    }


    /**
     * This handles firebase results and returns individual lines
     *
     * @param result
     * @return
     */
    @Deprecated(
            message = "deprecated with lines implementation to support block based extraction",
            replaceWith = ReplaceWith("handleFirebaseResultsBlocks"),
            level = DeprecationLevel.WARNING)
    private fun handleFirebaseResultsLines(result: FirebaseVisionText): ArrayList<String> {
        val textByLine = ArrayList<String>()
        textByLine.clear()

        for (block in result.textBlocks) {
            val blockText = block.text
            Log.wtf(TAG, "blockText :$blockText" + "" + "\nsize: " + blockText.length)
            for (line in block.lines) {
                val lineText = line.text
//                Log.wtf(TAG, "Linetext:$lineText")
                textByLine.add(lineText)
            }
        }
        return textByLine
    }

    /**     * This handles firebase results and returns blocks
     *
     * @param result FirebaseVisionText
     * @return ArrayList<String>
     */
    private fun handleFirebaseResultsBlocks(result: FirebaseVisionText): ArrayList<String> {
        val textByBlock = ArrayList<String>()
        textByBlock.clear()

        for (block in result.textBlocks) {
            val blockText = block.text
            Log.wtf(TAG, "blockText :$blockText" + "" + "\nsize: " + blockText.length)
            textByBlock.add(blockText)
        }
        return textByBlock
    }

}
