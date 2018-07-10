package com.creedon.cordova.plugin.photobrowser;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.creedon.androidphotobrowser.PhotoBrowserActivity;
import com.creedon.androidphotobrowser.PhotoBrowserBasicActivity;
import com.creedon.androidphotobrowser.common.data.models.CustomImage;
import com.creedon.androidphotobrowser.common.views.ImageOverlayView;
import com.creedon.cordova.plugin.photobrowser.metadata.ActionSheet;
import com.creedon.cordova.plugin.photobrowser.metadata.Datum;
import com.creedon.cordova.plugin.photobrowser.metadata.PhotoDetail;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.stfalcon.frescoimageviewer.ImageViewer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_ACTION;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_DESCRIPTION;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_PHOTOS;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_TYPE;

public class PhotoBrowserPluginActivity extends PhotoBrowserActivity implements PhotoBrowserBasicActivity.PhotoBrowserListener, ImageOverlayView.ImageOverlayVieListener {
    public static final String TAG = PhotoBrowserPluginActivity.class.getSimpleName();
    public static final float MAX = 100;
    public static final int SAVE_PHOTO = 0x11;
    public static final String KEY_ID = "id";
    private static final String KEY_ALBUM = "album";
    private static final String KEY_TYPE_NIXALBUM = "nixalbum";
    private static final String KEY_TYPE_SOCIAL_ALBUM = "socialAlbum";
    private static final int TAG_SELECT = 0x401;
    private static final int TAG_SELECT_ALL = 0x501;
    private static final int MAX_CHARACTOR = 160;

