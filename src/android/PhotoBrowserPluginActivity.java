package com.creedon.cordova.plugin.photobrowser;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.creedon.androidphotobrowser.PhotoBrowserActivity;
import com.creedon.androidphotobrowser.PhotoBrowserBasicActivity;
import com.creedon.androidphotobrowser.common.data.models.CustomImage;
import com.creedon.androidphotobrowser.common.views.ImageOverlayView;
import com.creedon.cordova.plugin.photobrowser.data.Demo;
import com.facebook.drawee.backends.pipeline.Fresco;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhotoBrowserPluginActivity extends PhotoBrowserActivity implements PhotoBrowserBasicActivity.PhotoBrowserListener, ImageOverlayView.ImageOverlayVieListener {
    private ArrayList<String> _previewUrls;
    private ArrayList<String> _thumbnailUrls;
    private ArrayList<String> _captions;
    private ArrayList<JSONObject> _data;
    private String name;
    private FakeR f;
    private Context context;
    private JSONArray actionSheet;
    final private static String DEFAULT_ACTION_ADD = "add";
    final private static String DEFAULT_ACTION_SELECT = "select";
    final private static String DEFAULT_ACTION_ADDTOPLAYLIST = "addToPlaylist";
    final private static String DEFAULT_ACTION_RENAME = "rename";
    final private static String DEFAULT_ACTION_DELETE = "delete";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this);
        }
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {

        if (!selectionMode) {
            //TODO build aaction menu from custom data
            for (int i = 0; i < actionSheet.length(); i++) {
                try {
                    JSONObject object = actionSheet.getJSONObject(i);
                    String label = object.getString("label");
                    String action = object.getString("action");
                    menu.add(0, i, 1, label);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            setupToolBar();
        } else {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(com.creedon.androidphotobrowser.R.menu.menu, menu);
            setupToolBar();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if ( id == android.R.id.home) {
            if (!selectionMode) {
                finish();
            }

        } else if (id == com.creedon.androidphotobrowser.R.id.delete) {
            try {
                deletePhotos();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //TODO delete item
        } else if (id == com.creedon.androidphotobrowser.R.id.send) {
            addAlbumToPlaylist();
        } else if (id == com.creedon.androidphotobrowser.R.id.download) {
            downloadPhotos();
        } else if (item.getTitle() != null) {

            for (int i = 0; i < actionSheet.length(); i++) {
                try {
                    JSONObject object = actionSheet.getJSONObject(i);
                    String label = object.getString("label");
                    if(label.equals(item.getTitle())) {
                        String action = object.getString("action");
                        if(action.equals(DEFAULT_ACTION_ADD)){
                            addPhotos();
                        }else if(action.equals(DEFAULT_ACTION_RENAME)){
                            editAlbumName();
                        }else if(action.equals(DEFAULT_ACTION_ADDTOPLAYLIST)){
                            addAlbumToPlaylist();
                        }else if(action.equals(DEFAULT_ACTION_DELETE)){
                            deleteAlbum();
                        }else if(action.equals(DEFAULT_ACTION_SELECT)){
                            setupSelectionMode(!selectionMode);
                        }
                        break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
        return false;

    }

    @Override
    protected void init() {
        f = new FakeR(getApplicationContext());
        context = getApplicationContext();


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
                name = jsonObject.getString("name");
                int count = jsonObject.getInt("count");
                String type = jsonObject.getString("type");
                try {
                    actionSheet = jsonObject.getJSONArray("actionSheet");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                _previewUrls = new ArrayList<String>();
                _thumbnailUrls = new ArrayList<String>();
                _captions = new ArrayList<String>();
                _data = new ArrayList<JSONObject>();
                for (int i = 0; i < images.length(); i++) {
                    _previewUrls.add(images.getString(i));
                }
                for (int i = 0; i < thumbnails.length(); i++) {
                    _thumbnailUrls.add(thumbnails.getString(i));
                }
                for (int i = 0; i < captions.length(); i++) {
                    _captions.add(captions.getString(i));
                }
                for (int i = 0; i < data.length(); i++) {
                    _data.add(data.getJSONObject(i));
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {

            String jsonString = Demo.getFlickrs();
            try {

                JSONArray array = new JSONArray(jsonString);
                _previewUrls = new ArrayList<String>();
                _thumbnailUrls = new ArrayList<String>();
                for (int i = 0; i < array.length(); i++) {
                    _previewUrls.add(array.getJSONObject(i).getString("previewUrl"));
                    _thumbnailUrls.add(array.getJSONObject(i).getString("thumbnailUrl"));
                }
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
        return this.name;
    }

    @Override
    public String getSubtitle() {
        if (this._previewUrls == null) {
            return new StringBuilder()
                    .append(0)
                    .append(" ")
                    .append(context.getResources().getString(f.getId("string", "PHOTOS")))
                    .toString();
        } else {
            return new StringBuilder()
                    .append(this._previewUrls.size())
                    .append(" ")
                    .append(context.getResources().getString(f.getId("string", "PHOTOS")))
                    .toString();
        }
    }

    @Override
    public List<CustomImage> getCustomImages(PhotoBrowserActivity photoBrowserActivity) {
        try {
            List<CustomImage> images = new ArrayList<CustomImage>();
            ArrayList<String> previewUrls = (ArrayList<String>) listener.photoBrowserPhotos(this);

            ArrayList<String> captions = (ArrayList<String>) listener.photoBrowserPhotoCaptions(this);


            try {
                for (int i = 0; i < previewUrls.size(); i++) {
                    images.add(new CustomImage(previewUrls.get(i), captions.get(i)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return images;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void addPhotos() {

    }

    private void addAlbumToPlaylist() {

    }

    private void editAlbumName() {

    }

    private void deleteAlbum() {

    }

    private void downloadPhotos() {

    }

    private void deletePhotos() throws JSONException {
//TODO delete photos from list
        ArrayList<String> fetchedDatas = new ArrayList<String>();
        ArrayList<JSONObject> tempDatas = new ArrayList<JSONObject>();
        ArrayList<String> tempPreviews = new ArrayList<String>();
        ArrayList<String> tempCations = new ArrayList<String>();
        ArrayList<String> tempThumbnails = new ArrayList<String>();
        for(int i = 0 ; i < selections.length ; i++){
            //add to temp lsit if not selected
            if(selections[i].equals("0")){
                tempDatas.add(_data.get(i));
                tempPreviews.add(_previewUrls.get(i));
                tempCations.add(_captions.get(i));
                tempThumbnails.add(_thumbnailUrls.get(i));
            }else{
                JSONObject object = _data.get(i);
                String id = object.getString("id");

                fetchedDatas.add(id);
            }
        }
        _data = (ArrayList<JSONObject>) tempDatas.clone();
        _previewUrls = (ArrayList<String>) tempPreviews.clone();
        _captions = (ArrayList<String>) tempCations.clone();
        _thumbnailUrls = (ArrayList<String>) tempThumbnails.clone();

    }

    private void deletePhoto() {

//TODO delete photo from list
//        delete image from liste , dismiss image viewer, reload again
    }

    @Override
    public void onDownloadButtonPressed(JSONObject data) {

    }

    @Override
    public void onTrashButtonPressed(JSONObject data) {

    }
}
