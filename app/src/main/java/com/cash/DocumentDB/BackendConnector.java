package com.cash.DocumentDB;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;


import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Cash on 16-Nov-17.
 */

public class BackendConnector {

    private static final String backend_host = "http://10.0.0.9";
    private static final String backend_port = "8000";

    private static String backend_url = backend_host + ":" + backend_port + "/";

    private static final String profile_method = "profile/";
    private static final String upload_image = "upload/";
    BackendConnector(Resources r) {
        this.r = r;

    }

    private Resources r;
    private HttpConnectionHandler httpConn;
    private static final String TAG = "BackendConnector";
    /**
     * URL for token exchange is:
     *  http://<url>:<port>/auth/convert-token
     * POST parameters are:
     * grant_type=convert_token&
     * client_id=<>&
     * backend=<>&
     * token=<>
     *
     */
    public OAuthToken tryLoginToBackend(String accessToken) {
        HashMap<String, String> nameValuePairs = new HashMap<>();

        nameValuePairs.put(r.getString(R.string.auth_token), accessToken);
        nameValuePairs.put(r.getString(R.string.auth_backend),
                r.getString(R.string.google_backend));
        nameValuePairs.put(r.getString(R.string.auth_client_id),
                r.getString(R.string.server_client_id));
        nameValuePairs.put(r.getString(R.string.auth_grant_type),
                r.getString(R.string.auth_convert_token));

        try {
            httpConn = new HttpConnectionHandler();
            HttpReturnParams retVals = httpConn.sendHttpMessage(
                    r.getString(R.string.backend_url) + r.getString(R.string.convert_token_url),
                    nameValuePairs,
                    HttpConnectionHandler.HTTP_METHODS.POST);
            if (retVals.mRetCode != HttpConnectionHandler.HTTP_OK) { //login failed
                Log.i(TAG, "Login failed");
                return null;
            } else {
                Log.d(TAG, "Auth @ backend successful. Got response: "
                        + retVals.mReturnBody);
                OAuthToken token = new OAuthToken(retVals.mReturnBody,
                        r.getString(R.string.oauth_app_name));
                Log.d(TAG, "Access_token: "
                        + token.getAccessToken());
                Log.d(TAG, "Refresh_token: "
                        + token.getRefreshToken());
                Log.d(TAG, "Expires: "
                        + token.getExpires());
                return token;
            }
        } catch (RuntimeException e ) {
            Log.i(TAG, "Error communicating with backend" + e.getMessage());
            return null;
        } finally { //cleanup
            httpConn = null;
        }
    }

    public UserProfile getUserProfile(OAuthToken token) {

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token.getAccessToken());
        final String profUrl = backend_url + profile_method;
        try {

            httpConn = new HttpConnectionHandler();

            HttpReturnParams retVals =
                    httpConn.sendHttpMessage(profUrl, HttpConnectionHandler.HTTP_METHODS.GET,
                            headers);
            return new UserProfile(token, retVals.mReturnBody);
        } catch (RuntimeException e ) {
            Log.i(TAG, "Error communicating with backend" + e.getMessage());
            return null;
        } finally {
            httpConn = null;
        }
    }

    public boolean uploadImage(UserProfile currentUser, String imagename, Bitmap imageBitmap) {

        byte[] data = null;
        final String targetUrl = backend_url + upload_image + imagename;
        if(imageBitmap!=null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            data = bos.toByteArray();
        } else {
            Log.i(TAG, "Tried to sent null image" );
            return false;
        }

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + currentUser.getToken().getAccessToken());


        try {
            httpConn = new HttpConnectionHandler();
            HttpReturnParams retVals =
                    httpConn.sendMultipartMessage(targetUrl, null,
                            false, headers, data, imagename);
            return (retVals.mRetCode == HttpConnectionHandler.HTTP_CREATED);
        } catch (RuntimeException e) {
            Log.i(TAG, "Error communicating with backend" + e.getMessage());
            return false;
        } finally { //cleanup
            httpConn = null;
        }
    }



}
