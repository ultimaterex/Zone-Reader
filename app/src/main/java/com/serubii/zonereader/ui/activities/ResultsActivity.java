package com.serubii.zonereader.ui.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.serubii.zonereader.R;
import com.serubii.zonereader.helpers.animation.ViewAnimation;
import com.serubii.zonereader.room.entities.Document;
import com.serubii.zonereader.room.viewmodels.DocumentViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_dob;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_document_number;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_expiration;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_gender;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_given_name;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_id_type;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_issue_country;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_nationality;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_optional_info;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_photo_string;
import static com.serubii.zonereader.helpers.ConstantsKt.preference_code_surname;


public class ResultsActivity extends AppCompatActivity {

    private Uri CSV_URI;
    private Uri PDF_URI;
    Uri xu = null;


    private static final int REQUEST_IMAGE_CAPTURE = 420;
    private static final int REQUEST_IMAGE_FAILURE = 0;

    Uri FRONT = null, BACK = null, INITIAL = null;
    String FRONT_STRING = null, BACK_STRING = null, INITIAL_STRING = null;
    String ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER, OPTIONAL_INFO;
    String DOB, GENDER, EXPIRATION, NATIONALITY;
    String SURNAME, GIVEN_NAME;
    Long SCAN_TIME;

    TextView rID_TYPE;
    TextView rSURNAME;
    TextView rGIVEN_NAME;
    TextView rDOC_NUMBER;
    TextView rISSUE_COUNTRY;
    TextView rNATIONALITY;
    TextView rDOB;
    TextView rGENDER;
    //    TextView rISSUE_DATE;
    TextView rEXP_DATE;
    TextView rOPTIONAL_INFO;

    FloatingActionButton rFAB, rFABSave, rFABShare;


    private boolean isRotate = false;

    private String TAG = this.getClass().getSimpleName();
    private String outputCSVString;
    private boolean edit_mode = false;
    private Integer ID;

    private DocumentViewModel viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide(); //<< this
        //Remove notification bar
        setContentView(R.layout.activity_results);

        setupElements();

        viewModel = ViewModelProviders.of(this).get(DocumentViewModel.class);


        // get edit info
        Intent intent = getIntent();
        if (intent != null) {
            Log.i(TAG, "onCreate: " + intent.getIntExtra("id", -1));

            if (intent.hasExtra("id")) {
                ID = intent.getIntExtra("id", -1);
                ID = intent.getIntExtra("id", -1);
            }
            if (intent.hasExtra("editMode")) {
                Log.i(TAG, "onCreate: " + intent.getBooleanExtra("editMode", false));
                edit_mode = intent.getBooleanExtra("editMode", false);
            }
        }


        // get data from bundle
        try {
            Bundle bundle = getIntent().getExtras();
            // log bundle data
            for (String key : Objects.requireNonNull(bundle).keySet()) {
                Log.i("Bundle Debug", key + " = \"" + bundle.get(key) + "\"");
            }
            populateText(bundle);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
        }

//        Glide.with(getApplicationContext())
//                .load(uri)
//                .listener()
//                .bitmapTransform(CropTransformation(view.context))
//                .into()


