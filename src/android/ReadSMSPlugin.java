package com.schermannj.cordova.plugin;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created on 14.08.2016.
 */
public class ReadSMSPlugin extends CordovaPlugin {
    private static final String READ_ACTION = "read";
    private static final String REQUEST_PERMISSION_IF_NEED = "requestPermissionIfNeed";

    private static final String READ_SMS_PERMISSION = "android.permission.READ_SMS";
    private static final int REQUEST_CODE_ENABLE_PERMISSION = 55433;

    private static final String KEY_ERROR = "error";
    private static final String KEY_DATA = "data";

    private static final String SMS_COL_ID = "_id";
    private static final String SMS_COL_ADDRESS = "address";
    private static final String SMS_COL_BODY = "body";
    private static final String SMS_COL_DATE = "date";
    private static final String SMS_COL_DATE_SENT = "date_sent";
    private static final String SMS_COL_READ = "read";
    private static final String SMS_COL_SEEN = "seen";

    private CallbackContext permissionsCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        boolean res = true;

        if (action.equals(REQUEST_PERMISSION_IF_NEED)) {
            requestReadSMSPermission(callbackContext);
        } else if (action.equals(READ_ACTION)) {
            checkPermissionAndReadSMS(args, callbackContext);
        } else {
            res = false;
            respondError("Unsupported action.", callbackContext);
        }

        return res;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (permissionsCallback == null) {
            return;
        }

        if (permissions != null && permissions.length > 0 && cordova.hasPermission(READ_SMS_PERMISSION)) {
            permissionsCallback.success();
        } else {
            respondError("Can't get permission to read sms!", permissionsCallback);
        }

        permissionsCallback = null;
    }

    private void requestReadSMSPermission(CallbackContext callbackContext) {
        if (!cordova.hasPermission(READ_SMS_PERMISSION)) {
            this.permissionsCallback = callbackContext;
            cordova.requestPermission(this, REQUEST_CODE_ENABLE_PERMISSION, READ_SMS_PERMISSION);
        } else {
            permissionsCallback.success();
        }
    }

    private void checkPermissionAndReadSMS(JSONArray args, CallbackContext callbackContext) {
        if (cordova.hasPermission(READ_SMS_PERMISSION)) {
            JSONObject filter = args.length() > 0 ? args.optJSONObject(0) : new JSONObject();

            readSMS(filter, callbackContext);
        } else {
            respondError("You didn't grant permission to read your sms.", callbackContext);
        }
    }

    private void readSMS(JSONObject filter, CallbackContext callbackContext) {
        Activity ctx = cordova.getActivity();
        Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        JSONArray smsList = new JSONArray();

        if (c.moveToFirst()) {
            do {

                if (isValid(filter, c)) {
                    Map<String, Object> sms = new HashMap<String, Object>();

                    sms.put(SMS_COL_ID, c.getLong(c.getColumnIndex(SMS_COL_ID)));
                    sms.put(SMS_COL_ADDRESS, c.getString(c.getColumnIndex(SMS_COL_ADDRESS)));
                    sms.put(SMS_COL_BODY, c.getString(c.getColumnIndex(SMS_COL_BODY)));
                    sms.put(SMS_COL_DATE, c.getLong(c.getColumnIndex(SMS_COL_DATE)));
                    sms.put(SMS_COL_DATE_SENT, c.getLong(c.getColumnIndex(SMS_COL_DATE_SENT)));
                    sms.put(SMS_COL_READ, c.getInt(c.getColumnIndex(SMS_COL_READ)));
                    sms.put(SMS_COL_SEEN, c.getInt(c.getColumnIndex(SMS_COL_SEEN)));

                    smsList.put(new JSONObject(sms));
                }

            } while (c.moveToNext());
        }

        callbackContext.success(buildResp(KEY_DATA, smsList));
    }

    private boolean isValid(JSONObject filter, Cursor c) {
        Iterator<String> filterFields = filter.keys();

        while (filterFields.hasNext()) {
            String field = filterFields.next();

            int cColumnIndex = c.getColumnIndex(field);

            if(cColumnIndex == -1) {
                continue;
            }

            String cursorValue = c.getString(cColumnIndex);

            if (cursorValue != null && !areFieldsEquals(cursorValue, filter, field)) {
                return false;
            }
        }

        return true;
    }

    private boolean areFieldsEquals(String cursorValue, JSONObject filter, String field) {
        boolean equals = false;

        try {
            equals = cursorValue.equals(filter.getString(field));
        } catch (JSONException ignored) {
        }

        return equals;
    }


    private void respondError(String msg, CallbackContext callbackContext) {
        callbackContext.error(buildResp(KEY_ERROR, msg));
    }

    private JSONObject buildResp(String key, Object value) {
        JSONObject obj = new JSONObject();

        try {
            if (value == null) {
                obj.put(key, JSONObject.NULL);
            } else {
                obj.put(key, value);
            }
        } catch (JSONException ignored) {
        }

        return obj;
    }
}
