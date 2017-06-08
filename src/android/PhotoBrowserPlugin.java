package com.creedon.cordova.plugin.photobrowser;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class PhotoBrowserPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("showGallery")) {
            JSONObject options = args.getJSONObject(0);
            JSONArray images = options.getJSONArray("images");
            JSONArray thumbnails = options.getJSONArray("thumbnails");
            JSONArray data = options.getJSONArray("data");
            JSONArray captions = options.getJSONArray("captions");
            String id = options.getString("id");
            String name = options.getString("name");
            int count = options.getInt("count");
            String type = options.getString("type");
            JSONArray actionSheet = options.getJSONArray("actionSheet");

            this.showGallery(options, callbackContext);
            return true;
        }
        if (action.equals("showBrowser")) {
            String message = args.getString(0);
            this.showBrowser(message, callbackContext);
            return true;
        }
        return false;
    }

    private void showGallery(JSONObject options, CallbackContext callbackContext) {
        if (options != null && options.length() > 0) {


            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long totalMegs = mi.totalMem / 1048576L;
            System.out.println("[NIX] totalMegs: " + totalMegs);

            Intent intent = new Intent(cordova.getActivity(), PhotoBrowserPluginActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("options",options.toString());
            this.cordova.startActivityForResult(this, intent, 0);

        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void showBrowser(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.RESULT_ADD_PHOTO){

        }
        else if (resultCode == Activity.RESULT_OK && data != null) {
            JSONObject res = new JSONObject();
            this.callbackContext.success(res);
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            if (error == null)
                this.callbackContext.error("Error");
            this.callbackContext.error(error);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONObject res = new JSONObject();
            if(this.callbackContext != null )
                this.callbackContext.error(res);

        } else {
            JSONObject res = new JSONObject();
            if(this.callbackContext != null )
                this.callbackContext.error(res);
        }

    }
}
