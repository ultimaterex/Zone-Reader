package com.serubii.zonereader.helpers.analyzers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.serubii.zonereader.helpers.mrz.LegacyMRZHelper;
import com.serubii.zonereader.ui.activities.ResultsActivity;
import com.serubii.zonereader.ui.activities.CameraActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class LegacyFirebaseAnalyzer {
    private ArrayList<String> text_by_line = new ArrayList<>();
    private final String TAG = this.getClass().getSimpleName();

    // TODO Adjust method for 1(native) or 2(kitkam)
    private final int METHOD = 1;


    public void initOCR(Context context, Uri uri) {
//        ArrayList<String> to_send = new ArrayList<>();
        FirebaseVisionImage image = null;
        try {
            image = FirebaseVisionImage.fromFilePath(context, uri);
        } catch (
                IOException e) {
            e.printStackTrace();
        }

        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result =
                detector.processImage(Objects.requireNonNull(image))
                        .addOnSuccessListener(firebaseVisionText -> {
                            // Task completed successfully
                            // ...
                            Log.wtf(TAG, "FIREBASE OCR SUCCESS");

                            text_by_line = FirebaseResults(firebaseVisionText);

                            LegacyMRZHelper MH = new LegacyMRZHelper();

                            HashMap<String, String> infoMap;

                            try {
                                infoMap = MH.processData(text_by_line);
                                if (infoMap.get("ID_TYPE") != null) {
                                    Log.i(TAG, "infomap " + infoMap.toString());

                                    for (String key : infoMap.keySet()) {
                                        Log.i("Hashmap Debug", key + " = \"" + infoMap.get(key) + "\"");
                                    }

                                    Bundle bundle = processData(infoMap, uri);

                                    for (String key : bundle.keySet()) {
                                        Log.i("Hashmap decode", key + " = \"" + bundle.get(key) + "\"");
                                    }
//                            ca.showResults(context, bundle);


                                    Intent intent = new Intent(context, ResultsActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtras(bundle);

                                    context.startActivity(intent);
                                    ((Activity) context).finish();

                                } else {
                                    switch (METHOD) {

                                        case 1:

                                            Toast toast = Toast.makeText(context, "Image Processing Failed, Please Try again", Toast.LENGTH_LONG);
                                            toast.show();
                                            break;
                                        case 2:
                                            AlertDialog.Builder kbuilder = new AlertDialog.Builder(context);
                                            // create button to restart camera activity
                                            kbuilder.setMessage("We're having trouble processing your image, do you want to try again?")
                                                    .setCancelable(false)
                                                    .setPositiveButton("OK", (dialog, id) -> {
                                                        //do things
                                                        Intent intent = new Intent(context, CameraActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        context.startActivity(intent);

                                                    });
                                            AlertDialog kAlert = kbuilder.create();
                                            kAlert.show();
                                            break;
                                        default:
                                            throw new RuntimeException("BAD IMPLEMENT");

                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    // ...
                                    Log.w(TAG, "FIREBASE FAILED");
                                });
    }

    private ArrayList<String> FirebaseResults(FirebaseVisionText result) {
//        String resultText = result.getText();
        text_by_line.clear();
        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
            String blockText = block.getText();
            Log.wtf(TAG, "blockText :" + blockText);
//            Float blockConfidence = block.getConfidence();
//            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
//            Log.wtf(TAG, "blockLanguages :" + blockLanguages.toString());
//            Point[] blockCornerPoints = block.getCornerPoints();
//            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionText.Line line : block.getLines()) {
                String lineText = line.getText();
                Log.wtf(TAG, "Linetext:" + lineText);
                text_by_line.add(lineText);
//                Float lineConfidence = line.getConfidence();
//                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
//                Point[] lineCornerPoints = line.getCornerPoints();
//                Rect lineFrame = line.getBoundingBox();
//                for (FirebaseVisionText.Element element : line.getElements()) {
//                    String elementText = element.getText();
//                    Float elementConfidence = element.getConfidence();
//                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
//                    Point[] elementCornerPoints = element.getCornerPoints();
//                    Rect elementFrame = element.getBoundingBox();
//                }
            }
        }
        return text_by_line;
    }

    private Bundle processData(HashMap<String, String> info, Uri uri) {
        // send data through a bundle
        Bundle bundle = new Bundle();
        bundle.putString("ID_TYPE", info.get("ID_TYPE"));
        bundle.putString("ISSUE_COUNTRY", info.get("ISSUE_COUNTRY"));
        bundle.putString("DOCUMENT_NUMBER", info.get("DOCUMENT_NUMBER"));
        bundle.putString("OPTIONAL_INFO", info.get("OPTIONAL_INFO"));
        bundle.putString("DOB", info.get("DOB"));
        bundle.putString("GENDER", info.get("GENDER"));
        bundle.putString("EXPIRATION", info.get("EXPIRATION"));
        bundle.putString("NATIONALITY", info.get("NATIONALITY"));
        bundle.putString("SURNAME", info.get("SURNAME"));
        bundle.putString("GIVEN_NAME", info.get("GIVEN_NAME"));
        bundle.putString("INITIAL", uri.toString());
        bundle.putString("FRONT", "");
        bundle.putString("BACK", "");


        for (String key : bundle.keySet()) {
            Log.d("Bundle Debug to send", key + " = \"" + bundle.get(key) + "\"");
        }

        return bundle;
    }
}
