package com.creedon.cordova.plugin.photobrowser;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * This class echoes a string called from JavaScript.
 */
public class PhotoBrowserPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("showGallery")) {
            String message = args.getString(0);
            this.showGallery(message, callbackContext);
            return true;
        }
        if (action.equals("showBrowser")) {
            String message = args.getString(0);
            this.showBrowser(message, callbackContext);
            return true;
        }
        return false;
    }

    private void showGallery(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            this.callbackContext = callbackContext;

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long totalMegs = mi.totalMem / 1048576L;
            System.out.println("[NIX] totalMegs: " + totalMegs);

            Intent intent = new Intent(cordova.getActivity(), CordovaPhotoBrowser.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            this.cordova.startActivityForResult(this, intent, 0);

//            callbackContext.success(message);
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
}
