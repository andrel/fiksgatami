// **************************************************************************
// Home.java
// **************************************************************************
package no.fiksgatami.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import no.fiksgatami.FiksGataMi;
import no.fiksgatami.R;
import no.fiksgatami.components.CameraButton;
import no.fiksgatami.utils.CommonUtil;
import no.fiksgatami.utils.HttpUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Home extends Base {
    // ****************************************************
    // Local variables
    // ****************************************************
    private static final String LOG_TAG = Home.class.getSimpleName();
    public static final String PREFS_NAME = "FMS_Settings";
    private Button btnReport;
    private EditText textSubmissionTitle;
    private CameraButton btnPicture;
    private Button btnPictureFromGallery;
    private Button btnPosition;
    // Info that's been passed from other activities
    private Boolean haveDetails = false;
    private Boolean havePicture = false;
    private String name = null;
    private String email = null;
    private String subject = null;
    // Location info
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Double latitude;
    private Double longitude;
    long firstGPSFixTime = 0;
    long latestGPSFixTime = 0;
    long previousGPSFixTime = 0;
    private Boolean locationDetermined = false;
    int locAccuracy;
    long locationTimeStored = 0;
    // hacky way of checking the results
    private static int globalStatus = 13;
    private static final int SUCCESS = 0;
    private static final int LOCATION_NOT_FOUND = 1;
    private static final int UPLOAD_ERROR = 2;
    private static final int UPLOAD_ERROR_SERVER = 3;
    private static final int PHOTO_NOT_FOUND = 5;
    private static final int UPON_UPDATE = 6;
    private static final int COUNTRY_ERROR = 7;
    private String serverResponse;
    SharedPreferences settings;
    String versionName = null;
    // Thread handling
    ProgressDialog myProgressDialog = null;
    private ProgressDialog pd;
    final Handler mHandler = new Handler();
    private Bundle extras;
    private TextView textProgress;
    private TextView textDebug;
    private String exception_string = "";
    private List<String> categories;
    private View progressLoading;
    private ReportUpload taskReportUpload;
    private String provider;
    private ImageView mImageView;
    private String mCurrentPhotoPath;
    private DisplayMetrics displayMetrics;
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String PICTURES_DIR = "/Pictures/";
    private File photo;

    // Called when the activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        // Log.d(LOG_TAG, "onCreate, havePicture = " + havePicture);
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        textSubmissionTitle = (EditText) findViewById(R.id.submission_title);
        btnPicture = (CameraButton) findViewById(R.id.camera_button);
        btnPictureFromGallery = (Button) findViewById(R.id.gallery_button);
        btnPosition = (Button) findViewById(R.id.position_button);
        btnReport = (Button) findViewById(R.id.report_button);
        btnReport.setVisibility(View.GONE);
        textProgress = (TextView) findViewById(R.id.progress_text);
        textProgress.setVisibility(View.GONE);
        textDebug = (TextView) findViewById(R.id.debug_text);
        textDebug.setText("Debug..");
        progressLoading = findViewById(R.id.loading);
        mImageView = (ImageView) findViewById(R.id.image_preview);
        photo = createImageFile();

        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("photouri");
        }

        extras = getIntent().getExtras();
        checkBundle();
        setListeners();
        handleUpdatedVersion();

        // So what if we're in another country / missing data connectivity?
        //verifyCountry();
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mCurrentPhotoPath != null
                && mImageView.getDrawable() == null) {
            // Resture thumbnail image.
            setPic();
        }
    }

    private File getPictureStorageDirectory() {
        // Androir 2.2 / API level 8
        /*storageDir = new File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ),
            getAlbumName()
        ); */

        return new File(Environment.getExternalStorageDirectory() + PICTURES_DIR);
    }

    @Override
    protected void onStart() {
        super.onStart();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        provider = locationManager.getBestProvider(criteria, false);
        Log.d(LOG_TAG, "Using provider: " + provider);
        Location location = locationManager.getLastKnownLocation(provider);

        locationListener = new FGMLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 0, locationListener);
    }

    /**
     * Display message to user the first time he views a "new" version of the program.
     */
    private void handleUpdatedVersion() {
        // Show update message - but not to new users
        int vc = 0;
        try {
            vc = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // TODO - add this code next time!
        boolean hasSeenUpdateVersion = settings.getBoolean("hasSeenUpdateVersion" + vc, false);
        boolean hasSeenOldVersion = settings.getBoolean("hasSeenUpdateVersion" + (vc - 1), false);
        if (!hasSeenUpdateVersion && hasSeenOldVersion) {
            showDialog(UPON_UPDATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("hasSeenUpdateVersion" + vc, true);
            editor.commit();
        }
    }

    /**
     * Check that the phone are in a required country.
     */
    private void verifyCountry() {
        // TODO: Telephonymanager will probably not work on i.e tablets w/o gsm, check this
        // Check country: show warning if not in Great Britain
        TelephonyManager mTelephonyMgr = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        String country = mTelephonyMgr.getNetworkCountryIso();
        Log.d(LOG_TAG, "country = " + country);
        if (!(country.matches("no"))) {
            showDialog(COUNTRY_ERROR);
        }
    }

    @Override
    protected void onResume() {
        // Restore persistent state.
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, locationListener);
    }

    @Override
    protected void onPause() {
        // Save persistent state.
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        checkBundle();
    }

    // ****************************************************
    // checkBundle - check the extras that have been passed
    // is the user able to upload things yet, or not?
    // ****************************************************
    private void checkBundle() {
        // Log.d(LOG_TAG, "checkBundle");
        if (extras != null) {
            // Log.d(LOG_TAG, "Checking extras");
            // Details extras
            name = extras.getString("name");
            email = extras.getString("email");
            subject = extras.getString("subject");
            if (!havePicture) {
                havePicture = extras.getBoolean("photo");
            }
            // Do we have the details?
            if ((name != null) && (email != null) && (subject != null)) {
                haveDetails = true;
            } else {
                // Log.d(LOG_TAG, "Don't have details");
            }
        } else {
            extras = new Bundle();
            // Log.d(LOG_TAG, "no Bundle at all");
        }
        // Log.d(LOG_TAG, "havePicture = " + havePicture);

        // Do we have the photo?
        if (havePicture) {
            btnPicture.setText(R.string.picture_taken);
        }
        if (havePicture && haveDetails) {
            textProgress.setVisibility(View.VISIBLE);
            progressLoading.setVisibility(View.VISIBLE);
        }
    }

    // ****************************************************
    // setListeners - set the button listeners
    // ****************************************************

    private void setListeners() {
        // TODO andlin: http://developer.android.com/training/camera/photobasics.html sjekk isIntentAvailable.
        btnPicture.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                captureImage();
            }
        });
        btnPictureFromGallery.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(i, "Velg bilde"), PICK_UPLOAD_PICTURE);
            }
        });
        btnPosition.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                Intent i = new Intent(Home.this, Position.class);
                i.putExtra("lat", latitude);
                i.putExtra("long", longitude);
                startActivity(i);
            }
        });
        btnReport.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                locationDetermined = true;
                uploadToFMS();
            }
        });
        mImageView.setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                captureImage();
            }
        });
    }

    private void captureImage() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        startActivityForResult(imageCaptureIntent, RECIEVE_CAMERA_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == RESULT_OK) && requestCode == RECIEVE_CAMERA_PICTURE) {
            setPic();
        } else if (resultCode == RESULT_OK && requestCode == PICK_UPLOAD_PICTURE) {
            setPic();
        } else {
            Log.w(LOG_TAG, String.format("onActivityResult with unknown requestCode/resultCode: %s/%s", requestCode, resultCode));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        Log.d(LOG_TAG, photoW + ", " + targetW);
        Log.d(LOG_TAG, photoH + ", " + targetH);

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    private File createImageFile() {
            File imageDir = getPictureStorageDirectory();
            Log.d(LOG_TAG, FiksGataMi.PHOTO_FILENAME + ", " + JPEG_FILE_SUFFIX + ", " + imageDir);
            File image = new File(imageDir, FiksGataMi.PHOTO_FILENAME + JPEG_FILE_SUFFIX);
            mCurrentPhotoPath = image.getAbsolutePath();
            return image;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save state related to the activity (transient state)
        Log.d(LOG_TAG, "onSaveInstanceState");

        outState.putString("photouri", mCurrentPhotoPath);

        if (havePicture != null) {
            // Log.d(LOG_TAG, "mRowId = " + mRowId);
            outState.putBoolean("photo", havePicture);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore state related to the activity (transient state)
        Log.d(LOG_TAG, "onRestoreInstanceState");

        havePicture = savedInstanceState.getBoolean("photo");
    }

    // **********************************************************************
    // uploadToFMS: uploads details, handled via a background thread
    // Also checks the age and accuracy of the GPS data first
    // **********************************************************************
    private void uploadToFMS() {
        if (taskReportUpload != null && taskReportUpload.getStatus() == AsyncTask.Status.RUNNING) {
            taskReportUpload.cancel(true);
        }
        taskReportUpload = new ReportUpload();
        taskReportUpload.execute(null);
    }

    private void updateResultsInUi() {
        if (globalStatus == UPLOAD_ERROR) {
            showDialog(UPLOAD_ERROR);
        } else if (globalStatus == UPLOAD_ERROR_SERVER) {
            showDialog(UPLOAD_ERROR_SERVER);
        } else if (globalStatus == LOCATION_NOT_FOUND) {
            showDialog(LOCATION_NOT_FOUND);
        } else if (globalStatus == PHOTO_NOT_FOUND) {
            showDialog(PHOTO_NOT_FOUND);
        } else {
            // Success! - Proceed to the success activity!
            Intent i = new Intent(Home.this, Success.class);
            // TODO andrel: Are "latString" and "longString" used?
            i.putExtra("latString", latitude());
            i.putExtra("lonString", longitude());
            startActivity(i);
        }
    }

    private String latitude() {
        return latitude == null ? "" : latitude.toString();
    }

    private String longitude() {
        return longitude == null ? "" : longitude.toString();
    }

    // **********************************************************************
    // onCreateDialog: Dialog warnings
    // **********************************************************************
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case COUNTRY_ERROR:
                return new AlertDialog.Builder(Home.this)
                .setTitle(R.string.dialog_country_error_title)
                .setPositiveButton(R.string.common_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                })
                .setMessage(R.string.dialog_country_error)
                .create();
            case UPLOAD_ERROR:
                return new AlertDialog.Builder(Home.this)
                .setTitle(R.string.dialog_upload_error_title)
                .setPositiveButton(R.string.common_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                })
                .setMessage(String.format(getString(R.string.dialog_upload_error), exception_string, serverResponse)).create();
            case UPLOAD_ERROR_SERVER:
                return new AlertDialog.Builder(Home.this)
                .setTitle(R.string.dialog_upload_server_error_title)
                .setPositiveButton(R.string.common_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                })
                .setMessage(String.format(getString(R.string.dialog_upload_server_error, serverResponse))).create();

            case LOCATION_NOT_FOUND:
                return new AlertDialog.Builder(Home.this)
                .setTitle(R.string.dialog_gps_no_location_title)
                .setPositiveButton(R.string.common_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                })
                .setMessage(R.string.dialog_gps_no_location)
                .create();
            case PHOTO_NOT_FOUND:
                return new AlertDialog.Builder(Home.this).setTitle(R.string.dialog_picture_not_found_title)
                .setPositiveButton(R.string.common_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                }).setMessage(R.string.dialog_picture_not_found).create();
            case UPON_UPDATE:
                if (versionName == null) {
                    versionName = "";
                }
                return new AlertDialog.Builder(Home.this).setTitle(R.string.app_update__whats_new_title)
                .setPositiveButton(R.string.common_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                }).setMessage(String.format(getString(R.string.app_update__whats_new_details, versionName))).create();
        }
        return null;
    }


    private boolean getCategories() {
        try {
            // TODO andlin: Bruk API: http://www.fiksgatami.no/open311
            // TODO andlin: Logg/sjekk at dette ikke kalles gjentatte ganger.
	        if (true) {
	            throw new RuntimeException("Hopper over henting av kategorier (for nå)!");
	        }

            HttpResponse response = HttpUtil.getCategories(this, latitude, longitude);
            String responseString = HttpUtil.isValidResponse(response);
            if (!CommonUtil.isStringNullOrEmpty(responseString)) {
                categories = HttpUtil.getCategoriesFromResponse(responseString);
                Log.d(LOG_TAG, "Fetched categories: " + categories);
                return true;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e.getCause());
        }

        return false;
    }

    private boolean checkLoc(Location location) {
        // get time - store the GPS time the first time it is reported, then check it against future reported times
        latestGPSFixTime = location.getTime();
        if (firstGPSFixTime == 0) {
            firstGPSFixTime = latestGPSFixTime;
        }
        if (previousGPSFixTime == 0) {
            previousGPSFixTime = latestGPSFixTime;
        }
        long timeDiffSecs = (latestGPSFixTime - previousGPSFixTime) / 1000;
        previousGPSFixTime = latestGPSFixTime;

        locAccuracy = (int) location.getAccuracy();
        // Check our location - no good if the GPS accuracy is more than 24m
        if ((locAccuracy > 24) || (timeDiffSecs == 0)) {
            if (timeDiffSecs == 0) {
                // nor do we want to report if the GPS time hasn't changed at
                // all - it is probably out of date
                textProgress.setText(R.string.gps_wait_expired_gps_position);
            } else {
                textProgress.setText(String.format(getString(R.string.gps_wait_require_more_accuracy), locAccuracy));
            }
            return false;
        } else {
            // but if all the requirements have been met, proceed
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            if (haveDetails && havePicture) {
                btnReport.setVisibility(View.VISIBLE);
                btnReport.setText(R.string.gps_signal_found_please_report_now);
                textProgress.setVisibility(View.GONE);
                progressLoading.setVisibility(View.GONE);
            } else {
                textProgress.setText(R.string.gps_signal_found);
            }
            mHandler.post(new Runnable() {
                public void run() {
                    getCategories(); // todo fixme implement spinner logic, show that data is loading
                }
            });
            return true;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (  Integer.valueOf(android.os.Build.VERSION.SDK) < 7 //Instead use android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
        }

        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        // TODO: This dosen't work - we are still sendt back to the last activity
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform.
        finish(); // Close application on back-press
    }

    // ****************************************************
    // Options menu functions
    // ****************************************************

    // TODO - add Bundles for these?
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem helpItem = menu.add(Menu.NONE, MENU_HELP, Menu.NONE, R.string.menu_help);
        MenuItem aboutItem = menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about);
        aboutItem.setIcon(android.R.drawable.ic_menu_info_details);
        helpItem.setIcon(android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                Intent i = new Intent(Home.this, Help.class);
                if (extras != null) {
                    i.putExtras(extras);
                }
                startActivity(i);
                return true;
            case MENU_ABOUT:
                Intent j = new Intent(Home.this, About.class);
                if (extras != null) {
                    j.putExtras(extras);
                }
                startActivity(j);
                return true;
        }
        return false;
    }

    // read the photo file into a byte array...
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "
                    + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    private class ReportUpload extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog
            .show(Home.this,
                    getString(R.string.progress_uploading_title),
                    getString(R.string.progress_uploading),
                    true, false);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            String responseString = null;

            HttpResponse response = null;
            try {
                response = HttpUtil.postReport(Home.this, subject, name, email, latitude, longitude );

                HttpEntity resEntity = response.getEntity();
                responseString = EntityUtils.toString(resEntity);
                Log.i(LOG_TAG, String.format("Response was %s : %s", response.getStatusLine().getStatusCode(), responseString));

                if (resEntity != null) {
                    Log.i(LOG_TAG, "Response content length: " + resEntity.getContentLength());
                }

                // use startswith to workaround bug where CATEGORIES-info
                // is display on every call to import.cgi
                if (responseString.startsWith("SUCCESS")) {
                    // launch the Success page
                    globalStatus = SUCCESS;
                    return true;
                } else {
                    // print the response string?
                    serverResponse = responseString;
                    globalStatus = UPLOAD_ERROR;
                    return false;
                }

            } catch (IOException e) {
                Log.v(LOG_TAG, "Exception", e);
                exception_string = e.getMessage();
                globalStatus = UPLOAD_ERROR;
                serverResponse = "";
            }
            return false;

        }

        @Override
        protected void onPostExecute(Boolean result) {
            pd.dismiss();
            updateResultsInUi();
        }
    }

    private class FGMLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            textDebug.setText("OnLocationChanged[" + location.getProvider() + "] " + location.getLongitude() + "/" + location.getLatitude());

            latitude = location.getLatitude();
            longitude = location.getLongitude();

            // keep checking the location + updating text - until we have what we need
            if (!locationDetermined) {
                //checkLoc(location);
            }
        }

        public void onProviderDisabled(String provider) {
            Log.d(LOG_TAG, "Disabled provider " + provider);
            Toast.makeText(Home.this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Log.d(LOG_TAG, "Enabled new provider " + provider);
            Toast.makeText(Home.this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.d(LOG_TAG, "StatusChanged[" + provider + "] " + status + "(" + extras.get("satellites") +  ")");
        }
    }

}
