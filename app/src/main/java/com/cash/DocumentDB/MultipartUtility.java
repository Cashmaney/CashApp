package com.cash.DocumentDB;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Code taken from:
 * https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
 */
class MultipartUtility {

    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private BufferedWriter writer;
    public int statusCode;

    public MultipartUtility(String requestURL, String charset)
            throws IOException {
        this(requestURL, charset, null);
    }
    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL desc
     * @param charset desc
     * @throws IOException
     */
    public MultipartUtility(String requestURL, String charset, HashMap<String, String> headers)
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
        //add headers to request
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpConn.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        outputStream = httpConn.getOutputStream();
        writer = new BufferedWriter(new OutputStreamWriter(outputStream, charset));
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) throws IOException {
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