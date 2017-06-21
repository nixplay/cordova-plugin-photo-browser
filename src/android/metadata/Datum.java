
package com.creedon.cordova.plugin.photobrowser.metadata;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Datum implements Serializable, Parcelable
{

    @SerializedName("id")
    @Expose
    private String id;

    @Nullable
    @SerializedName("originalUrl")
    @Expose
    private String originalUrl;

    @Nullable
    @SerializedName("caption")
    @Expose
    private String caption;

    @Nullable
    @SerializedName("orientation")
    @Expose
    private int orientation;

    public final static Parcelable.Creator<Datum> CREATOR = new Creator<Datum>() {


        @SuppressWarnings({
            "unchecked"
        })
        public Datum createFromParcel(Parcel in) {
            Datum instance = new Datum();
            instance.id = ((String) in.readValue((String.class.getClassLoader())));
            instance.originalUrl = ((String) in.readValue((String.class.getClassLoader())));
            instance.caption = ((String) in.readValue((String.class.getClassLoader())));
            instance.orientation = ((Integer) in.readValue((Integer.class.getClassLoader())));
            return instance;
        }

        public Datum[] newArray(int size) {
            return (new Datum[size]);
        }

    }
    ;
    private final static long serialVersionUID = 6516892402532993702L;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Nullable
    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(@Nullable int orientation) {
        this.orientation = orientation;
    }


    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(originalUrl);
        dest.writeValue(caption);
        dest.writeValue(orientation);
    }

    public int describeContents() {
        return  0;
    }

    public JSONObject toJSON() throws JSONException {


        Gson gson = new Gson();
        String json = gson.toJson(this); //convert
        JSONObject jo = new JSONObject(json);
        return jo;
    }

}
