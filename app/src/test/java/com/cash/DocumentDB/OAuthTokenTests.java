package com.cash.DocumentDB;

import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

/**
 *
 */
public class OAuthTokenTests {
    @Test
    public void TestCtor() throws Exception {
        String json_token = "{\"zip\":\"56601\"}";
        String app_name = "test";
        try {
            JSONObject token = new JSONObject(json_token);
           //OAuthToken token = new OAuthToken(json_token, app_name);
            assertEquals(token.get("zip"), "56601");
        } catch (Exception e) {
            fail("Failed to parse simple token input as JSON: " + e.getMessage());
        }

        json_token = "{\"expires_in\":36000,\"scope\":\"read write\",\"token_type\":\"Bearer\"," +
                "\"access_token\":\"k8zsX4EVFqX8cOxmK7P4wuveZa6WFt\"," +
                "\"refresh_token\":\"1IbZXY9wJtexDGqSeHuPpjxpqUXfh3\"}";

        try {
            //JSONObject token = new JSONObject(json_token);
            OAuthToken token = new OAuthToken(json_token, app_name);
            assertEquals(token.getAccessToken(), "k8zsX4EVFqX8cOxmK7P4wuveZa6WFt");
            assertEquals(token.getExpires(), 36000);
            assertEquals(token.getRefreshToken(), "1IbZXY9wJtexDGqSeHuPpjxpqUXfh3");
        } catch (Exception e) {
            fail("Failed to parse real token input as JSON: " + e.getMessage());
        }

    }
}
