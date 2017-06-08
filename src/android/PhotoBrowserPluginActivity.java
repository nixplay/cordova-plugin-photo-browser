package com.creedon.cordova.plugin.photobrowser;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.creedon.androidphotobrowser.PhotoBrowserActivity;
import com.creedon.androidphotobrowser.PhotoBrowserBasicActivity;
import com.creedon.androidphotobrowser.common.data.models.CustomImage;
import com.creedon.androidphotobrowser.common.views.ImageOverlayView;
import com.creedon.cordova.plugin.photobrowser.data.Demo;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.flyco.dialog.listener.OnBtnClickL;
import com.flyco.dialog.widget.NormalDialog;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PhotoBrowserPluginActivity extends PhotoBrowserActivity implements PhotoBrowserBasicActivity.PhotoBrowserListener, ImageOverlayView.ImageOverlayVieListener {
    private static final String TAG = PhotoBrowserPluginActivity.class.getSimpleName();
    private static final float MAX = 100;
    private static final int SAVE_PHOTO = 0x11;
    private CallerThreadExecutor currentExecutor;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private ArrayList<String> pendingFetchDatas;

    PhotoBrowserPluginActivity.PhotosDownloadListener photosDownloadListener = new PhotosDownloadListener() {
        @Override
        public void onPregress(final float progress) {
            //TODO handle proess

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int v = (int) (progress * MAX);
                    //TODO can not show progress
                    progressDialog.setProgress(v);
                }
            });

        }

        @Override
        public void onComplete() {
            //TODO handle proess

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
        }

        @Override
        public void onFailed(Error err) {
//TODO handle proess
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

    private static final String LOG_TAG = PhotoBrowserPluginActivity.class.getSimpleName();
    private static final String KEY_ORIGINALURL = "originalUrl";
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
    ProgressDialog progressDialog;

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
            addAlbumToPlaylist();
        } else if (id == com.creedon.androidphotobrowser.R.id.download) {
            try {
                downloadPhotos();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (item.getTitle() != null) {

            for (int i = 0; i < actionSheet.length(); i++) {
                try {
                    JSONObject object = actionSheet.getJSONObject(i);
                    String label = object.getString("label");
                    if (label.equals(item.getTitle())) {
                        String action = object.getString("action");
                        if (action.equals(DEFAULT_ACTION_ADD)) {
                            addPhotos();
                        } else if (action.equals(DEFAULT_ACTION_RENAME)) {
                            editAlbumName();
                        } else if (action.equals(DEFAULT_ACTION_ADDTOPLAYLIST)) {
                            addAlbumToPlaylist();
                        } else if (action.equals(DEFAULT_ACTION_DELETE)) {
                            deleteAlbum();
                        } else if (action.equals(DEFAULT_ACTION_SELECT)) {
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
        progressDialog = new ProgressDialog(this);
        String title = getString(f.getId("string", "DOWNLOADING"));
        progressDialog.setMessage(title);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (currentExecutor != null) {
                    currentExecutor.shutdown();
                }
            }
        });

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

    @Override
    protected ImageViewer.OnImageChangeListener getImageChangeListener() {
        return new ImageViewer.OnImageChangeListener() {
            @Override
            public void onImageChange(int position) {
                CustomImage image = getImages().get(position);
                overlayView.setShareText(image.getUrl());
                overlayView.setDescription(image.getDescription());
                overlayView.setData(_data.get(position));
            }
        };
    }


    private void addPhotos() {
//dismiss and send code

        finishActivity(Constants.RESULT_ADD_PHOTO);
    }

    private void addAlbumToPlaylist() {
//pop up ui for confirmation
    }

    private void editAlbumName() {
//pop up ui for album name edit
    }

    private void deleteAlbum() {
//pop up ui for confirmation
        final NormalDialog dialog = new NormalDialog(this);
        dialog.title(getString(f.getId("string", "DELETE_ALBUM")))
                .content(getString(f.getId("string", "ARE_YOU_SURE_YOU_WANT_TO_DELETE_THIS_ALBUM_THIS_WILL_ALSO_REMOVE_THE_PHOTOS_FROM_THE_PLAYLIST_IF_THEY_ARE_NOT_IN_ANY_OTHER_ALBUMS")))
                .btnNum(2)
                .btnText(getString(f.getId("string", "CONFIRM")),
                        getString(f.getId("string", "CANCEL")))
                .show();

        dialog.setOnBtnClickL(new OnBtnClickL() {
            @Override
            public void onBtnClick() {
                setResult(RESULT_OK);
                finishActivity(Constants.RESULT_DELETE_ALBUM);
                dialog.dismiss();
            }
        }, new OnBtnClickL() {
            @Override
            public void onBtnClick() {

                dialog.dismiss();
            }
        });
//        NormalDialog dialog = new NormalDialog(getApplicationContext());
//        dialog.setTitle(getString(f.getId("string","DELETE_ALBUM")));
//
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialogInterface) {
//
//            }
//        });
//
//        dialog.setOnBtnClickL(new OnBtnClickL() {
//            @Override
//            public void onBtnClick() {
//                setResult(RESULT_OK);
//                finishActivity(Constants.RESULT_DELETE_ALBUM);
//            }
//        });
//        dialog.setCancelable(false);
//        dialog.show();

    }

    private void downloadPhotos() throws JSONException {
//TODO download photos

        ArrayList<String> fetchedDatas = new ArrayList<String>();


        for (int i = 0; i < selections.length; i++) {
            //add to temp lsit if not selected
            if (selections[i].equals("1")) {
                JSONObject object = _data.get(i);
                String id = object.getString(KEY_ORIGINALURL);
                fetchedDatas.add(id);
            }
        }


        if (fetchedDatas.size() > 0) {

            progressDialog.setMax((int) MAX);
            progressDialog.setProgress(0);
            progressDialog.show();
            downloadWithURLS(fetchedDatas, fetchedDatas.size(), this.photosDownloadListener);
        }

    }

    private void deletePhotos() throws JSONException {

        ArrayList<String> fetchedDatas = new ArrayList<String>();
        ArrayList<JSONObject> tempDatas = new ArrayList<JSONObject>();
        ArrayList<String> tempPreviews = new ArrayList<String>();
        ArrayList<String> tempCations = new ArrayList<String>();
        ArrayList<String> tempThumbnails = new ArrayList<String>();
        for (int i = 0; i < selections.length; i++) {
            //add to temp lsit if not selected
            if (selections[i].equals("0")) {
                tempDatas.add(_data.get(i));
                tempPreviews.add(_previewUrls.get(i));
                tempCations.add(_captions.get(i));
                tempThumbnails.add(_thumbnailUrls.get(i));
            } else {
                JSONObject object = _data.get(i);
                String id = object.getString("id");

                fetchedDatas.add(id);
            }
        }
        _previewUrls = tempPreviews;
        _data = tempDatas;
        _captions = tempCations;
        _thumbnailUrls = tempThumbnails;
        if (_previewUrls.size() == 0) {

            finishActivity(-1);
        } else {
            getRcAdapter().swap(_thumbnailUrls);
        }
//        todo notify changed
    }

    private void deletePhoto(int position, JSONObject data) {

        _data.remove(position);
        _previewUrls.remove(position);
        _captions.remove(position);
        _thumbnailUrls.remove(position);

        if (_previewUrls.size() == 0) {
            finishActivity(-1);
        } else {
            imageViewer.onDismiss();
            showPicker(_previewUrls.size() == 1 ? 0 : getCurrentPosition() > 0 ? getCurrentPosition() - 1 : getCurrentPosition());
            ArrayList<String> list = (ArrayList<String>) _thumbnailUrls.clone();
            getRcAdapter().swap(list);
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
                float PROGRESS = (counts - fetchedDatas.size()) / counts + progress;
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

                progressDialog.setMax((int) MAX);
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
    public void onTrashButtonPressed(JSONObject data) {
        deletePhoto(getCurrentPosition(), data);
    }

    @Override
    public void onCaptionchnaged(JSONObject data, String caption) {
        //TODO send caption
    }

    @Override
    public void onCloseButtonClicked() {
        imageViewer.onDismiss();
    }


}
