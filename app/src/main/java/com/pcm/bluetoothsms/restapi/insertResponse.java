
package com.pcm.bluetoothsms.restapi;

import javax.annotation.Generated;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class insertResponse implements Parcelable
{

    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("status")
    @Expose
    private boolean status;
    public final static Parcelable.Creator<insertResponse> CREATOR = new Creator<insertResponse>() {


        @SuppressWarnings({
            "unchecked"
        })
        public insertResponse createFromParcel(Parcel in) {
            insertResponse instance = new insertResponse();
            instance.message = ((String) in.readValue((String.class.getClassLoader())));
            instance.status = ((boolean) in.readValue((boolean.class.getClassLoader())));
            return instance;
        }

        public insertResponse[] newArray(int size) {
            return (new insertResponse[size]);
        }

    }
    ;

    /**
     * 
     * @return
     *     The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * 
     * @param message
     *     The message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 
     * @return
     *     The status
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * 
     * @param status
     *     The status
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(message);
        dest.writeValue(status);
    }

    public int describeContents() {
        return  0;
    }

}
