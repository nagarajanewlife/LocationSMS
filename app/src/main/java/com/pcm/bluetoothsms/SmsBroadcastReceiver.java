package com.pcm.bluetoothsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * A broadcast receiver who listens for incoming SMS
 */
public class SmsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                String smsSender = "";
                String smsBody = "";

                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsBody += smsMessage.getMessageBody();
                    smsSender += smsMessage.getOriginatingAddress();
                }

                //Toast.makeText(context, "SMS detected: From " + smsSender + " With text " + smsBody, Toast.LENGTH_LONG).show();

                // if (smsSender.length() > 10) {
                Intent intents = new Intent("mqttdata");
                intents.putExtra("msg", smsBody);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intents);
                //}
                Log.d(TAG, "SMS detected: From " + smsSender + " With text " + smsBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
