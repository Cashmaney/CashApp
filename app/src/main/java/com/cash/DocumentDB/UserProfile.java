package com.cash.DocumentDB;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Cash on 16-Nov-17.
 *
 * Does stuff
 *
 */

public class UserProfile implements Parcelable{

    private OAuthToken mToken;
    private String mUsername;
    private String mEmail;

    /**
     * Parcel methods
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public UserProfile createFromParcel (Parcel in) {
            return new UserProfile(in);
        }
        public UserProfile[] newArray(int size) {
            return new UserProfile[size];
        }
    };
    private UserProfile(Parcel in){
        this.mUsername = in.readString();
        this.mEmail = in.readString();
        this.mToken = in.readParcelable(OAuthToken.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mUsername);
        dest.writeString(this.mEmail);
        dest.writeParcelable(this.mToken, flags);
    }

    public UserProfile(OAuthToken mToken, String json_username) {
        try {
            JSONObject token = new JSONObject(json_username);
            this.mUsername = token.get("username").toString();
            this.mEmail = token.get("email").toString();
        } catch (JSONException e) {
            throw new RuntimeException (e);
        }
        this.mToken = mToken;
    }

    public OAuthToken getToken() {
        return mToken;
    }

    public void setToken(OAuthToken mToken) {
        this.mToken = mToken;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        this.mEmail = email;
    }

}
