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
            String albumType = options.getString("albumType");
            JSONArray actionSheet = options.getJSONArray("actionSheet");
/*
0 = {LinkedHashMap$LinkedHashMapEntry@7684} "images" -> "["https:\/\/nixplay-rnd-original.s3.amazonaws.com\/2018\/2018_106533b2bcc1d60a771406bf10759b58_PREVIEW.jpg?Signature=%2BtaFBYvRZF0d4TQoK4jXNGu2sjQ%3D&Expires=1496224800&AWSAccessKeyId=AKIAIDQQLEZE235GX5QQ&response-expires=Wed%2C%2031%20May%202017%2010%3A00%3A00%20GMT&response-cache-control=max-age%3D86400","https:\/\/nixplay-rnd-original.s3.amazonaws.com\/2018\/2018_fc988f1af1e59ee04104c43c775c3bb7_PREVIEW.jpg?Signature=KSMdfC0RFw%2F0asblyqRvWxnV51w%3D&Expires=1496224800&AWSAccessKeyId=AKIAIDQQLEZE235GX5QQ&response-expires=Wed%2C%2031%20May%202017%2010%3A00%3A00%20GMT&response-cache-control=max-age%3D86400","https:\/\/nixplay-rnd-original.s3.amazonaws.com\/2018\/2018_841eb5e47fd28b76631f0ca099b7d784_PREVIEW.jpg?Signature=ZIaWpn%2F0Ab23E9VgqfuxT740Eh4%3D&Expires=1496224800&AWSAccessKeyId=AKIAIDQQLEZE235GX5QQ&response-expires=Wed%2C%2031%20May%202017%2010%3A00%3A00%20GMT&response-cache-control=max-age%3D86400"]"
1 = {LinkedHashMap$LinkedHashMapEntry@7685} "thumbnails" -> "["https:\/\/d2tu30k3qbci1d.cloudfront.net\/2018\/2018_106533b2bcc1d60a771406bf10759b58.jpg?Expires=1496224800&Signature=J0q7Ca-UdjfecuqetPG8cGe1vfVCJ6tDmbc8oFZVNgcnvLpSexTPSd-zu6cYuu6dso-0Ac1IVtljWjJaQAX8iJqtjJCpPj8IN3wICC0U26ee7W7rF7tQSCRG2-E9~jvUM5R0B86trTt3snfrLPG-IODCruXCltjCuUY2fw~pEpITSBPv0ztxJDEemDq6NZCUwqVbps8VUi2sL6RR9i8UcUSEO88IEfZCWOebdr2Pfir1PbRDvxcQxxAFmtmXZ6dn-rhtdBp65grl9c8mVWPjIymbJBty-GrW5v4Ih4K2nBJYLfSqaoXx6o9xaCfeEWBfiE6uCfCbhJJGBgTpZgGbQA__&Key-Pair-Id=APKAJTL2VMY3XS7W2VIA","https:\/\/dejf3pcvgnzwx.cloudfront.net\/2018\/2018_fc988f1af1e59ee04104c43c775c3bb7.jpg?Expires=1496224800&Signature=fDqO1Cp4w-eb6FWh1zr1Ms56rKcqhBTs1LKgL~OjDGBJCj9r~RxTAEGNrdaZio~CQG3FaapEtfECmBmd0c7m-SiBRPri9SmPICqX-MDsiRCYHcq0fDZvKA9JsO3hkUlMuORcGboKiwH-GTVwEQJbPD0iWw9G2r-2XGVa~fnTcHirVPVugHN958b-3XyhyJ8D4NmI28FmJJH-UC1vWkIg1pN5PRL5SkB1yKYVI9wZCmATyFcnefLLt7xZYE-OWPg~O36RV-0jM7u4Ci5MFGp-Vg0nVWg93IM19PA~xOwOJVmteVBgzn3CMsq5D6YzxlehZSVb9lBm7tC-UrsR965enQ__&Key-Pair-Id=APKAJTL2V
2 = {LinkedHashMap$LinkedHashMapEntry@7686} "data" -> "[{"id":97102,"originalUrl":"https:\/\/nixplay-rnd-original.s3.amazonaws.com\/2018\/2018_106533b2bcc1d60a771406bf10759b58.jpg?Signature=i8sm0LBSLjXFKmNcuukaj8Oyb38%3D&Expires=1496224800&AWSAccessKeyId=AKIAIDQQLEZE235GX5QQ&response-expires=Wed%2C%2031%20May%202017%2010%3A00%3A00%20GMT&response-cache-control=max-age%3D86400","caption":" "},{"id":97103,"originalUrl":"https:\/\/nixplay-rnd-original.s3.amazonaws.com\/2018\/2018_fc988f1af1e59ee04104c43c775c3bb7.jpg?Signature=OxClr2%2BSkv7GhCTFtxbLg%2BPsmsg%3D&Expires=1496224800&AWSAccessKeyId=AKIAIDQQLEZE235GX5QQ&response-expires=Wed%2C%2031%20May%202017%2010%3A00%3A00%20GMT&response-cache-control=max-age%3D86400","caption":" "},{"id":97101,"originalUrl":"https:\/\/nixplay-rnd-original.s3.amazonaws.com\/2018\/2018_841eb5e47fd28b76631f0ca099b7d784.jpg?Signature=Y9ge0ebDf3wQ58U2Mx%2BR1oZvFQc%3D&Expires=1496224800&AWSAccessKeyId=AKIAIDQQLEZE235GX5QQ&response-expires=Wed%2C%2031%20May%202017%2010%3A00%3A00%20GMT&response-cache-control=
3 = {LinkedHashMap$LinkedHashMapEntry@7687} "captions" -> "[" "," "," Hello Pug"]"
4 = {LinkedHashMap$LinkedHashMapEntry@7688} "id" -> "14939"
5 = {LinkedHashMap$LinkedHashMapEntry@7689} "name" -> "1 album"
6 = {LinkedHashMap$LinkedHashMapEntry@7690} "count" -> "3"
7 = {LinkedHashMap$LinkedHashMapEntry@7691} "type" -> "album"
8 = {LinkedHashMap$LinkedHashMapEntry@7692} "albumType" -> "Web"
9 = {LinkedHashMap$LinkedHashMapEntry@7693} "actionSheet" -> "[{"action":"add","label":"Add photos"},{"action":"select","label":"Select photos"},{"action":"addToPlaylist","label":"Add Album to Playlist"},{"action":"rename","label":"Edit Album Name"},{"action":"delete","label":"Delete Album"}]"
* */

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
            this.callbackContext = callbackContext;

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long totalMegs = mi.totalMem / 1048576L;
            System.out.println("[NIX] totalMegs: " + totalMegs);

            Intent intent = new Intent(cordova.getActivity(), PhotoBrowserPluginActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("options",options.toString());
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            JSONObject res = new JSONObject();
            this.callbackContext.success(res);
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            if (error == null)
                this.callbackContext.error("Error");
            this.callbackContext.error(error);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            if (res == null)
                this.callbackContext.error("Cancel");
            this.callbackContext.success(res);
        } else {
            this.callbackContext.error("error");
        }

    }
}
