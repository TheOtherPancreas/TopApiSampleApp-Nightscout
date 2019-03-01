package org.theotherpancreas.nightscoutuploader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import com.theotherpancreas.api.ThirdPartyRoutineAdapter;
import com.theotherpancreas.data.LogEntry;

import org.json.JSONException;
import org.json.JSONObject;
import org.theotherpancreas.nightscoutuploader.settings.SettingKeys;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class NightscoutUploadRoutine extends ThirdPartyRoutineAdapter {

    @Override
    public void onGlucoseReceipt(Context context, LogEntry logEntry) {
        final JSONObject entryJson = createEntryJson(logEntry);
        beginPost(context, entryJson, "entries");
    }

    @Override
    public void onTreatmentFinalized(Context context, LogEntry logEntry) {
        if (logEntry.insulinInjection == 0 && logEntry.basalInjection == 0)
            return;

        final JSONObject treatmentJson = createTreatmentJson(logEntry);
        beginPost(context, treatmentJson, "treatments");
    }








    private JSONObject createEntryJson(LogEntry logEntry) {
        JSONObject json = new JSONObject();

        try {
            json.put("_id", NSUtil.formatUUID(logEntry.id));
            json.put("glucose", logEntry.glucose);
            json.put("units", "mg/dl");
            json.put("device", "dexcom");
            json.put("glucoseType", "Sensor");
            json.put("direction", NSUtil.convertDeltaToDirection(logEntry.trend));
            json.put("date", logEntry.time);
            json.put("dateString", new Date(logEntry.time).toString());
            json.put("sgv", logEntry.glucose);
            json.put("type", "sgv");
            json.put("filtered", logEntry.filteredGlucose);
            json.put("unfiltered", logEntry.unfilteredGlucose);
//          json.put("rssi", logEntry.getRssi());
//          json.put("noise", logEntry.getNoise());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;

    }



    private JSONObject createTreatmentJson(LogEntry logEntry) {
        JSONObject json = new JSONObject();

        try {
            json.put("_id", NSUtil.formatUUID(logEntry.id));
            json.put("enteredBy", "TOP");
            json.put("eventType", "Correction Bolus");
            json.put("glucose", logEntry.glucose);
            json.put("units", "mg/dl");
            json.put("glucoseType", "Sensor");
            json.put("direction", NSUtil.convertDeltaToDirection(logEntry.trend));
            json.put("insulin", logEntry.insulinInjection);
            json.put("isf", logEntry.correctionRatio);
            json.put("absolute", logEntry.basalInjection);
            json.put("created_at", NSUtil.formatTime(logEntry.time));
            json.put("date", logEntry.time);
            json.put("dateString", new Date(logEntry.time).toString());
            json.put("sgv", logEntry.glucose);
            json.put("type", "sgv");
            json.put("filtered", logEntry.filteredGlucose);
            json.put("unfiltered", logEntry.unfilteredGlucose);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;

    }

    private void beginPost(Context context, final JSONObject json, String endpoint) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String secret = preferences.getString(SettingKeys.API_SECRET, null);
        String baseUrl = preferences.getString(SettingKeys.BASE_URL, null);
        if (secret == null || baseUrl == null)
            return;

        final String secretHash = Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();//https://github.com/nightscout/android-uploader/blob/37d1fe04eca2097c4bbf1e7cdb6580a5d32db1a3/core/src/main/java/com/nightscout/core/utils/RestUriUtils.java
        final String url = Joiner.on('/').join(baseUrl, "api", "v1", endpoint);

        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                doPost(url, secretHash, json);
            }
        });
        networkThread.start();
        try {
            networkThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    protected boolean doPost(String url,  String secretHash, JSONObject jsonObject) {
        HttpURLConnection conn = null;
        try {
            Log.e("POST", "About to post to: " + url + "\n" + jsonObject.toString());
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("api-secret", secretHash);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            try(OutputStream os = conn.getOutputStream()) {
                os.write(jsonObject.toString().getBytes("UTF-8"));
            }

            Log.e("RESPONSE", conn.getResponseCode() + "\t" + conn.getResponseMessage());
            int statusCodeFamily = conn.getResponseCode() / 100;
            conn.disconnect();

            return statusCodeFamily == 2;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        finally {
            if (conn != null)
                conn.disconnect();
        }
    }
}
