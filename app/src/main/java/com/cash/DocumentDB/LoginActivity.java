/*
 * Package Description
 */
package com.cash.DocumentDB;

import static android.Manifest.permission.READ_CONTACTS;
import static java.lang.Thread.sleep;

import android.accounts.Account;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements
        LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    private BackendConnector backend;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "SignInActivity";
    public static final String EXTRA_MESSAGE = "com.cash.DocumentDB.username";
    //Id to identity READ_CONTACTS permission request.
    private static final int REQUEST_READ_CONTACTS = 0;
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private UserProfile mUserProfile = null;
    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginSignInButtonsView;
    private View mLoginTextInputView;
    private GoogleApiClient mGoogleApiClient;
    private static final int AC_GOOGLE_OAUTH2 = 1000;
    private static final int AC_USER_PASS = 2000;
    private static final int AC_FACEBOOK = 3000;
    private static final int AUTH_CODE_REQUEST_CODE = 8585;

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onStart() {
        super.onStart();
        //todo: fix weird behaviour with silent logins (probably cause i don't understand it at the moment)
        mGoogleApiClient.connect();
/*        OptionalPendingResult<GoogleSignInResult> opr =
                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            handleSignInResult taskSignIn = new handleSignInResult();
            taskSignIn.execute(opr.get());
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult taskSignIn = new handleSignInResult();
                    taskSignIn.execute(googleSignInResult);
                }
            });
        }*/
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.google_sign_in_button:
                signIn();
                break;
/*            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;*/
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult taskSignIn = new handleSignInResult();
            taskSignIn.execute(result);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("_", "connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        backend = (BackendConnector) new BackendConnector(getResources());
        findViewById(R.id.google_sign_in_button).setOnClickListener(this);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .build();


        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginSignInButtonsView = findViewById(R.id.sign_in_buttons);
        mProgressView = findViewById(R.id.login_progress);
        mLoginTextInputView = findViewById(R.id.text_input);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginTextInputView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginTextInputView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginTextInputView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mLoginSignInButtonsView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginSignInButtonsView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginSignInButtonsView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void OpenMainActivity() {
        Intent intent = new Intent(LoginActivity.this.getApplicationContext(), MainActivity.class);
        intent.putExtra(getString(R.string.user_profile_id), mUserProfile);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.disappear);
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    private class handleSignInResult extends AsyncTask<GoogleSignInResult, Void, Boolean> {
        //private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        //Log.d(TAG, "handleSignInResult:" + completedTask.getResult().isSuccess());

        // handleSignInResult(GoogleSignInResult result)

        int connType = AC_GOOGLE_OAUTH2;
        String accountName = "null";
        boolean rerunlogin = false;

        @Override
        protected Boolean doInBackground(GoogleSignInResult... result) {
            OAuthToken token;
            switch (connType) {
                case AC_GOOGLE_OAUTH2:
                    String accessToken = tryLoginToGoogle(result[0].getSignInAccount());
                    if (!accessToken.equals("")) {
                        token = backend.tryLoginToBackend(accessToken);
                        if (token != null) {  //todo: simplify if no more processing needed
                            mUserProfile = backend.getUserProfile(token);
                            return true;
                        } else {
                            return false;
                        }
                    }
                case AC_USER_PASS:
                    //signIn(); //todo: username/password authentication
                    return false;

                case AC_FACEBOOK:
                    //signIn(); //todo: facebook login
                    return false;
                default:
                    Log.d(TAG, "loginToBackendServer::Tried to login with invalid type");
                    return false;
            }
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            showProgress(false);

            if (success) {
                OpenMainActivity();
            } else {
                if (!accountName.equals("null")) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                    mGoogleApiClient.connect();
                    if(mGoogleApiClient.isConnected()) {
                        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                                new ResultCallback<com.google.android.gms.common.api.Status>() {
                                    @Override
                                    public void onResult(@NonNull com.google.android.gms.common.api.Status status) {
                                        Log.d(TAG, "Revoke access using Google Api.");
                                        //signOutIfConnected(); //REMOVED
                                    }
                                });
                        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback
                                (new ResultCallback<com.google.android.gms.common.api.Status>() {
                                    @Override
                                    public void onResult(@NonNull com.google.android.gms.common.api.Status status) {
                                    }
                                });
                    }
                }
            }
        }
        @Override
        protected void onCancelled() {
            showProgress(false);
        }

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        /*!
         *  Returns "" on failure, the actual hexstring access_token on success
         */
        private String tryLoginToGoogle(GoogleSignInAccount account) {
            String accessToken = "";
            if (account == null || account.getAccount() == null) {
                return accessToken;
            }
            accountName = account.getDisplayName();
            Bundle mBundle = new Bundle();

            try {
                accessToken = GetGoogleAccessToken(account);
            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), AUTH_CODE_REQUEST_CODE);
                rerunlogin = true;
            } catch (GoogleAuthException e) {
                Log.i(TAG, "GoogleAuthException on login -- login failed");
                return "";
            } catch (NullPointerException e) {
                Log.i(TAG, "NullPointerException on login -- login failed");
                return "";
            } catch (IOException e) {
                Log.i(TAG, "IOException on login -- login failed");
                return "";
            }

                    /*
                     *    GetGoogleAccessToken could return UserRecoverableAuthException which we
                     *    prompt users to allow permissions, then we rerun the function.
                     */
            if (rerunlogin) {
                try {
                    accessToken = GetGoogleAccessToken(account);
                } catch (UserRecoverableAuthException e) {
                    Log.e(TAG, "UserRecoverableAuthException on login -- login failed");
                    return "";
                } catch (GoogleAuthException e) {
                    Log.e(TAG, "GoogleAuthException on login -- login failed");
                    return "";
                } catch (NullPointerException e) {
                    Log.e(TAG, "NullPointerException on login -- login failed");
                    return "";
                } catch (IOException e) {
                    Log.e(TAG, "IOException on login -- login failed");
                    return "";
                }
            }
            Log.d(TAG, "Auth @ Google successful. Got access_token: " + accessToken);
            return accessToken;
        }

    }
    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                OpenMainActivity();
            } else {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.signed_out),Toast.LENGTH_SHORT).show();
                // mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an synchronous call to google api to get the access token task used to authenticate
     * the user to the backend.
     */
    private String GetGoogleAccessToken(GoogleSignInAccount account) throws
    NullPointerException, IOException, UserRecoverableAuthException, GoogleAuthException {

        final Account mAccount = account.getAccount();
        Bundle mBundle = new Bundle();



        if (mAccount == null) {
            throw new NullPointerException();
        }
        // TODO: attempt authentication against a network service.
        String sAccessToken = "";
        try {
            sAccessToken = GoogleAuthUtil.getToken(
                LoginActivity.this,
                mAccount,
                "oauth2:" + Scopes.PROFILE + " "
                        + "https://www.googleapis.com/auth/userinfo.profile"
                , mBundle);
        } catch (IOException e) {
            Log.w(TAG, "IOException on authentication user.", e); //let caller handle this
            throw e;
        } catch (UserRecoverableAuthException e) {
            Log.e(TAG, "GetGoogleAccessToken:: UserRecoverableAuthException", e);
            throw e;
        } catch (GoogleAuthException e) { //let caller handle this
            Log.w(TAG, "Authentication error.", e); //let caller handle this
            throw e;
        }

        return sAccessToken;
    }
}

