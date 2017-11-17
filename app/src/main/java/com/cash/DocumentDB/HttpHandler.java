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
            MultipartUtility multipartUtility = new MultipartUtility(string_url, charset);
            _AddHeadersToReq(multipartUtility, headers);
            _AddFormFieldsToReq(multipartUtility, params);
            multipartUtility.addFilePart(FILE_FIELD_NAME, fileBytes, fileName);
            String response = multipartUtility.finish();

            return new HttpReturnParams(response, multipartUtility.statusCode);

        } catch (IOException e) { //just wrap exception, no special handling needed
            throw new RuntimeException(e);
        }
    }

    private void _AddHeadersToReq( MultipartUtility mpu, HashMap<String, String> headers)
            throws IOException{
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                mpu.addHeaderField(entry.getKey(),entry.getValue());
            }
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
    /**
     * Code taken from:
     * https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
     */
    private class MultipartUtility {

        private final String boundary;
        private static final String LINE_FEED = "\r\n";
        private HttpURLConnection httpConn;
        private String charset;
        private OutputStream outputStream;
        private PrintWriter writer;
        public int statusCode;
        /**
         * This constructor initializes a new HTTP POST request with content type
         * is set to multipart/form-data
         *
         * @param requestURL
         * @param charset
         * @throws IOException
         */
        public MultipartUtility(String requestURL, String charset)
                throws IOException {
            this.charset = charset;

            // creates a unique boundary based on time stamp
            boundary = "===" + System.currentTimeMillis() + "===";

            URL url = new URL(requestURL);
            Log.e("URL", "URL : " + requestURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true); // indicates POST method
            httpConn.setDoInput(true);
            httpConn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);
            //httpConn.setRequestProperty();
            outputStream = httpConn.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                    true);
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        public void addFormField(String name, String value) {
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=").append(charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a upload file section to the request
         *
         * @param fieldName  name attribute in <input type="file" name="..." />
         * @param imageBytes a File to be uploaded
         * @throws IOException
         */
        public void addFilePart(String fieldName, byte[] imageBytes, String fileName)
                throws IOException {
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(fileName).append("\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            outputStream.write(encodedImage.getBytes(Charset.forName(charset)));
            /*
            ByteArrayInputStream inputStream = new ByteArrayInputStream(encodedImage);
            char[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer, pos, 4096))) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            */
            outputStream.flush();

            writer.append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a header field to the request.
         *
         * @param name  - name of the header field
         * @param value - value of the header field
         */
        public void addHeaderField(String name, String value) {
            writer.append(name).append(": ").append(value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Completes the request and receives response from the server.
         *
         * @return a list of Strings as response in case the server returned
         * status OK, otherwise an exception is thrown.
         * @throws IOException
         */
        public String finish() throws IOException {
            StringBuilder response = new StringBuilder();

            writer.append(LINE_FEED).flush();
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
            writer.close();

            // checks server's status code first
            statusCode = httpConn.getResponseCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            httpConn.disconnect();

            return response.toString();
        }
    }
}
