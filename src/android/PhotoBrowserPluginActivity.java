package com.creedon.cordova.plugin.photobrowser;

import android.os.Bundle;

import com.creedon.androidphotobrowser.PhotoBrowserActivity;
import com.creedon.androidphotobrowser.PhotoBrowserBasicActivity;
import com.creedon.cordova.plugin.photobrowser.data.Demo;
import com.facebook.drawee.backends.pipeline.Fresco;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        super.onCreate(savedInstanceState);

    }
    @Override
    protected void init(){



        listener = this;

        if (getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            String optionsJsonString = bundle.getString("options");
            try {
                JSONObject jsonObject = new JSONObject(optionsJsonString);
                JSONArray images = jsonObject.getJSONArray("images");
                JSONArray thumbnails = jsonObject.getJSONArray("thumbnails");
                JSONArray data = jsonObject.getJSONArray("data");
                JSONArray captions = jsonObject.getJSONArray("captions");
                String id = jsonObject.getString("id");
                String name = jsonObject.getString("name");
                int count = jsonObject.getInt("count");
                String type = jsonObject.getString("type");
                String albumType = jsonObject.getString("albumType");
                JSONArray actionSheet = jsonObject.getJSONArray("actionSheet");

                _previewUrls = new ArrayList<String>();
                _thumbnailUrls = new ArrayList<String>();

                for (int i = 0; i < images.length(); i++) {
                    _previewUrls.add(images.getString(i));
                }
                for (int i = 0; i < thumbnails.length(); i++) {
                    _thumbnailUrls.add(thumbnails.getString(i));
                }
                for (int i = 0; i < captions.length(); i++) {
                    _captions.add(captions.getString(i));
                }
                

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }else {

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
        }
        super.init();
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
