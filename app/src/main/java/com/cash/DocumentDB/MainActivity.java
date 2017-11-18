package com.cash.DocumentDB;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.lang.ref.WeakReference;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final int ACTION_TAKE_PHOTO_S = 2;
    private static final int REQUEST_CAMERA = 0;
    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private static final String TAG = "MainActivity";

    private static final int INTENT_FLAGS = 0;

    private UserProfile mUserProfile = null;

    private static Bundle extras = null;

    protected GoogleApiClient mGoogleApiClient;

    private ImageView mImageView;
    private Bitmap mImageBitmap = null;
    private Button mUploadBtn = null;
    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
                }
            };


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null) );
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUserProfile = extras.getParcelable(getString(R.string.user_profile_id));
        }

        if (mUserProfile == null || mUserProfile.getUsername() == null) {
            throw new IllegalArgumentException("No username passed");
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            //Permission not granted, ask for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }

        mImageView = findViewById(R.id.PicImView); //sets imageview as the bitmap

        Button picBtn = findViewById(R.id.CameraStartBtn);
        setBtnListenerOrDisable(
                picBtn,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );


        Button logoutBtn = findViewById(R.id.Logout);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                signout_google();
            }
        });

        mUploadBtn = findViewById(R.id.upload_btn);
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new UploadPhotoTask((MainActivity)getParent()).execute();
            }
        });

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI(mUserProfile.getUsername()));
    }

    private static class UploadPhotoTask extends AsyncTask<Void, Void, Boolean> {
        //private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        //Log.d(TAG, "handleSignInResult:" + completedTask.getResult().isSuccess());

        // handleSignInResult(GoogleSignInResult result)
        private WeakReference<MainActivity> activityReference;

        UploadPhotoTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Boolean doInBackground(Void... image) {
            return uploadPhoto();
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            //showProgress(false);

            if (success) {
                //
            } else {
                //
            }
        }
        @Override
        protected void onCancelled() {
            //showProgress(false);
        }

        @Override
        protected void onPreExecute() {
            //showProgress(true);
        }

        /**
         *  Actual synchronous upload task
         */
        private boolean uploadPhoto() {
            BackendConnector backend = new BackendConnector(activityReference.get().getResources());

            if (backend.uploadImage(activityReference.get().mUserProfile,
                    "pic1.jpg", activityReference.get().mImageBitmap)) {
                Log.d(TAG, "Upload success");
                return true;
            } else {
                Log.d(TAG, "Upload failed");
                return false;
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTION_TAKE_PHOTO_S: {
                    handleCameraPhoto(data);
                }
                break;
            }
/*
    Bitmap image = (Bitmap) data.getExtras().get("data");

    imageview.setImageBitmap(image);
}*/
        }
    }

    private void handleCameraPhoto(Intent intent) {
        if (extras == null) {
            extras = intent.getExtras();
        }
        if (extras!= null) {
            mImageBitmap = (Bitmap) extras.get("data");
        }

        //scalePic();

        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(View.VISIBLE);
        mUploadBtn.setVisibility(View.VISIBLE);
    }

    /*@SuppressWarnings("StatementWithEmptyBody")
    private void handlePhoto() {

		*//* Scale the picture taken to the view size *//*
        if (mImageBitmap != null) {
            //stuff
        }
    }*/



    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

/*        switch(actionCode) {
            case ACTION_TAKE_PHOTO_B:

                break;

            default:
                break;
        } // switch*/

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void signout_google() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        mGoogleApiClient.connect();
        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {

                if(mGoogleApiClient.isConnected()) {
                    Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                            new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    Log.d(TAG, "Revoke access using Google Api.");
                                    //signOutIfConnected(); //REMOVED
                                }
                            });
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback
                            (new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.d(TAG, "User Logged out");
                                /*  Toast.makeText(getApplicationContext(),
                                 getString(R.string.signed_out),Toast.LENGTH_SHORT).show();*/
                                startActivity(new Intent(MainActivity.this, LoginActivity.class)
                                        .addFlags(INTENT_FLAGS));
                            }
                        }
                    });
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "Google API Client Connection Suspended");
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void setBtnListenerOrDisable(
            Button btn,
            Button.OnClickListener onClickListener,
            String intentName) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(
                    getText(R.string.cannot).toString() + " " + btn.getText());
            btn.setClickable(false);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI(String str);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
