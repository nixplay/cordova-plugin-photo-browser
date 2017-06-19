package com.creedon.cordova.plugin.photobrowser;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.creedon.androidphotobrowser.PhotoBrowserActivity;
import com.creedon.androidphotobrowser.PhotoBrowserBasicActivity;
import com.creedon.androidphotobrowser.common.data.models.CustomImage;
import com.creedon.androidphotobrowser.common.views.ImageOverlayView;
import com.creedon.cordova.plugin.photobrowser.data.ActionSheet;
import com.creedon.cordova.plugin.photobrowser.data.Datum;
import com.creedon.cordova.plugin.photobrowser.data.PhotoData;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
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
import java.util.List;

import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_ACTION;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_ACTION_SEND;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_DESCRIPTION;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_PHOTOS;
import static com.creedon.cordova.plugin.photobrowser.PhotoBrowserPlugin.KEY_TYPE;

public class PhotoBrowserPluginActivity extends PhotoBrowserActivity implements PhotoBrowserBasicActivity.PhotoBrowserListener, ImageOverlayView.ImageOverlayVieListener {
    public static final String TAG = PhotoBrowserPluginActivity.class.getSimpleName();
    public static final float MAX = 100;
    public static final int SAVE_PHOTO = 0x11;
    public static final String KEY_ID = "id";
    private static final String KEY_ALBUM = "album";
    private CallerThreadExecutor currentExecutor;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private ArrayList<String> pendingFetchDatas;
    PhotoData photoData;
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
            Fresco.initialize(this);

        }
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {

        if (!selectionMode) {
            //TODO build aaction menu from custom data

            int index = 0;
            for (ActionSheet actionSheet : photoData.getActionSheet()) {

                String label = actionSheet.getLabel();
                String action = actionSheet.getAction();
                menu.add(0, index, 1, label);
                index++;

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
        int id = item.getItemId();
        if (id == android.R.id.home) {
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
//            addAlbumToPlaylist();
            try {
                sendPhotos();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (id == com.creedon.androidphotobrowser.R.id.download) {
            try {
                downloadPhotos();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (item.getTitle() != null) {

            for (ActionSheet actionSheet : photoData.getActionSheet()) {

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
        } else {
            return super.onOptionsItemSelected(item);
        }
        return false;

    }

    private void addAlbumToFrame() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(KEY_ACTION, DEFAULT_ACTION_ADDTOFRAME);
        res.put(KEY_ID, photoData.getId());
        res.put(KEY_TYPE, photoData.getType());
        res.put(KEY_DESCRIPTION, "send photos to destination");
        finishWithResult(res);

    }


    private void sendPhotos() throws JSONException {

        ArrayList<String> fetchedDatas = new ArrayList<String>();


        for (int i = 0; i < selections.size(); i++) {
            //add to temp lsit if not selected
            if (selections.get(i).equals("1")) {
                JSONObject object = photoData.getData().get(i).toJSON();
                String id = object.getString(KEY_ID);
                fetchedDatas.add(id);
            }
        }


        if (fetchedDatas.size() > 0) {
            JSONObject res = new JSONObject();
            try {

                res.put(KEY_PHOTOS, new JSONArray(fetchedDatas));
                res.put(KEY_ACTION, KEY_ACTION_SEND);
                res.put(KEY_ID, photoData.getId());
                res.put(KEY_TYPE, photoData.getType());
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
            try {
                photoData = PhotoData.getInstance(new JSONObject(optionsJsonString));
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            try {
//                JSONObject jsonObject = new JSONObject(optionsJsonString);
//                JSONArray images = jsonObject.getJSONArray("images");
//                JSONArray thumbnails = jsonObject.getJSONArray("thumbnails");
//                JSONArray data = jsonObject.getJSONArray("data");
//                JSONArray captions = jsonObject.getJSONArray("captions");
//                String id = jsonObject.getString("id");
//                name = jsonObject.getString("name");
//                int count = jsonObject.getInt("count");
//                String type = jsonObject.getString("type");
//                try {
//                    actionSheet = jsonObject.getJSONArray("actionSheet");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                _previewUrls = new ArrayList<String>();
//                _thumbnailUrls = new ArrayList<String>();
//                _captions = new ArrayList<String>();
//                _data = new ArrayList<JSONObject>();
//                for (int i = 0; i < images.length(); i++) {
//                    _previewUrls.add(images.getString(i));
//                }
//                for (int i = 0; i < thumbnails.length(); i++) {
//                    _thumbnailUrls.add(thumbnails.getString(i));
//                }
//                for (int i = 0; i < captions.length(); i++) {
//                    _captions.add(captions.getString(i));
//                }
//                for (int i = 0; i < data.length(); i++) {
//                    _data.add(data.getJSONObject(i));
//                }
//
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }

        }
        super.init();
    }

    @Override
    public List<String> photoBrowserPhotos(PhotoBrowserBasicActivity activity) {
        return photoData.getImages();
    }

    @Override
    public List<String> photoBrowserThumbnails(PhotoBrowserBasicActivity activity) {
        return photoData.getThumbnails();
    }

    @Override
    public String photoBrowserPhotoAtIndex(PhotoBrowserBasicActivity activity, int index) {
        return null;
    }

    @Override
    public List<String> photoBrowserPhotoCaptions(PhotoBrowserBasicActivity photoBrowserBasicActivity) {
        return photoData.getCaptions();
    }

    @Override
    public String getActionBarTitle() {
        return photoData.getName();
    }

    @Override
    public String getSubtitle() {
        if (photoData.getImages() == null) {
            return "0" +
                    " " +
                    context.getResources().getString(f.getId("string", "PHOTOS"));
        } else {
            return String.valueOf(this.photoData.getImages().size()) +
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
                setCurrentPosition(position);
                overlayView.setDescription(photoData.getCaptions().get(position));
                try {
                    overlayView.setData(photoData.getData().get(position).toJSON());
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
        res.put(KEY_ID, photoData.getId());
        res.put(KEY_TYPE, photoData.getType());
        res.put(KEY_DESCRIPTION, "add photo to album");
        finishWithResult(res);
    }

    private void addAlbumToPlaylist() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(KEY_ACTION, DEFAULT_ACTION_ADDTOPLAYLIST);
        res.put(KEY_ID, photoData.getId());
        res.put(KEY_TYPE, photoData.getType());
        res.put(KEY_DESCRIPTION, "send photos to destination");
        finishWithResult(res);


    }

    private void editAlbumName() {

        new MaterialDialog.Builder(this)
                .title(getString(f.getId("string", photoData.getType().toLowerCase().equals(KEY_ALBUM) ? "EDIT_ALBUM_NAME" : "EDIT_PLAYLIST_NAME")))
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(f.getId("string", "EDIT_ALBUM_NAME")), photoData.getName(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, @NonNull CharSequence input) {

                        dialog.dismiss();
                        photoData.setName(input.toString());
                        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setTitle(input.toString());
                        }
                        photoData.onSetName(input.toString());
                    }
                }).show();

    }

    private void deleteAlbum() {
        String content = getString(f.getId("string", "ARE_YOU_SURE_YOU_WANT_TO_DELETE_THIS_ALBUM_THIS_WILL_ALSO_REMOVE_THE_PHOTOS_FROM_THE_PLAYLIST_IF_THEY_ARE_NOT_IN_ANY_OTHER_ALBUMS"));
        content.replace(KEY_ALBUM, photoData.getType());

        new MaterialDialog.Builder(this)
                .title(getString(f.getId("string", "DELETE")) + photoData.getType())
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
                            res.put(KEY_ID, photoData.getId());
                            res.put(KEY_TYPE, photoData.getType());
                            res.put(KEY_DESCRIPTION, "delete "+photoData.getType());
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
                JSONObject object = photoData.getData().get(i).toJSON();
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
        for (int i = 0; i < selections.size(); i++) {
            //add to temp lsit if not selected
            if (selections.get(i).equals("0")) {
                tempDatas.add(photoData.getData().get(i));
                tempPreviews.add(photoData.getImages().get(i));
                tempCations.add(photoData.getCaptions().get(i));
                tempThumbnails.add(photoData.getThumbnails().get(i));
                tempSelection.add(selections.get(i));
            } else {
                Datum object = photoData.getData().get(i);
                String id = object.getId();

                fetchedDatas.add(id);
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

                            photoData.onPhotoDeleted(fetchedDatas);

                            photoData.setImages(tempPreviews);
                            photoData.setData(tempDatas);
                            photoData.setCaptions(tempCations);
                            photoData.setThumbnails(tempThumbnails);
                            selections = tempSelection;
                            if (photoData.getImages().size() == 0) {

                                finish();
                            } else {
                                ArrayList<String> list = new ArrayList<String>(photoData.getThumbnails());
                                getRcAdapter().swap(list);
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
            Datum deletedData = photoData.getData().remove(position);
            photoData.getImages().remove(position);
            photoData.getCaptions().remove(position);
            photoData.getThumbnails().remove(position);
            selections.remove(position);
            ArrayList<String> deletedDatas = new ArrayList<String>();
            deletedDatas.add(deletedData.getId());
            photoData.onPhotoDeleted(deletedDatas);
            if (photoData.getImages().size() == 0) {
                finish();
            } else {
                imageViewer.onDismiss();
                super.refreshCustomImage();
                showPicker(photoData.getImages().size() == 1 ? 0 : getCurrentPosition() > 0 ? getCurrentPosition() - 1 : getCurrentPosition());
                ArrayList<String> list = new ArrayList<String>(photoData.getThumbnails());
                getRcAdapter().swap(list);


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
                Log.d(TAG,"particialProgress "+particialProgress);
                float curentsize = fetchedDatas.size();
                float partition = (counts - curentsize);
                Log.d(TAG,"partition "+partition);
                float PROGRESS = (partition  / counts )+particialProgress;
                Log.d(TAG,"Progress "+PROGRESS);
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
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                Error error = new Error(e.getMessage());
                                photosDownloadListener.onFailed(error);
                            } finally {
                                try {
                                    outStream.flush();
                                    outStream.close();
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
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        String galleryPath = storageDir.getAbsolutePath() + "/" + imageFileName;
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
        photoData.setCaption(getCurrentPosition(), caption);
        overlayView.setDescription(caption);
        if (photoData.setCaption(getCurrentPosition(), caption)) {

        } else {
            Log.e(TAG, "Error failed to set caption");
        }

    }

    @Override
    public void onCloseButtonClicked() {
        imageViewer.onDismiss();
    }

    @Override
    public void onSendButtonClick(JSONObject data) {
        ArrayList<String> ids = new ArrayList<String>();
        try {
            ids.add(data.getString("id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject res = new JSONObject();
        try {

            res.put(KEY_PHOTOS, new JSONArray(ids));
            res.put(KEY_ACTION, KEY_ACTION_SEND);
            res.put(KEY_ID, photoData.getId());
            res.put(KEY_TYPE, photoData.getType());
            res.put(KEY_DESCRIPTION, "send photos to destination");
            finishWithResult(res);
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    @Override
    public void didEndEditing(JSONObject data, String s) {
        photoData.setCaption(getCurrentPosition(), s);
    }

    void finishWithResult(JSONObject result) {
        Bundle conData = new Bundle();
        conData.putString(Constants.RESULT, result.toString());
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);

        finish();
    }

}
