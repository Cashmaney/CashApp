package com.cash.DocumentDB;

import org.junit.Test;

import java.util.HashMap;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

/**
 *
 */
public class HttpHandlerTests extends HttpConnectionHandler {
    @Test
    public void HttpGetURLCorrect() throws Exception {
        String url = "jfk://this_is_a_test_url:8000/";

        HashMap<String, String> params = new HashMap<>();

        params.put("param1", "value1"); //basic values
        params.put("p@", "2@"); //tests for url encoding

        final String expected = "jfk://this_is_a_test_url:8000/?p%40=2%40&param1=value1";

        String result = this.constructGetURL(url, params);

        assertEquals("Constructed URL test", expected, result);

        }

    @Test
    public void CreatePostData() throws Exception {
        HashMap<String, String> params = new HashMap<>();

        params.put("param1", "value1"); //basic values
        params.put("p@", "2@"); //tests for url encoding

        final String expected = "p%40=2%40&param1=value1";

        String result = this.getPostDataString(params);

        assertEquals("Post data correctness", expected, result);
    }
}