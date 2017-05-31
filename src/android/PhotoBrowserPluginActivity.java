package com.creedon.cordova.plugin.photobrowser;

import android.os.Bundle;

import com.creedon.androidphotobrowser.PhotoBrowserActivity;
import com.creedon.androidphotobrowser.PhotoBrowserBasicActivity;
import com.creedon.cordova.plugin.photobrowser.data.Demo;
import com.facebook.drawee.backends.pipeline.Fresco;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhotoBrowserPluginActivity extends PhotoBrowserActivity implements PhotoBrowserBasicActivity.PhotoBrowserListener {
    private ArrayList<String> _previewUrls;
    private ArrayList<String> _thumbnailUrls;
    private ArrayList<String> _captions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this);
        }

        listener = this;



        String jsonString = Demo.getFlickrs();
        try {

            JSONArray array = new JSONArray(jsonString);
            _previewUrls = new ArrayList<String>();
            _thumbnailUrls = new ArrayList<String>();
            for (int i = 0; i < array.length(); i++) {
                _previewUrls.add(array.getJSONObject(i).getString("previewUrl"));
                _thumbnailUrls.add(array.getJSONObject(i).getString("thumbnailUrl"));
            }
//            rowListItem = thumbnailUrls;
//            posters = previewUrls.toArray(new String[0]);
            _captions = new ArrayList<String>(Arrays.asList(Demo.getDescriptions()));

        } catch (JSONException e) {
            e.printStackTrace();

        }
        super.onCreate(savedInstanceState);

    }
    @Override
    public List<String> photoBrowserPhotos(PhotoBrowserBasicActivity activity) {
        return _previewUrls;
    }

    @Override
    public List<String> photoBrowserThumbnails(PhotoBrowserBasicActivity activity) {
        return _thumbnailUrls;
    }

    @Override
    public String photoBrowserPhotoAtIndex(PhotoBrowserBasicActivity activity, int index) {
        return null;
    }

    @Override
    public List<String> photoBrowserPhotoCaptions(PhotoBrowserBasicActivity photoBrowserBasicActivity) {
        return _captions;
    }
    @Override
    public String getActionBarTitle() {
        return "Album";
    }
    @Override
    public String getSubtitle() {
        return "Subtitle";
    }
}
