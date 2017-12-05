package com.creedon.cordova.plugin.photobrowser;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.creedon.cordova.plugin.photobrowser.metadata.Datum;
import com.creedon.cordova.plugin.photobrowser.metadata.PhotoDetail;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static org.apache.cordova.PluginResult.Status.OK;

/**
 * This class echoes a string called from JavaScript.
 */
public class PhotoBrowserPlugin extends CordovaPlugin {

    public  static final String KEY_CAPTION = "caption";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ID = "id";
    public static final String KEY_PHOTO = "photo";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_ACTION_SEND = "send";
    public static final String KEY_ACTION  = "action";
    public static final String KEY_PHOTOS ="photos";
    private static final String KEY_NAME = "name";
    public static final String DEFAULT_ACTION_RENAME = "rename";
    private static final String DEFAULT_ACTION_EDITCAPTION = "editCaption";
    private static final String DEFAULT_ACTION_DELETEPHOTOS = "deletePhotos";
    private CallbackContext callbackContext;
    private PhotoDetail photoDetail;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("showGallery")) {
            JSONObject jsonOptions = args.getJSONObject(0);
            photoDetail = PhotoDetail.getInstance(jsonOptions);
            photoDetail.setOnCaptionChangeListener(new PhotoDetail.PhotoDataListener(){

                @Override
                public boolean onCaptionChanged(Datum datum, String caption, String id, String type) {
                    JSONObject res = new JSONObject();
                    try {
                        res.put(KEY_PHOTO,datum.toJSON());
                        res.put(KEY_CAPTION,caption);
                        res.put(KEY_ACTION,DEFAULT_ACTION_EDITCAPTION);
                        res.put(KEY_ID,id);
                        res.put(KEY_TYPE,type);
                        res.put(KEY_DESCRIPTION,"edit caption of photo");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return false;
                    }
                    PluginResult result = new PluginResult(OK,res);
                    result.setKeepCallback(true);
                    PhotoBrowserPlugin.this.callbackContext.sendPluginResult(result);

                    return true;
                }

                @Override
                public void onSetName(String s, String id, String type) {
                    JSONObject res = new JSONObject();
                    try {

                        res.put(KEY_ACTION,DEFAULT_ACTION_RENAME);
                        res.put(KEY_ID,id);
                        res.put(KEY_TYPE,type);
                        res.put(KEY_NAME,s);
                        res.put(KEY_DESCRIPTION,"edit album name");

                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                    PluginResult result = new PluginResult(OK,res);
                    result.setKeepCallback(true);
                    PhotoBrowserPlugin.this.callbackContext.sendPluginResult(result);
                }

                @Override
                public void onPhotoDeleted(ArrayList<String> deletedData, String id, String type) {
                    JSONObject res = new JSONObject();
                    try {

                        res.put(KEY_ACTION,DEFAULT_ACTION_DELETEPHOTOS);
                        res.put(KEY_ID,id);
                        res.put(KEY_TYPE,type);
                        res.put(KEY_PHOTOS, new JSONArray(deletedData));
                        res.put(KEY_DESCRIPTION,"delete selected photos");

                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                    PluginResult result = new PluginResult(OK,res);
                    result.setKeepCallback(true);
                    PhotoBrowserPlugin.this.callbackContext.sendPluginResult(result);
                }

            });

            this.showGallery(jsonOptions, callbackContext);
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

            photoDetail = PhotoDetail.getInstance(options);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long totalMegs = mi.totalMem / 1048576L;
            System.out.println("[NIX] totalMegs: " + totalMegs);
            if(options.has("images")){
                options.remove("images");
            }if(options.has("thumbnails")){
                options.remove("thumbnails");
            }if(options.has("captions")){
                options.remove("captions");
            }if(options.has("data")){
                options.remove("data");
            }
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

            String result = data.getStringExtra(Constants.RESULT);
            if(result != null && this.callbackContext != null)  {
                JSONObject res = null;
                try {
                    res = new JSONObject(result);
                    if(res != null){
                        this.callbackContext.success(res);
                    }else{
                        this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{



                PluginResult res = new PluginResult(OK);
                res.setKeepCallback(false);
                PhotoBrowserPlugin.this.callbackContext.sendPluginResult(res);
            }

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

    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();

        return state;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;
    }

}
