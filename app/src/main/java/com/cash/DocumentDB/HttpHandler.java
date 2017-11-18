package com.cash.DocumentDB;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

//import javax.net.ssl.HttpsURLConnection;




/**
 * Created by Cash on 07-Nov-17.
 */
class HttpReturnParams {

    public String mReturnBody;
    public int mRetCode;

    public HttpReturnParams(String returnBody, int retCode) {
        this.mReturnBody = returnBody;
        this.mRetCode = retCode;
    }
}



class HttpConnectionHandler {

    public enum HTTP_METHODS {
        GET,
        POST
    }
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_OK = 200;

    private static final String charset = "UTF-8";
    private static final String TAG = "HttpHandler";
    private static final int HTTP_NOT_STARTED = 0;
    private static final int HTTP_WAITING_FOR_RESPONSE = 1;
    private static final String LINE_FEED = "\r\n";
    private Integer responseCode = HTTP_NOT_STARTED;

    private static final String FILE_FIELD_NAME = "file";

    protected HttpReturnParams sendHttpMessage(@NonNull String string_url,
                                               @NonNull HTTP_METHODS method,
                                               HashMap<String, String> headers) {
        return sendHttpMessage(string_url, null, method, false, headers);
    }

    protected HttpReturnParams sendHttpMessage(@NonNull String string_url,
                                               @NonNull HashMap<String, String> params,
                                               @NonNull HTTP_METHODS method) {
        return sendHttpMessage(string_url, params, method, false, null);
    }

    protected HttpReturnParams sendHttpMessage(@NonNull String string_url,
                                               @NonNull HashMap<String, String> params,
                                               @NonNull HTTP_METHODS method,
                                               HashMap<String, String> headers) {
        return sendHttpMessage(string_url, params, method, false, headers);
    }

    protected HttpReturnParams sendHttpMessage(@NonNull String string_url,
                                               @NonNull HashMap<String, String> params,
                                               @NonNull HTTP_METHODS method,
                                               boolean secure) {
        return sendHttpMessage(string_url, params, method, secure, null);
    }

    protected HttpReturnParams sendHttpMessage(@NonNull String string_url,
                                               HashMap<String, String> params,
                                               @NonNull HTTP_METHODS method,
                                               boolean secure,
                                               HashMap<String, String> headers)
            throws RuntimeException  {
        if (string_url == null || method == null) {
            throw new NullPointerException("One or more of function parameters are null");
        }
        switch (method) {
            case POST:
                return _SendPostRequest(string_url, params, secure, headers);
            case GET:
                return _SendGetRequest(string_url, params, secure, headers);
            default:
                Log.e(TAG, "Unsupported HTTP method");
                throw new RuntimeException("Should never get here - " +
                        "unless new options are added to the methods enum");
        }
    }

    protected HttpReturnParams sendMultipartMessage(@NonNull String string_url,
                                                    HashMap<String, String> params,
                                                    boolean secure,
                                                    HashMap<String, String> headers,
                                                    byte[] fileBytes, String fileName)
            throws RuntimeException {

        try {
            MultipartUtility multipartUtility = new MultipartUtility(string_url, charset, headers);
            _AddFormFieldsToReq(multipartUtility, params);
            multipartUtility.addFilePart(FILE_FIELD_NAME, fileBytes, fileName);
            String response = multipartUtility.finish();

            return new HttpReturnParams(response, multipartUtility.statusCode);

        } catch (IOException e) { //just wrap exception, no special handling needed
            throw new RuntimeException(e);
        }
    }

    private void _AddFormFieldsToReq (MultipartUtility mpu, HashMap<String, String> params)
            throws IOException{
        //add form fields
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                mpu.addFormField(entry.getKey(),entry.getValue());
            }
        }
    }

    private HttpReturnParams _SendPostRequest(@NonNull String string_url,
                                             HashMap<String, String> params,
                                             boolean secure,
                                             HashMap<String, String> headers) {
        HttpURLConnection conn = null;
        String response = "";
        if (params == null) {
            throw new RuntimeException("POST body cannot be empty");
        }
        try {
            URL url = new URL(string_url);

            //init connection
            if (secure) {
                conn = (HttpsURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            //add headers to request
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            //set connection parameters
                //conn.setReadTimeout(15000);
                //conn.setConnectTimeout(15000);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //initialize connection writers
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, charset));

            //write POST body
            writer.write(getPostDataString(params));
            writer.flush();
            writer.close();

            //send the request
            conn.connect();

            //check HTTP response
            responseCode = conn.getResponseCode();

            //handle output
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader
                        (new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) { //todo: improve this shit
                    response += line;
                }
            } else {
                response = "";
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Encoding not supported - " +
                    "we should be using UTF-8 and never get here\n");
            throw new RuntimeException(e);
        } catch (IOException e){ //just wrap exception, no special handling needed
            throw new RuntimeException(e);
        } finally { //cleanup
            if (conn != null)
                conn.disconnect();

        }
        return new HttpReturnParams(response, responseCode);
    }

    private HttpReturnParams _SendGetRequest(@NonNull String string_url,
                                             HashMap<String, String> params,
                                             boolean secure,
                                             HashMap<String, String> headers) {
        HttpURLConnection conn = null;
        String response = "";
        try {
            URL url = new URL(constructGetURL(string_url, params));
            //initialize connection
            if (secure) {
                conn = (HttpsURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            //set HTTP headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            //send the request
            conn.setRequestMethod("GET");
            conn.connect();

            //handle response
            responseCode = conn.getResponseCode();
            String line;
            BufferedReader br = new BufferedReader
                    (new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) { //todo: improve this shit
                response += line;
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e){ //just wrap exception, no special handling needed
            throw new RuntimeException(e);
        } finally { //cleanup
            if (conn != null)
                conn.disconnect();
        }
        return new HttpReturnParams(response, responseCode);
    }

    protected String constructGetURL(String url, HashMap<String, String> params) {
        if (params == null ) {
            return url;
        }
        return url + getGetDataString(params);
    }

    protected String getPostDataString(HashMap<String, String> postDataParams) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : postDataParams.entrySet()) {
            if (first) {
                first = false;
            }
            else {
                result.append("&");
            }
            try {
                result.append(URLEncoder.encode(entry.getKey(), charset));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), charset));
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "getPostDataString::UnsupportedEncodingException", e);
            }
        }
        return result.toString();
    }

    protected String getGetDataString(HashMap<String, String> params) {
        if (params == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
                result.append("?");
            }
            else {
                result.append("&");
            }
            try {
                result.append(URLEncoder.encode(entry.getKey(), charset));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), charset));
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "getPostDataString::UnsupportedEncodingException", e);
            }
        }
        return result.toString();
    }

}
