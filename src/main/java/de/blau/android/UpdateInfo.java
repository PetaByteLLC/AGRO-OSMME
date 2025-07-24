package de.blau.android;

import org.json.JSONException;
import org.json.JSONObject;

public class UpdateInfo {

    private final int versionCode;
    private final String versionName;
    private final String apkUrl;
    private final String changelog;

    public UpdateInfo(JSONObject json) throws JSONException {
        this.versionCode = json.getInt("versionCode");
        this.versionName = json.getString("versionName");
        this.apkUrl = json.getString("apkUrl");
        this.changelog = json.getString("changelog");
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public String getChangelog() {
        return changelog;
    }
}