package com.creedon.cordova.plugin.photobrowser.metadata;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * _   _ _______   ________ _       _____   __
 * | \ | |_   _\ \ / /| ___ \ |     / _ \ \ / /
 * |  \| | | |  \ V / | |_/ / |    / /_\ \ V /
 * | . ` | | |  /   \ |  __/| |    |  _  |\ /
 * | |\  |_| |_/ /^\ \| |   | |____| | | || |
 * \_| \_/\___/\/   \/\_|   \_____/\_| |_/\_/
 * <p>
 * Created by jameskong on 9/6/2017.
 */


public class PhotoDetail implements Serializable, Parcelable {
    private static final String TAG = PhotoDetail.class.getSimpleName();
    private static PhotoDetail ourInstance;
    private static PhotoDataListener photoDataListener;

    public static PhotoDetail getInstance(@NonNull JSONObject jsonObject) {
        try {
            if(ourInstance != null){
                Log.w(TAG,"ourInstance is not null : \n"+ ourInstance.toString());
            }
            ourInstance = new Gson().fromJson(jsonObject.toString(), PhotoDetail.class);
        } catch (NullPointerException e) {
            e.printStackTrace();

        } finally {
            if (ourInstance == null) {
                ourInstance = new PhotoDetail();
            }
        }
        return ourInstance;
    }

    public static PhotoDetail getInstance() {
        if (ourInstance == null) {
            ourInstance = new PhotoDetail();
        }
        return ourInstance;
    }

    private PhotoDetail() {

    }

    @SerializedName("images")
    @Expose
    private List<String> images = null;
    @SerializedName("thumbnails")
    @Expose
    private List<String> thumbnails = null;
    @SerializedName("data")
    @Expose
    private List<Datum> data = null;
    @SerializedName("captions")
    @Expose
    private List<String> captions = null;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("action")
    @Expose
    private String action;
    @SerializedName("count")
    @Expose
    private Integer count;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("albumType")
    @Expose
    private String albumType;
    @SerializedName("actionSheet")
    @Expose
    private List<ActionSheet> actionSheet = null;
    public final static Parcelable.Creator<PhotoDetail> CREATOR = new Creator<PhotoDetail>() {


        @SuppressWarnings({
                "unchecked"
        })
        public PhotoDetail createFromParcel(Parcel in) {
            PhotoDetail instance = new PhotoDetail();
            in.readList(instance.images, (java.lang.String.class.getClassLoader()));
            in.readList(instance.thumbnails, (java.lang.String.class.getClassLoader()));
            in.readList(instance.data, (com.creedon.cordova.plugin.photobrowser.metadata.Datum.class.getClassLoader()));
            in.readList(instance.captions, (java.lang.String.class.getClassLoader()));
            instance.id = ((String) in.readValue((Integer.class.getClassLoader())));
            instance.name = ((String) in.readValue((String.class.getClassLoader())));
            instance.action= ((String) in.readValue((String.class.getClassLoader())));
            instance.count = ((Integer) in.readValue((Integer.class.getClassLoader())));
            instance.type = ((String) in.readValue((String.class.getClassLoader())));
            instance.albumType = ((String) in.readValue((String.class.getClassLoader())));
            in.readList(instance.actionSheet, (com.creedon.cordova.plugin.photobrowser.metadata.ActionSheet.class.getClassLoader()));
            return instance;
        }

        public PhotoDetail[] newArray(int size) {
            return (new PhotoDetail[size]);
        }

    };

    private final static long serialVersionUID = -7595661236141589704L;

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<String> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public List<Datum> getData() {
        return data;
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }

    public List<String> getCaptions() {
        return captions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlbumType() {
        return albumType;
    }

    public void setAlbumType(String albumType) {
        this.albumType = albumType;
    }

    public List<ActionSheet> getActionSheet() {
        return actionSheet;
    }

    public void setActionSheet(List<ActionSheet> actionSheet) {
        this.actionSheet = actionSheet;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(images);
        dest.writeList(thumbnails);
        dest.writeList(data);
        dest.writeList(captions);
        dest.writeValue(id);
        dest.writeValue(name);
        dest.writeValue(action);
        dest.writeValue(count);
        dest.writeValue(type);
        dest.writeValue(albumType);
        dest.writeList(actionSheet);
    }

    public int describeContents() {
        return 0;
    }


    public void setOnCaptionChangeListener(PhotoDataListener _photoDataListener) {
        photoDataListener = _photoDataListener;
    }

    public void setCaptions(List<String> captions) {
        this.captions = captions;
    }

    public boolean setCaption(int index, String caption) {
        this.captions.set(index, caption);
        if (photoDataListener != null) {
            return photoDataListener.onCaptionChanged(data.get(index), caption, id, type);
        }
        return false;
    }

    public void onSetName(String s) {
        if (photoDataListener != null) {
            photoDataListener.onSetName(s, id, type);
        }
    }

    public void onPhotoDeleted(ArrayList<String> deletedDatas) {
        if (photoDataListener != null) {
            photoDataListener.onPhotoDeleted(deletedDatas, id, type);
        }
    }

    public interface PhotoDataListener {
        boolean onCaptionChanged(Datum datum, String caption, String id, String type);

        void onSetName(String s, String id, String type);

        void onPhotoDeleted(ArrayList<String>  deletedData, String id, String type);

    }
}