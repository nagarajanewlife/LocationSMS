package com.pcm.bluetoothsms.Utills;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by da Ent on 29/11/2015.
 */
public class HangingValue implements Parcelable {
    public static final Creator<HangingValue> CREATOR = new Creator<HangingValue>() {
        @Override
        public HangingValue createFromParcel(Parcel source) {
            return new HangingValue(source);
        }

        @Override
        public HangingValue[] newArray(int size) {
            return new HangingValue[size];
        }
    };
    Date time;
    String message;
    String device;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public HangingValue(String device, String message) {
        this.time = new Date();
        this.message = message;
        this.device = device;
    }

    protected HangingValue(Parcel in) {
        long tmpTime = in.readLong();
        this.time = tmpTime == -1 ? null : new Date(tmpTime);
        this.message = in.readString();
        this.device = in.readString();
        this.sdf = (SimpleDateFormat) in.readSerializable();
    }

    public String getTime() {
        return sdf.format(this.time);
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Date getDate() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.time != null ? this.time.getTime() : -1);
        dest.writeString(this.message);
        dest.writeString(this.device);
        dest.writeSerializable(this.sdf);
    }
}