        // button code

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ViewAnimation.init(rFABSave);
            ViewAnimation.init(rFABShare);
        }, 50);

        rFAB.setOnClickListener(v -> {
            isRotate = ViewAnimation.rotateFAB(v, !isRotate);
            if (isRotate) {
                ViewAnimation.showIn(rFABSave);
                ViewAnimation.showIn(rFABShare);
            } else {
                ViewAnimation.showOut(rFABSave);
                ViewAnimation.showOut(rFABShare);
            }
        });

        rFABShare.setOnClickListener(v -> shareHandler());

        rFABSave.setOnClickListener(v -> {
            saveHandler();
            Snackbar.make(v, "Saving Data", Snackbar.LENGTH_LONG).setAction("Action", null).show();

        });


    }

    private void setupElements() {
        // talk about boilerplate, time for view binding?
        rID_TYPE = findViewById(R.id.resultsDocumentTypeText);
        rSURNAME = findViewById(R.id.resultsSurnameInput);

        rGIVEN_NAME = findViewById(R.id.resultsGNameInput);
        rDOC_NUMBER = findViewById(R.id.resultsDocumentNumberInput);
        rISSUE_COUNTRY = findViewById(R.id.resultsIssueCountryInput);

        rNATIONALITY = findViewById(R.id.resultsNationalityInput);
        rDOB = findViewById(R.id.resultsDateOfBirthInput);
        rGENDER = findViewById(R.id.resultsSexInput);

//        rISSUE_DATE = findViewById(R.id.resultsEstimatedIssueDateInput);
        rEXP_DATE = findViewById(R.id.resultsExpirationDateInput);
        rOPTIONAL_INFO = findViewById(R.id.resultsOptionalValuesInput);


        rFAB = findViewById(R.id.resultsFAB);
        rFABSave = findViewById(R.id.resultsFABSave);
        rFABShare = findViewById(R.id.resultFABShare);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE: {
                if (resultCode == RESULT_OK) {
                    Bitmap bm = null;
                    try {
                        InputStream is = this.getContentResolver().openInputStream(xu);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        bm = BitmapFactory.decodeStream(is, null, options);

                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
                    }

                    if (bm == null) {
                        Log.w(TAG, "BM WAS NULL");
                    } else {
                        Log.i(TAG, "BM IS " + bm);
                    }
                    Log.i(TAG, "BM IS " + xu.getPath());


                }
            }
            case REQUEST_IMAGE_FAILURE: {
//                throw new IllegalStateException("Unexpected value: " + requestCode);
                Log.i(TAG, "Closed camera without taking picture");

            }
        }
    }

    private Uri startIntentPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Toast.makeText(ResultsActivity.this, R.string.errorFileCreate, Toast.LENGTH_LONG).show();
                {
                    Log.e(TAG, "Error writing to storage" + e.getMessage());
                }

            }

            if (photoFile != null) {
                xu = FileProvider.getUriForFile(
                        ResultsActivity.this,
                        "com.serubii.mrz", //(use your app signature + ".provider" )
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, xu);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            }
        }
        return xu;
    }

    private File createImageFile() throws IOException {
        // create a image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                imageFileName,      /* prefix*/
                ".jpg",      /* suffix*/
                storageDir          /* directory*/
        );
    }


    private void populateText(Bundle bundle) {
        ID_TYPE = ISSUE_COUNTRY = DOCUMENT_NUMBER = OPTIONAL_INFO = " ";
        DOB = GENDER = EXPIRATION = NATIONALITY = " ";
        SURNAME = GIVEN_NAME = " ";

        ID_TYPE = bundle.getString(preference_code_id_type);
        ISSUE_COUNTRY = bundle.getString(preference_code_issue_country);
        DOCUMENT_NUMBER = bundle.getString(preference_code_document_number);
        OPTIONAL_INFO = bundle.getString(preference_code_optional_info);
        DOB = bundle.getString(preference_code_dob);
        GENDER = bundle.getString(preference_code_gender);
        EXPIRATION = bundle.getString(preference_code_expiration);
        NATIONALITY = bundle.getString(preference_code_nationality);
        SURNAME = bundle.getString(preference_code_surname);
        GIVEN_NAME = bundle.getString(preference_code_given_name);
        INITIAL = Uri.parse(bundle.getString(preference_code_photo_string));

        // bundle returns empty for these values when coming from camera view, only try to populate when edit_mode is active
        if (edit_mode) {
            try {
                FRONT = Uri.parse(bundle.getString("FRONT"));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load Front Image, " + "\n" + e.getMessage());
            }
            try {
                BACK = Uri.parse(bundle.getString("BACK"));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load Back Image, " + "\n" + e.getMessage());
            }
        }


        Log.i("FINAL", ID_TYPE + ", " + ISSUE_COUNTRY + ", " + DOCUMENT_NUMBER + ", " + OPTIONAL_INFO + ", "
                + DOB + ", " + GENDER + ", " + EXPIRATION + ", " + NATIONALITY + ", " + SURNAME + ", " + GIVEN_NAME + ", "
                + INITIAL + ", " + FRONT + ", " + BACK);

        outputCSVString = ID_TYPE + ", " + ISSUE_COUNTRY + ", " + DOCUMENT_NUMBER + ", " + OPTIONAL_INFO + ", "
                + DOB + ", " + GENDER + ", " + EXPIRATION + ", " + NATIONALITY + ", " + SURNAME + ", " + GIVEN_NAME;


        if (DOB != null) {
            if (DOB.length() == 6) {
                DOB = DOB.substring(0, 2) + "-" + DOB.substring(2, 4) + "-" + DOB.substring(4, 6);
                Log.i(TAG, "Successfully split DOB into valid date format");
            }
        }
        if (EXPIRATION != null) {
            if (EXPIRATION.length() == 6) {
                EXPIRATION = EXPIRATION.substring(0, 2) + "-" + EXPIRATION.substring(2, 4) + "-" + EXPIRATION.substring(4, 6);
                Log.i(TAG, "Successfully split Expiration into valid date format");
            }
        }


        rID_TYPE.setText(ID_TYPE);
        rSURNAME.setText(SURNAME);
        rGIVEN_NAME.setText(GIVEN_NAME);
        rDOC_NUMBER.setText(DOCUMENT_NUMBER);
        rISSUE_COUNTRY.setText(ISSUE_COUNTRY);
        rNATIONALITY.setText(NATIONALITY);
        rDOB.setText(DOB);
        rGENDER.setText(GENDER);
//        rISSUE_DATE.setText(""); // TODO schedule for removal
        rEXP_DATE.setText(EXPIRATION);
        rOPTIONAL_INFO.setText(OPTIONAL_INFO);


    }

    private void saveHandler() {
        saveDocument();
    }

    private void shareHandler() {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(ResultsActivity.this);
        builder.setTitle("What do you want to share as?");

        // add a list
        String[] formats = {"PDF", "CSV", "JSON"};
        builder.setItems(formats, (dialog, which) -> {
            switch (which) {
                case 0: // PDF
                    File pdf_file = resolvePDF();
                    PDF(pdf_file);

                    Intent pdfShareIntent = new Intent("com.serubii.mrz.ACTION_RETURN_FILE");
                    Log.i(TAG, "PDF:" + PDF_URI);

                    String pdf_mime = getMimeType(PDF_URI);
                    Log.i(TAG, "Mime: " + pdf_mime);
                    try {
                        pdfShareIntent.setAction(Intent.ACTION_SEND);
                        pdfShareIntent.putExtra(Intent.EXTRA_STREAM, PDF_URI);
                        pdfShareIntent.setType(pdf_mime);
                        startActivity(Intent.createChooser(pdfShareIntent, "Share PDF"));
                    } catch (Exception e) {
                        Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
                    }

                    break;
                case 1: // CSV
                    File csv_file = resolveCSV();
                    writeToFile(outputCSVString, csv_file);

                    Intent csvShareIntent = new Intent("com.serubii.mrz.ACTION_RETURN_FILE");
                    Log.i(TAG, "CSV:" + CSV_URI);

                    String csv_mime = getMimeType(CSV_URI);
                    Log.i(TAG, "Mime: " + csv_mime);
                    try {
                        csvShareIntent.setAction(Intent.ACTION_SEND);
                        csvShareIntent.putExtra(Intent.EXTRA_STREAM, CSV_URI);
                        csvShareIntent.setType(csv_mime);
                        startActivity(Intent.createChooser(csvShareIntent, "Share CSV"));
                    } catch (Exception e) {
                        Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
                    }
                    break;
                case 2: // JSON

                    // send in json formatting not as file
                    Intent jsonShareIntent = new Intent();


                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("ID_TYPE", ID_TYPE);
                        jsonObject.put("ISSUE_COUNTRY", ISSUE_COUNTRY);
                        jsonObject.put("DOCUMENT_NUMBER", DOCUMENT_NUMBER);
                        jsonObject.put("SURNAME", SURNAME);
                        jsonObject.put("GIVEN_NAME", GIVEN_NAME);
                        jsonObject.put("GENDER", GENDER);
                        jsonObject.put("DOB", DOB);
                        jsonObject.put("NATIONALITY", NATIONALITY);
                        jsonObject.put("EXPIRATION", EXPIRATION);
                        jsonObject.put("OPTIONAL_INFO ", OPTIONAL_INFO);


                    } catch (JSONException e) {
                        Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
                    }


                    String jsonString = jsonObject.toString();
                    Log.i(TAG, "json: " + jsonString);

                    Log.i(TAG, "Mime: " + "text/plain");
                    try {
                        jsonShareIntent.setAction(Intent.ACTION_SEND);
                        jsonShareIntent.putExtra(Intent.EXTRA_TEXT, jsonString);
                        jsonShareIntent.setType("text/plain");
                        startActivity(Intent.createChooser(jsonShareIntent, "Share JSON Text"));
                    } catch (Exception e) {
                        Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
                    }
                    break;

            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void writeToFile(String data, File file) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(data);
            writer.flush();
            writer.close();
            Log.i(TAG, "Saved Content to CSV");
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.getMessage());
        }
    }

    private File resolveCSV() {
        File CSV_File = null;
        try {
            CSV_File = createCSV_File();
        } catch (IOException e) {
            Toast.makeText(this, R.string.errorFileCreate, Toast.LENGTH_LONG).show();
            {
                Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
            }

        }

        if (CSV_File != null) {
            CSV_URI = FileProvider.getUriForFile(
                    this,
                    "com.serubii.mrz", //(use your app signature + ".provider" )
                    CSV_File);
        }
        return CSV_File;
    }

    private File createCSV_File() throws IOException {
        // create a image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(new Date());
        String FileName = "CSV_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        //save a file: path for use with ACTION_VIEW intents
        return File.createTempFile(
                FileName,      /* prefix*/
                ".csv",      /* suffix*/
                storageDir          /* directory*/
        );
    }

    public String getMimeType(Uri uri) {
        String mimeType;
        if (Objects.equals(uri.getScheme(), ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    private File createPDF_File() throws IOException {
        // create a image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(new Date());
        String FileName = "PDF_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        return File.createTempFile(
                FileName,      /* prefix*/
                ".pdf",      /* suffix*/
                storageDir          /* directory*/
        );
    }

    private File resolvePDF() {
        File PDF_file = null;
        try {
            PDF_file = createPDF_File();
        } catch (IOException e) {
            Toast.makeText(this, R.string.errorFileCreate, Toast.LENGTH_LONG).show();
            {
                Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
            }

        }

        if (PDF_file != null) {
            PDF_URI = FileProvider.getUriForFile(
                    this,
                    "com.serubii.mrz", //(use your app signature + ".provider" )
                    PDF_file);
        }
        return PDF_file;
    }

    private void PDF(File file) {
        /*
          Creating Document
         */
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4);


        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
        } catch (DocumentException | FileNotFoundException e) {
            Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
        }

        document.open();

//         ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER, OPTIONAL_INFO, DOB, GENDER, EXPIRATION, NATIONALITY, SURNAME, GIVEN_NAME;


        Paragraph pID_TYPE = new Paragraph("ID_TYPE: " + ID_TYPE);
        Paragraph pISSUE_COUNTRY = new Paragraph("ISSUE_COUNTRY: " + ISSUE_COUNTRY);
        Paragraph pDOCUMENT_NUMBER = new Paragraph("DOCUMENT_NUMBER: " + DOCUMENT_NUMBER);
        Paragraph pOPTIONAL_INFO = new Paragraph("OPTIONAL_INFO: " + OPTIONAL_INFO);
        Paragraph pDOB = new Paragraph("DOB: " + DOB);
        Paragraph pGENDER = new Paragraph("GENDER: " + GENDER);
        Paragraph pEXPIRATION = new Paragraph("EXPIRATION: " + EXPIRATION);
        Paragraph pNATIONALITY = new Paragraph("NATIONALITY: " + NATIONALITY);
        Paragraph pSURNAME = new Paragraph("SURNAME: " + SURNAME);
        Paragraph pGIVEN_NAME = new Paragraph("GIVEN_NAME: " + GIVEN_NAME);


        // Creating an Image object
        Image back = null;
        Image front = null;


        try {
            front = Image.getInstance(getBytes(this, FRONT));
            Log.i(TAG, "Loaded Front Image!");
        } catch (BadElementException | IOException e) {
            Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
            Log.i(TAG, "Failed Loading Front Image!");

        }

        try {
            back = Image.getInstance(getBytes(this, BACK));
            Log.i(TAG, "Loaded Back Image!");
        } catch (BadElementException | IOException e) {
            Log.e(TAG, "An exception has occurred " + "\n" + e.getMessage());
            Log.w(TAG, "Failed Loading back Image!");

        }

        float documentWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
        float documentHeight = document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin();

        //scale images
        if (front != null) {
//            front.scaleAbsolute(826, 1100);
            front.scaleToFit(documentWidth, documentHeight);
        }
        if (back != null) {
//            back.scaleAbsolute(826, 1100);
            back.scaleToFit(documentWidth, documentHeight);
        }

        // One try to create PDF
        try {
            document.add(pID_TYPE);
            document.add(pSURNAME);
            document.add(pGIVEN_NAME);
            document.add(pNATIONALITY);
            document.add(pDOB);
            document.add(pGENDER);
            document.add(pISSUE_COUNTRY);
            document.add(pDOCUMENT_NUMBER);
            document.add(pEXPIRATION);
            document.add(pOPTIONAL_INFO);
            document.add(front);
            document.add(back);
        } catch (DocumentException e) {
            Log.e(TAG, "Error creating PDF," + "\n" + e.getMessage());
        }

        document.close();


    }

    private void saveDocument() {

        SCAN_TIME = getTime();

        // fetch updated variables from text fields
        updateVariables();

        if (!edit_mode) {
            viewModel.insert(new Document(null, ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER,
                    EXPIRATION, SURNAME, GIVEN_NAME, GENDER, DOB, NATIONALITY, OPTIONAL_INFO, INITIAL_STRING,
                    FRONT_STRING, BACK_STRING, SCAN_TIME));
            Toast.makeText(this, "Location Saved with VM", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "VM SAVE DATA: " + ID_TYPE + "\n" +
                    ISSUE_COUNTRY + "\n" +
                    DOCUMENT_NUMBER + "\n" +
                    EXPIRATION + "\n" +
                    SURNAME + "\n" +
                    GIVEN_NAME + "\n" +
                    GENDER + "\n" +
                    DOB + "\n" +
                    NATIONALITY + "\n" +
                    OPTIONAL_INFO + "\n" +
                    INITIAL_STRING + "\n" +
                    FRONT_STRING + "\n" +
                    BACK_STRING + "\n" +
                    SCAN_TIME.toString());
        } else {
            Document document = new Document(ID, ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER,
                    EXPIRATION, SURNAME, GIVEN_NAME, GENDER, DOB, NATIONALITY, OPTIONAL_INFO, INITIAL_STRING,
                    FRONT_STRING, BACK_STRING, SCAN_TIME);
            viewModel.update(document);
            Toast.makeText(this, "Location updated with VM", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "VM UPDATE DATA: " + ID_TYPE + "\n" +
                    ISSUE_COUNTRY + "\n" +
                    DOCUMENT_NUMBER + "\n" +
                    EXPIRATION + "\n" +
                    SURNAME + "\n" +
                    GIVEN_NAME + "\n" +
                    GENDER + "\n" +
                    DOB + "\n" +
                    NATIONALITY + "\n" +
                    OPTIONAL_INFO + "\n" +
                    INITIAL_STRING + "\n" +
                    FRONT_STRING + "\n" +
                    BACK_STRING + "\n" +
                    SCAN_TIME.toString());
        }
    }

    private long getTime() {
        return System.currentTimeMillis();
    }

    private void updateVariables() {

        ID_TYPE = rID_TYPE.getText().toString();

        SURNAME = rSURNAME.getText().toString();

        GIVEN_NAME = rGIVEN_NAME.getText().toString();

        DOCUMENT_NUMBER = rDOC_NUMBER.getText().toString();

        ISSUE_COUNTRY = rISSUE_COUNTRY.getText().toString();

        NATIONALITY = rNATIONALITY.getText().toString();

        DOB = rDOB.getText().toString();

        GENDER = rGENDER.getText().toString();

        EXPIRATION = rEXP_DATE.getText().toString();

        OPTIONAL_INFO = rOPTIONAL_INFO.getText().toString();

        if (INITIAL == null) {
            INITIAL_STRING = "";
        } else {
            INITIAL_STRING = INITIAL.toString();
        }
        if (FRONT == null) {
            FRONT_STRING = "";
        } else {
            FRONT_STRING = FRONT.toString();
        }
        if (BACK == null) {
            BACK_STRING = "";
        } else {
            BACK_STRING = BACK.toString();
        }


    }


    /**
     * get bytes array from Uri.
     *
     * @param context current context.
     * @param uri     uri fo the file to read.
     * @return a bytes array.
     * @throws IOException failure to create file
     */
    public static byte[] getBytes(Context context, Uri uri) throws IOException {
        try (InputStream iStream = context.getContentResolver().openInputStream(uri)) {
            return getBytes(Objects.requireNonNull(iStream));
        }
        // close the stream
        /* do nothing */
    }


    /**
     * get bytes from input stream.
     *
     * @param inputStream inputStream.
     * @return byte array read from the inputStream.
     * @throws IOException because of file input stream
     */
    public static byte[] getBytes(InputStream inputStream) throws IOException {

        byte[] bytesResult;
        try (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bytesResult = byteBuffer.toByteArray();
        }
        // close the stream
        /* do nothing */
        return bytesResult;
    }
}