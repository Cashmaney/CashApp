package com.cash.DocumentDB;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Cash on 16-Nov-17.
 *
 * Does stuff
 *
 */
public class OAuthToken implements Parcelable {

    /**
     * Parcel methods
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public OAuthToken createFromParcel (Parcel in) { return new OAuthToken(in); }
        public OAuthToken[] newArray(int size) { return new OAuthToken[size];  }
    };
    private OAuthToken(Parcel in){
        this.mAccessToken = in.readString();
        this.mRefreshToken = in.readString();
        this.mExpires =  in.readInt();
        this.mApplication = in.readString();
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mAccessToken);
        dest.writeString(this.mRefreshToken);
        dest.writeInt(this.mExpires);
        dest.writeString(this.mApplication);
    }


    private static final String TAG = "OAuthToken";

    private String mAccessToken;
    private String mRefreshToken;
    private int mExpires;
    private String mApplication;

    OAuthToken(String json_token, String app_name) throws RuntimeException {
        try {
            JSONObject token = new JSONObject(json_token);
            mAccessToken = token.get("access_token").toString();
            mRefreshToken = token.get("refresh_token").toString();
            mExpires = Integer.valueOf(token.get("expires_in").toString());
            mApplication = app_name;
        } catch (JSONException e) {
            throw new RuntimeException (e);
        }
    }

    public String getApplication() {
        return mApplication;
    }

    public void setApplication(String mApplication) {
        this.mApplication = mApplication;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(String mRefreshToken) {
        this.mRefreshToken = mRefreshToken;
    }

    public int getExpires() {
        return mExpires;
    }

    public void setmExpires(int mExpires) {
        this.mExpires = mExpires;
    }
}
