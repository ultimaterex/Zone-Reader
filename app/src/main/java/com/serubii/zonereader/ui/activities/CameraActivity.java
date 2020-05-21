package com.serubii.zonereader.ui.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.serubii.zonereader.R;
import com.serubii.zonereader.helpers.analyzers.LegacyFirebaseAnalyzer;
import com.bumptech.glide.Glide;
import com.camerakit.CameraKit;
import com.camerakit.CameraKitView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    // Camera kit view doesn't work with butter knife
//    @BindView(R.id.camera)
    CameraKitView cameraKitView;
    ImageView kitGalleryImageView;
    Uri photoURI;
    Bitmap bitmap;

    private int torchID = -1;

    // Camera kit view doesn't work with butter knife
//    @BindView(R.id.kitTorchButton)
    FloatingActionButton kitTorchButton;
    FloatingActionButton kitShutterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide(); //<< this
        setContentView(R.layout.activity_camera);


        cameraKitView = findViewById(R.id.camera);
        kitTorchButton = findViewById(R.id.kitTorchButton);
        kitShutterButton = findViewById(R.id.kitShutterButton);
        kitGalleryImageView = findViewById(R.id.kitGalleryImageView);

        // glide image into carousel
        Glide.with(this).load(R.drawable.ic_view_carousel_white).into(kitGalleryImageView);

        kitGalleryImageView.setOnClickListener(v -> {
            //initiate fragment
//            showResults();
            Snackbar.make(v, "Function not available", Snackbar.LENGTH_LONG).setAction("Action", null).show();


        });

        // setup custom camera
        setupCamera();
        boolean flashSupport = cameraKitView.hasFlash();
        flashFunction();
        Log.i(TAG, "flash support: " + flashSupport);

        kitTorchButton.setOnClickListener(view -> flashFunction());

        kitShutterButton.setOnClickListener(view -> {
            Log.i("TAG", "Starting Shutter");
            takePicture();
            Snackbar.make(view, "Processing Image", Snackbar.LENGTH_LONG).setAction("Action", null).show();

        });
    }

    private void setupCamera() {
        cameraKitView.setPermissions(CameraKitView.PERMISSION_CAMERA);
        cameraKitView.setFocus(CameraKit.FOCUS_AUTO);
        cameraKitView.setPermissions(CameraKitView.PERMISSION_STORAGE);
        cameraKitView.requestPermissions(this);
        cameraKitView.setAdjustViewBounds(true);


    }

    private void flashFunction() {
        if (torchID == 4) {
            torchID = 1;
        } else {
            torchID++;
        }

        // TORCH and AUTO are not supported in this build
        switch (torchID) {
            case (1):
                cameraKitView.setFlash(CameraKit.FLASH_ON);
                kitTorchButton.setImageResource(R.drawable.ic_flash_on_white);
                break;
            case (2):
                cameraKitView.setFlash(CameraKit.FLASH_OFF);
                kitTorchButton.setImageResource(R.drawable.ic_flash_off_white);
                break;
            case (3):
                cameraKitView.setFlash(CameraKit.FLASH_AUTO);
                kitTorchButton.setImageResource(R.drawable.ic_flash_auto_white);
                torchID = 0;
                flashFunction();
                break;
            case (4):
                cameraKitView.setFlash(CameraKit.FLASH_TORCH);
                kitTorchButton.setImageResource(R.drawable.ic_flash_on_red);
                flashFunction();
                torchID = 0;
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void takePicture() {
        cameraKitView.captureImage((cameraKitView, capturedImage) -> {
            bitmap = BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage.length);

            Log.d(TAG, "clicked camera shutter");
            File savedPhoto = resolveImageFile();
            Log.d(TAG, "created file in " + photoURI);


            try {
                FileOutputStream outputStream = new FileOutputStream(savedPhoto.getPath());
                outputStream.write(capturedImage);
                outputStream.close();
                Log.i(TAG, "saved image in file");

                sendToHelper(this, photoURI);


            } catch (java.io.IOException e) {
                e.printStackTrace();
            }


            Glide.with(this).load(photoURI).into(kitGalleryImageView);

        });
    }

    private File createImageFile() throws IOException {
        // create a image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        //save a file: path for use with ACTION_VIEW intents
        return File.createTempFile(
                imageFileName,      /* prefix*/
                ".jpg",      /* suffix*/
                storageDir          /* directory*/
        );
    }

    private File resolveImageFile() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, R.string.errorFileCreate, Toast.LENGTH_LONG).show();
            {
                e.printStackTrace();
            }

        }

        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(
                    CameraActivity.this,
                    "com.serubii.mrz", //(use your app signature + ".provider" )
                    photoFile);
        }
        return photoFile;
    }


    public void sendToHelper(Context context, Uri uri) {
        Log.i(TAG, "Sent to Helper " + uri);

        LegacyFirebaseAnalyzer FA = new LegacyFirebaseAnalyzer();
        FA.initOCR(context, uri);
    }




    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions,
                                           @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