    private CallerThreadExecutor currentExecutor;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private ArrayList<String> pendingFetchDatas;
    PhotoDetail photoDetail;
    PhotoBrowserPluginActivity.PhotosDownloadListener photosDownloadListener = new PhotosDownloadListener() {
        @Override
        public void onPregress(final float progress) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int v = (int) (progress * MAX);

                    progressDialog.setProgress(v);
                }
            });

        }

        @Override
        public void onComplete() {


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
        }

        @Override
        public void onFailed(Error err) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
        }

    };
    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private View mask;
    private boolean readOnly;
    private Button floatingActionButton;
    private String ctaText;


    interface PhotosDownloadListener {

        void onPregress(float progress);

        void onComplete();

        void onFailed(Error err);
    }

    private static final String KEY_ORIGINALURL = "originalUrl";

    private FakeR f;
    private Context context;

    final private static String DEFAULT_ACTION_ADD = "add";
    final private static String DEFAULT_ACTION_SELECT = "select";
    final private static String DEFAULT_ACTION_ADDTOPLAYLIST = "addToPlaylist";
    final private static String DEFAULT_ACTION_ADDTOFRAME = "addToFrame";
    final private static String DEFAULT_ACTION_RENAME = "rename";
    final private static String DEFAULT_ACTION_DELETE = "delete";

    MaterialDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!Fresco.hasBeenInitialized()) {
            Context context = this;
//            global 3 = new OkHttpClient();
//            ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
//                    .newBuilder(context,globalOkHttpClient3)
//                    .setNetworkFetcher(new OkHttp3NetworkFetcher(globalOkHttpClient3))
//                    .build();
//            Fresco.initialize(context, config);
            Fresco.initialize(this);

        }
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (!readOnly) {
            if (!photoDetail.getType().equals(KEY_TYPE_SOCIAL_ALBUM)) {
                if (!selectionMode) {
                    int index = 0;
                    MenuItem menuItem = menu.add(0, TAG_SELECT, 1, getString(f.getId("string", "SELECT")));
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    setupToolBar();
                } else {
                    if (photoDetail.getType().equals(KEY_TYPE_NIXALBUM)) {
                        MenuItem menuItem = menu.add(0, TAG_SELECT_ALL, 1, getString(f.getId("string", "SELECT_ALL")));
                        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    } else {
                        MenuInflater inflater = getMenuInflater();
                        inflater.inflate(com.creedon.androidphotobrowser.R.menu.menu, menu);
                        setupToolBar();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == SAVE_PHOTO) {

            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) {
                    if (pendingFetchDatas != null) {
                        downloadWithURLS(pendingFetchDatas, pendingFetchDatas.size(), this.photosDownloadListener);
                    }
                }
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            mask.setVisibility(GONE);

        }
        int id = item.getItemId();
        if (id == TAG_SELECT) {
            if (!selectionMode) {
                setupSelectionMode(true);
            }

        } else if (id == TAG_SELECT_ALL) {
            if (item.getTitle().equals(getString(f.getId("string", "SELECT_ALL")))) {
                item.setTitle(getString(f.getId("string", "DESELECT_ALL")));
                for (int i = 0; i < selections.size(); i++) {
                    selections.set(i, "1");
                }
                rcAdapter.notifyDataSetChanged();

            } else {
                item.setTitle(getString(f.getId("string", "SELECT_ALL")));
                for (int i = 0; i < selections.size(); i++) {
                    selections.set(i, "0");
                }
                rcAdapter.notifyDataSetChanged();
            }
            if (photoDetail.getType().equals(KEY_TYPE_NIXALBUM)) {
                floatingActionButton.setEnabled(hasItemSelected());
            }


        } else if (id == android.R.id.home) {
            if (!selectionMode || photoDetail.getType().equals(KEY_TYPE_NIXALBUM)
                    || photoDetail.getType().equals(KEY_TYPE_SOCIAL_ALBUM)) {
                finish();
            }

        } else if (id == com.creedon.androidphotobrowser.R.id.delete) {
            try {
                deletePhotos();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //TODO delete item
        }
//        else if (id == com.creedon.androidphotobrowser.R.id.send) {
////            addAlbumToPlaylist();
//            try {
//                sendPhotos();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        } else if (id == com.creedon.androidphotobrowser.R.id.download) {
//            try {
//                downloadPhotos();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
        else if (item.getTitle() != null) {
            if (photoDetail.getActionSheet() != null) {
                for (ActionSheet actionSheet : photoDetail.getActionSheet()) {

                    String label = actionSheet.getLabel();
                    String action = actionSheet.getAction();
                    if (label.equals(item.getTitle())) {

                        if (action.equals(DEFAULT_ACTION_ADD)) {
                            try {
                                addPhotos();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (action.equals(DEFAULT_ACTION_RENAME)) {
                            editAlbumName();
                        } else if (action.equals(DEFAULT_ACTION_ADDTOPLAYLIST)) {
                            try {
                                addAlbumToPlaylist();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (action.equals(DEFAULT_ACTION_DELETE)) {
                            deleteAlbum();
                        } else if (action.equals(DEFAULT_ACTION_SELECT)) {
                            setupSelectionMode(!selectionMode);
                        } else if (action.equals(DEFAULT_ACTION_ADDTOFRAME)) {
                            try {
                                addAlbumToFrame();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }

                }
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
        return false;

    }

    private void addAlbumToFrame() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(KEY_ACTION, DEFAULT_ACTION_ADDTOFRAME);
        res.put(KEY_ID, photoDetail.getId());
        res.put(KEY_TYPE, photoDetail.getType());
        res.put(KEY_DESCRIPTION, "send photos to destination");
        finishWithResult(res);

    }

    private void selectAlbum() {
        JSONObject res = new JSONObject();
        try {
            String action = photoDetail.getAction();
            res.put(KEY_ACTION, action);
            res.put(KEY_ID, photoDetail.getId());
            res.put(KEY_TYPE, photoDetail.getType());
            res.put(KEY_DESCRIPTION, "add social album");
            finishWithResult(res);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendPhotos(String action) throws JSONException {

        ArrayList<Integer> fetchedDatas = new ArrayList<Integer>();


        for (int i = 0; i < selections.size(); i++) {
            //add to temp lsit if not selected
            if (selections.get(i).equals("1")) {
                JSONObject object = photoDetail.getData().get(i).toJSON();
                String id = object.getString(KEY_ID);
                fetchedDatas.add(Integer.parseInt(id));
            }
        }


        if (fetchedDatas.size() > 0) {
            JSONObject res = new JSONObject();
            try {

                res.put(KEY_PHOTOS, new JSONArray(fetchedDatas));
                res.put(KEY_ACTION, action);
                res.put(KEY_ID, photoDetail.getId());
                res.put(KEY_TYPE, photoDetail.getType());
                res.put(KEY_DESCRIPTION, "send photos to destination");
                finishWithResult(res);
            } catch (JSONException e) {
                e.printStackTrace();

            }
        }
    }

    @Override
    protected void init() {
        f = new FakeR(getApplicationContext());

        progressDialog = new MaterialDialog
                .Builder(this)
                .title(getString(f.getId("string", "DOWNLOADING")))
                .neutralText(getString(f.getId("string", "CANCEL")))
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (currentExecutor != null) {
                            currentExecutor.shutdown();
                        }
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (currentExecutor != null) {
                            currentExecutor.shutdown();
                        }
                    }
                })
                .progress(false, 100, true)
                .build();


        context = getApplicationContext();


        listener = this;

        if (getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            String optionsJsonString = bundle.getString("options");

            photoDetail = PhotoDetail.getInstance();
            if (photoDetail.getImages() == null) {
                finishWithResult(new JSONObject());
            }
            try {
                JSONObject jsonObject = new JSONObject(optionsJsonString);
                if (jsonObject.has("readOnly")) {
                    readOnly = jsonObject.getBoolean("readOnly");
                } else {
                    readOnly = false;
                }
                if(jsonObject.has("ctaText")){
                    ctaText = jsonObject.getString("ctaText");
                }else{
                    ctaText = getString(f.getId("string", photoDetail.getType().equals(KEY_ALBUM) ? "ADD_PHOTOS" : "ADD_PHOTOS_TO_PLAYLIST"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        super.init();
        try {
            if (photoDetail.getType().equals(KEY_TYPE_NIXALBUM)) {
                setupSelectionMode(true);
                findViewById(f.getId("id", "floatingButton")).setVisibility(View.VISIBLE);
            }


            if (photoDetail.getType().equals(KEY_TYPE_SOCIAL_ALBUM)) {
                findViewById(f.getId("id", "floatingButton")).setVisibility(View.VISIBLE);
            }

            View bottomSheet = findViewById(f.getId("id", "bottom_sheet1"));
            mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            mask = findViewById(f.getId("id", "mask"));
            mask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        mask.setVisibility(GONE);
                    }
                }
            });
            mBottomSheetBehavior.setHideable(true);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            TextView titleTextView = (TextView) findViewById(f.getId("id", "titleTextView"));
            titleTextView.setText(getString(f.getId("string", "ADD_PHOTOS")));
            floatingActionButton = (Button) findViewById(f.getId("id", "floatingButton"));
            if (readOnly) {
                try {
                    View scrollView = findViewById(f.getId("id", "scrollView"));
                    scrollView.setVisibility(GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {

                if (photoDetail.getActionSheet() != null) {
                    floatingActionButton.setText(ctaText);
                    floatingActionButton.setVisibility(View.VISIBLE);
                    if (floatingActionButton != null) {
                        floatingActionButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {


                                if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                    mask.setVisibility(View.VISIBLE);
                                } else {
                                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                    mask.setVisibility(GONE);
                                }

                            }
                        });
                    }
                } else {
                    if (photoDetail.getType().equals(KEY_TYPE_NIXALBUM)) {
                        floatingActionButton.setText(ctaText);
                        floatingActionButton.setVisibility(View.VISIBLE);
                        floatingActionButton.setEnabled(false);

                        floatingActionButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    sendPhotos(DEFAULT_ACTION_ADDTOPLAYLIST);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    } else if (photoDetail.getType().equals(KEY_TYPE_SOCIAL_ALBUM)) {
                        floatingActionButton.setText(ctaText);
                        floatingActionButton.setVisibility(View.VISIBLE);
                        floatingActionButton.setEnabled(true);
                        System.out.println(floatingActionButton.isEnabled());

                        floatingActionButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectAlbum();
                            }
                        });
                    } else {
                        floatingActionButton.setVisibility(GONE);
                    }
                }
            }


            LinearLayout sheetLinearLayout = (LinearLayout) findViewById(f.getId("id", "sheetLinearLayout"));
            if (photoDetail.getActionSheet() != null) {
                int index = 0;
                float weight = 1.0f / photoDetail.getActionSheet().size();
                for (ActionSheet actionSheet : photoDetail.getActionSheet()) {
                    try {
                        String label = actionSheet.getLabel();
                        final String action = actionSheet.getAction();
                        Button imageButton = new Button(this, null, android.R.style.Widget_Button_Small);//@android:style/Widget.Button.Small


                        Drawable drawable = null;
                        if (index % 3 == 0) {
                            drawable = getResources().getDrawable(com.creedon.androidphotobrowser.R.drawable.camera);

                        } else if (index % 3 == 1) {
                            drawable = getResources().getDrawable(com.creedon.androidphotobrowser.R.drawable.photolibrary);
                        } else if (index % 3 == 2) {
                            drawable = getResources().getDrawable(com.creedon.androidphotobrowser.R.drawable.nixplayalbum);
                        }
                        if (drawable != null) {

                            int h = drawable.getIntrinsicHeight();
                            int w = drawable.getIntrinsicWidth();
                            drawable.setBounds(0, 0, w, h);
                            imageButton.setBackgroundColor(0x00000000);
                            imageButton.setCompoundDrawables(null, drawable, null, null);
                            imageButton.setText(label);
                            imageButton.setTextSize(16);
                            imageButton.setTextColor(0x4A4A4A);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                imageButton.setTextAppearance(f.getId("style", "AppTheme"));
                                imageButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                    imageButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                }
                                imageButton.setTextAppearance(this, f.getId("style", "AppTheme"));
                            }

                        }
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
                        layoutParams.setMargins(20, 0, 0, 0);
                        imageButton.setLayoutParams(layoutParams);

                        imageButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                JSONObject res = new JSONObject();
                                try {
                                    res.put(KEY_ACTION, action);
                                    res.put(KEY_ID, photoDetail.getId());
                                    res.put(KEY_TYPE, photoDetail.getType());
                                    res.put(KEY_DESCRIPTION, "add photo to album");
                                    finishWithResult(res);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                mask.setVisibility(GONE);

                            }
                        });

                        sheetLinearLayout.addView(imageButton);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    index++;
                    if (index == 3)
                        break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<String> photoBrowserPhotos(PhotoBrowserBasicActivity activity) {
        return photoDetail.getImages();
    }

    @Override
    public List<String> photoBrowserThumbnails(PhotoBrowserBasicActivity activity) {
        return photoDetail.getThumbnails();
    }

    @Override
    public String photoBrowserPhotoAtIndex(PhotoBrowserBasicActivity activity, int index) {
        return null;
    }

    @Override
    public List<String> photoBrowserPhotoCaptions(PhotoBrowserBasicActivity photoBrowserBasicActivity) {
        return photoDetail.getCaptions();
    }

    @Override
    public String getActionBarTitle() {
        return photoDetail.getName();
    }

    @Override
    public String getSubtitle() {
        if (photoDetail.getImages() == null) {
            return "0" +
                    " " +
                    context.getResources().getString(f.getId("string", "PHOTOS"));
        } else {
            return String.valueOf(this.photoDetail.getImages().size()) +
                    " " +
                    context.getResources().getString(f.getId("string", "PHOTOS"));
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

    @Override
    protected ImageViewer.OnImageChangeListener getImageChangeListener() {
        return new ImageViewer.OnImageChangeListener() {
            @Override
            public void onImageChange(int position) {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    mask.setVisibility(GONE);
                }
                setCurrentPosition(position);
                String caption = photoDetail.getCaptions().get(position);
                if (photoDetail.getType().equals(KEY_ALBUM)) {

                    if (caption.equals("") || caption.equals(" ")) {
                        caption = getString(f.getId("string", "ADD_CAPTION"));
                    }
                }
                overlayView.setDescription(caption, photoDetail.getCaptions().get(position));

                try {
                    overlayView.setData(photoDetail.getData().get(position).toJSON());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }


    private void addPhotos() throws JSONException {
//dismiss and send code
        JSONObject res = new JSONObject();
        res.put(KEY_ACTION, DEFAULT_ACTION_ADD);
        res.put(KEY_ID, photoDetail.getId());
        res.put(KEY_TYPE, photoDetail.getType());
        res.put(KEY_DESCRIPTION, "add photo to album");
        finishWithResult(res);
    }

    private void addAlbumToPlaylist() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(KEY_ACTION, DEFAULT_ACTION_ADDTOPLAYLIST);
        res.put(KEY_ID, photoDetail.getId());
        res.put(KEY_TYPE, photoDetail.getType());
        res.put(KEY_DESCRIPTION, "send photos to destination");
        finishWithResult(res);


    }

    private void editAlbumName() {

        new MaterialDialog.Builder(this)
                .title(getString(f.getId("string", photoDetail.getType().toLowerCase().equals(KEY_ALBUM) ? "EDIT_ALBUM_NAME" : "EDIT_PLAYLIST_NAME")))
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(f.getId("string", "EDIT_ALBUM_NAME")), photoDetail.getName(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, @NonNull CharSequence input) {

                        dialog.dismiss();
                        photoDetail.setName(input.toString());
                        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setTitle(input.toString());
                        }
                        photoDetail.onSetName(input.toString());
                    }
                }).show();

    }

    private void deleteAlbum() {
        String content = getString(f.getId("string", "ARE_YOU_SURE_YOU_WANT_TO_DELETE_THIS_ALBUM_THIS_WILL_ALSO_REMOVE_THE_PHOTOS_FROM_THE_PLAYLIST_IF_THEY_ARE_NOT_IN_ANY_OTHER_ALBUMS"));
        content.replace(KEY_ALBUM, photoDetail.getType());

        new MaterialDialog.Builder(this)
                .title(getString(f.getId("string", "DELETE")) + photoDetail.getType())
                .content(content)
                .positiveText(getString(f.getId("string", "CONFIRM")))
                .negativeText(getString(f.getId("string", "CANCEL")))
                .btnStackedGravity(GravityEnum.CENTER)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();

                        try {
                            JSONObject res = new JSONObject();
                            res.put(KEY_ACTION, DEFAULT_ACTION_DELETE);
                            res.put(KEY_ID, photoDetail.getId());
                            res.put(KEY_TYPE, photoDetail.getType());
                            res.put(KEY_DESCRIPTION, "delete " + photoDetail.getType());
                            finishWithResult(res);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();

    }

    private void downloadPhotos() throws JSONException {
//TODO download photos

        ArrayList<String> fetchedDatas = new ArrayList<String>();


        for (int i = 0; i < selections.size(); i++) {
            //add to temp lsit if not selected
            if (selections.get(i).equals("1")) {
                JSONObject object = photoDetail.getData().get(i).toJSON();
                String id = object.getString(KEY_ORIGINALURL);
                fetchedDatas.add(id);
            }
        }


        if (fetchedDatas.size() > 0) {

            progressDialog.setProgress(0);
            progressDialog.show();
            downloadWithURLS(fetchedDatas, fetchedDatas.size(), this.photosDownloadListener);
        }

    }

    private void deletePhotos() throws JSONException {

        final ArrayList<String> fetchedDatas = new ArrayList<String>();
        final ArrayList<Datum> tempDatas = new ArrayList<Datum
                >();
        final ArrayList<String> tempPreviews = new ArrayList<String>();
        final ArrayList<String> tempCations = new ArrayList<String>();
        final ArrayList<String> tempThumbnails = new ArrayList<String>();
        final ArrayList<String> tempSelection = new ArrayList<String>();
        final ArrayList<Integer> tempSelected = new ArrayList<Integer>();
        for (int i = 0; i < selections.size(); i++) {
            //add to temp lsit if not selected
            if (selections.get(i).equals("0")) {
                tempDatas.add(photoDetail.getData().get(i));
                tempPreviews.add(photoDetail.getImages().get(i));
                tempCations.add(photoDetail.getCaptions().get(i));
                tempThumbnails.add(photoDetail.getThumbnails().get(i));
                tempSelection.add(selections.get(i));
            } else {
                Datum object = photoDetail.getData().get(i);
                String id = object.getId();

                fetchedDatas.add(id);
                tempSelected.add(i);
            }
        }
        if (fetchedDatas.size() > 0) {

            new MaterialDialog.Builder(this)
                    .title(getString(f.getId("string", "DELETE_PHOTOS")))
                    .content(getString(f.getId("string", "ARE_YOU_SURE_YOU_WANT_TO_DELETE_THE_SELECTED_PHOTOS")))
                    .positiveText(getString(f.getId("string", "CONFIRM")))
                    .negativeText(getString(f.getId("string", "CANCEL")))
                    .btnStackedGravity(GravityEnum.CENTER)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();

                            photoDetail.onPhotoDeleted(fetchedDatas);

                            photoDetail.setImages(tempPreviews);
                            photoDetail.setData(tempDatas);
                            photoDetail.setCaptions(tempCations);
                            photoDetail.setThumbnails(tempThumbnails);
                            selections = tempSelection;
                            if (photoDetail.getImages().size() == 0) {

                                finish();
                            } else {
                                ArrayList<String> list = new ArrayList<String>(photoDetail.getThumbnails());
                                refreshCustomImage();
                                Iterator<Integer> it = tempSelected.iterator();

                                getRcAdapter().swap(list);
                            }
                            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                            if (actionBar != null) {
                                actionBar.setSubtitle(listener.getSubtitle());
                            }
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }


//        todo notify changed
    }

    private void deletePhoto(int position, JSONObject data) {
        try {
            Datum deletedData = photoDetail.getData().remove(position);
            photoDetail.getImages().remove(position);
            photoDetail.getCaptions().remove(position);
            photoDetail.getThumbnails().remove(position);
            selections.remove(position);
            ArrayList<String> deletedDatas = new ArrayList<String>();
            deletedDatas.add(deletedData.getId());
            photoDetail.onPhotoDeleted(deletedDatas);
            if (photoDetail.getImages().size() == 0) {
                finish();
            } else {

                imageViewer.onDismiss();
                super.refreshCustomImage();
                ArrayList<String> list = new ArrayList<String>(photoDetail.getThumbnails());
//                getRcAdapter().swap(list);
                getRcAdapter().remove(position, list);

                android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setSubtitle(listener.getSubtitle());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        todo notify changed

    }

    private void downloadWithURLS(final ArrayList<String> fetchedDatas, final int counts, final PhotosDownloadListener _photosDownloadListener) {
        Log.d(TAG, "going to download " + fetchedDatas.get(0));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, SAVE_PHOTO);
            }
            pendingFetchDatas = fetchedDatas;
            return;
        }
        downloadPhotoWithURL(fetchedDatas.get(0), new PhotosDownloadListener() {
            @Override
            public void onPregress(float progress) {
                float particialProgress = ((1.0f / counts) * progress);
                Log.d(TAG, "particialProgress " + particialProgress);
                float curentsize = fetchedDatas.size();
                float partition = (counts - curentsize);
                Log.d(TAG, "partition " + partition);
                float PROGRESS = (partition / counts) + particialProgress;
                Log.d(TAG, "Progress " + PROGRESS);
                _photosDownloadListener.onPregress(PROGRESS);
            }

            @Override
            public void onComplete() {
                fetchedDatas.remove(0);
                if (fetchedDatas.size() > 0) {
                    downloadWithURLS(fetchedDatas, counts, _photosDownloadListener);
                } else {
                    _photosDownloadListener.onComplete();
                }
            }

            @Override
            public void onFailed(Error err) {
                _photosDownloadListener.onFailed(err);
            }
        });
    }


    @Override
    public void onDownloadButtonPressed(JSONObject data) {
        //Save image to camera roll
        try {
            if (data.getString(KEY_ORIGINALURL) != null) {

                progressDialog.setProgress(0);
                progressDialog.show();
                ArrayList<String> fetchedDatas = new ArrayList<String>();
                fetchedDatas.add(data.getString(KEY_ORIGINALURL));
                downloadWithURLS(fetchedDatas, fetchedDatas.size(), this.photosDownloadListener);

            }
        } catch (JSONException e) {

            e.printStackTrace();
        }
    }

    private void downloadPhotoWithURL(String string, @NonNull final PhotosDownloadListener photosDownloadListener) {
        final Uri uri = Uri.parse(string);
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .build();

        DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline()
                .fetchDecodedImage(request, this);
        CallerThreadExecutor executor = CallerThreadExecutor.getInstance();
        currentExecutor = executor;
        dataSource.subscribe(
                new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
                    @Override
                    protected void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                        currentExecutor = null;
                        if (!dataSource.isFinished()) {
                            return;
                        }

                        CloseableReference<CloseableImage> closeableImageRef = dataSource.getResult();
                        Bitmap bitmap = null;
                        if (closeableImageRef != null &&
                                closeableImageRef.get() instanceof CloseableBitmap) {
                            bitmap = ((CloseableBitmap) closeableImageRef.get()).getUnderlyingBitmap();
                        }

                        try {
                            String filePath = getPicturesPath(uri.toString());
                            File file = new File(filePath);
                            OutputStream outStream = null;
                            try {
                                outStream = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100,
                                        outStream);
//                                MediaStore.Images.Media.insertImage(getContentResolver(),
//                                        file.getAbsolutePath(), file.getName(), file.getName());
                                galleryAddPic(file);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                Error error = new Error(e.getMessage());
                                photosDownloadListener.onFailed(error);
                            } finally {
                                try {
                                    if(outStream != null) {
                                        outStream.flush();
                                        outStream.close();
                                    }
                                } catch (IOException e) {
                                    Error error = new Error(e.getMessage());
                                    photosDownloadListener.onFailed(error);
                                    e.printStackTrace();
                                }

                            }
                            photosDownloadListener.onComplete();
                            //TODO notify file saved

                        } finally {
                            CloseableReference.closeSafely(closeableImageRef);
                        }
                    }

                    @Override
                    protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                        //TODO notify failed download
                        Error err = new Error("Failed to download");

                        photosDownloadListener.onFailed(err);
                        currentExecutor = null;
                    }

                    @Override
                    public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {
                        boolean isFinished = dataSource.isFinished();
                        float progress = dataSource.getProgress();
                        photosDownloadListener.onPregress(progress);

                    }


                }
                , executor);
    }

    public void galleryAddPic(File f) {
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        sendBroadcast(mediaScanIntent);
    }

    public String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    private String getPicturesPath(String urlString) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //TODO dirty handle, may find bettery way handel data type
        int slashIndex = urlString.lastIndexOf("/");
        int jpgIndex = urlString.indexOf("?");
        String fileName = "";
        if (slashIndex > 0 && jpgIndex > 0) {
            fileName = urlString.substring(slashIndex + 1, jpgIndex);
        }
        String imageFileName = (!fileName.equals("")) ? fileName : "IMG_" + timeStamp + (urlString.contains(".jpg") ? ".jpg" : ".png");

        final String appDirectoryName = getApplicationName(getApplicationContext());

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appDirectoryName);
        String galleryPath = storageDir.getAbsolutePath() + "/" + imageFileName;
        File parentFolder = new File(galleryPath);
        if (!parentFolder.getParentFile().exists()) {
            parentFolder.getParentFile().mkdir();
        }
        return galleryPath;
    }

    @Override
    public void onTrashButtonPressed(final JSONObject data) {

        new MaterialDialog.Builder(this)
                .title(getString(f.getId("string", "DELETE_PHOTOS")))
                .content(getString(f.getId("string", "ARE_YOU_SURE_YOU_WANT_TO_DELETE_THE_SELECTED_PHOTOS")))
                .positiveText(getString(f.getId("string", "CONFIRM")))
                .negativeText(getString(f.getId("string", "CANCEL")))

                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        deletePhoto(getCurrentPosition(), data);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();

    }

    @Override
    public void onCaptionChanged(JSONObject data, String caption) {
        //TODO send caption
        photoDetail.setCaption(getCurrentPosition(), caption);
        overlayView.setDescription(caption, caption);
        if (photoDetail.setCaption(getCurrentPosition(), caption)) {

        } else {
            Log.e(TAG, "Error failed to set caption");
        }

    }

    @Override
    public void onCloseButtonClicked() {
        imageViewer.onDismiss();
    }

//    @Override
//    public void onEditButtonClick(JSONObject data) {
//        ArrayList<String> ids = new ArrayList<String>();
//        try {
//            ids.add(data.getString("id"));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        JSONObject res = new JSONObject();
//        try {
//
//            res.put(KEY_PHOTOS, new JSONArray(ids));
//            res.put(KEY_ACTION, KEY_ACTION_SEND);
//            res.put(KEY_ID, photoDetail.getId());
//            res.put(KEY_TYPE, photoDetail.getType());
//            res.put(KEY_DESCRIPTION, "send photos to destination");
//            finishWithResult(res);
//        } catch (JSONException e) {
//            e.printStackTrace();
//
//        }
//    }

    @Override
    public void didEndEditing(JSONObject data, String s) {
        photoDetail.setCaption(getCurrentPosition(), s);
    }

    @Override
    public void onInitTextView(final MaterialEditText editText) {

        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(MAX_CHARACTOR);
        editText.setFilters(filterArray);
        editText.setFloatingLabelText(getString(f.getId("string", "ADD_CAPTION")));
        editText.setHint(getString(f.getId("string", "ADD_CAPTION")));
        editText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        editText.setFloatingLabel(MaterialEditText.FLOATING_LABEL_HIGHLIGHT);
                        editText.setFloatingLabelText(getString(f.getId("string", "ADD_CAPTION")) + "(" + charSequence.length() + "/" + MAX_CHARACTOR + ")");
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                }
        );

    }

    @Override
    public int downloadButtonVisiblity() {
        return photoDetail.getType().equals(KEY_ALBUM) ? View.VISIBLE : GONE;
    }

    @Override
    public int trashButtonVisiblity() {
        return (!readOnly) ? VISIBLE : GONE;
    }

    void finishWithResult(JSONObject result) {
        Bundle conData = new Bundle();
        conData.putString(Constants.RESULT, result.toString());
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    public int getOrientation(int currentPosition) {
        List<Datum> datums = photoDetail.getData();
        int orientation = 0;
        if (datums != null) {
            if (datums.size() > currentPosition) {
                Datum datum = datums.get(currentPosition);
                orientation = datum.getOrientation();
            }
        }
        return exifToDegrees(orientation);
    }

    @Override
    protected ImageViewer.OnOrientationListener getOrientationListener() {
        return new ImageViewer.OnOrientationListener() {
            @Override
            public int OnOrientaion(int currentPosition) {
                List<Datum> datums = photoDetail.getData();
                int orientation = 0;
                if (datums != null) {
                    if (datums.size() > currentPosition) {
                        Datum datum = datums.get(currentPosition);
                        orientation = datum.getOrientation();
                    }
                }
                return exifToDegrees(orientation);

            }
        };
    }

    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        } else {
            return 0;
        }
    }

    @Override
    protected void showPicker(int startPosition) {
        isDialogShown = true;
        currentPosition = startPosition;
        if (photoDetail.getType().equals(KEY_ALBUM)) {
            overlayView = new ImageOverlayView(this);
        } else {
            overlayView = new CustomeImageOverlayView(this);
        }
        imageViewer = new ImageViewer.Builder<String>(this, posters)
                .setOverlayView(overlayView)
                .setStartPosition(startPosition)
                .setImageChangeListener(getImageChangeListener())
                .setOnDismissListener(getDismissListener())
                .setOnOrientationListener(getOrientationListener())
                .show();
    }

    @Override
    public void onItemClick(View view, int position) {
        super.onItemClick(view, position);
        //check any possitive value
        if (photoDetail.getType().equals(KEY_TYPE_NIXALBUM)) {
            floatingActionButton.setEnabled(hasItemSelected());
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        super.onItemLongClick(view, position);
        if (photoDetail.getType().equals(KEY_TYPE_NIXALBUM)) {
            floatingActionButton.setEnabled(hasItemSelected());
        }
    }

    boolean hasItemSelected() {
        for (int i = 0; i < selections.size(); i++) {
            //add to temp lsit if not selected
            if (selections.get(i).equals("1")) {
                return true;
            }
        }
        return false;
    }
}
