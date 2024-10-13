package com.pcm.bluetoothsms.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pcm.bluetoothsms.R;
import com.pcm.bluetoothsms.Utills.CheckInternetAvailability;
import com.pcm.bluetoothsms.Utills.Constants;
import com.pcm.bluetoothsms.Utills.HangingValue;
import com.pcm.bluetoothsms.fragment.ItemFragment;
import com.pcm.bluetoothsms.mqtt.MqttMessageService;
import com.pcm.bluetoothsms.mqtt.PahoMqttClient;
import com.pcm.bluetoothsms.restapi.ApiConstant;
import com.pcm.bluetoothsms.restapi.Get_Service;
import com.pcm.bluetoothsms.service.BluetoothService;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class SwitchControl extends AppCompatActivity implements View.OnClickListener,
        ItemFragment.OnListFragmentInteractionListener, Get_Service.Data_listener {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private static final String TAG = SwitchControl.class.getSimpleName();
    private SharedPreferences pref;// = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
    private LinearLayout rl_main;
    private Context context;
    private HangingValue messageRead;
    private BluetoothAdapter bluetoothAdapter;
    private AlertDialog BTDialog;
    private ArrayList<String> adapter;
    private int selectedDevice;
    private List<BluetoothDevice> bluetoothDevices;
    private ArrayList<HangingValue> list = new ArrayList<>();
    private BluetoothDevice device;
    private Snackbar snackTurnOn;
    private ProgressDialog dialogSearching;
    private ProgressDialog dialogposting;
    private BluetoothService bluetoothService;
    private Button getWeightBtn;
    private String phoneNo, message;
    private myHandler handler;
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                device.fetchUuidsWithSdp();

                Log.d(Constants.TAG, "device.getAddress()- " + device.getAddress());

                bluetoothDevices.add(device);
                adapter.add(
                        ((device.getName() != null) ? (device.getName() + " - ") : "") + device.getAddress());

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                if (dialogSearching != null) {
                    if (dialogSearching.isShowing()) {
                        dialogSearching.dismiss();
                    }
                }

                if (adapter.isEmpty()) {
                    Toast.makeText(context, "No Devices found", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] strings = adapter.toArray(new String[adapter.size()]);
                BTDialog = new AlertDialog.Builder(context)
                        .setTitle("Select Devices")
                        .setSingleChoiceItems(strings, selectedDevice, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selectedDevice = which;
                            }
                        })
                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                bluetoothAdapter.cancelDiscovery();
                                device = bluetoothDevices.get(selectedDevice);
                                bluetoothService = new BluetoothService(handler, device);
                                bluetoothService.connect();
                                if (BTDialog != null && BTDialog.isShowing()) {
                                    BTDialog.dismiss();
                                }
                            }
                        }).show();

            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Snackbar.make(rl_main, "Bluetooth turned off", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Turn on", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        enableBluetooth();
                                    }
                                }).show();
                        break;
                }
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        snackTurnOn.show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        if (snackTurnOn.isShownOrQueued()) {
                            snackTurnOn.dismiss();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        reconnect();
                }
            }
        }
    };
    private MqttAndroidClient client;
    private PahoMqttClient pahoMqttClient;
    private TextView statusField;
    private boolean BTWConnected = false;
    private boolean btn1 = true, btn2 = true, btn3 = true, btn4 = true, btn5 = true, btn6 = true, btn7 = true, btn8 = true;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("msg");
            Log.d(TAG, "SMS detected:  With text " + msg);
            Toast.makeText(context, "SMS detected: " + msg, Toast.LENGTH_SHORT).show();

            if (BTWConnected) {
                bluetoothService.write(msg.getBytes());
                Toast.makeText(context, "Data send to " + device.getName(), Toast.LENGTH_SHORT).show();
            } else Toast.makeText(context, "Device not connected", Toast.LENGTH_SHORT).show();
        }
    };

    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
            Log.d(TAG, "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS},
                MY_PERMISSIONS_REQUEST_SEND_SMS);
    }

    /**
     * Runtime permission shenanigans
     */
    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    public void setStatus(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        //getDeviceBtn.setText(msg);
    }

    private void ShowpairedDevicesOrSearch() {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevices.clear();
        adapter.clear();

        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            bluetoothDevices.add(device);
            adapter.add(
                    ((device.getName() != null) ? (device.getName() + " - ") : "") + device.getAddress());
        }

        if (adapter.isEmpty()) {
            startSearching();
            return;
        }
        String[] strings = adapter.toArray(new String[adapter.size()]);
        BTDialog = new AlertDialog.Builder(context)
                .setTitle("Select Devices")
                .setSingleChoiceItems(strings, selectedDevice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedDevice = which;
                    }
                })
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        device = bluetoothDevices.get(selectedDevice);
                        bluetoothService = new BluetoothService(handler, device);
                        bluetoothService.connect();
                        if (BTDialog != null && BTDialog.isShowing()) {
                            BTDialog.dismiss();
                        }
                    }
                })
                .setNegativeButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startSearching();
                        if (BTDialog != null && BTDialog.isShowing()) {
                            BTDialog.dismiss();
                        }
                    }
                })
                .show();
    }

    private void startSearching() {
        if (bluetoothAdapter.startDiscovery()) {
            adapter.clear();
            bluetoothDevices.clear();

            if (dialogSearching != null) {
                if (!dialogSearching.isShowing()) {
                    dialogSearching.show();
                }
            } else {
                dialogSearching = new ProgressDialog(context);
                dialogSearching.setMessage("Searching for devices, please wait.");
                dialogSearching.show();
            }

        } else {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            Snackbar.make(rl_main, "Failed to start searching", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Try Again", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startSearching();
                        }
                    }).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startSearching();
            } else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();

                Snackbar.make(rl_main, "Failed to enable bluetooth", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Try Again", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                enableBluetooth();
                            }
                        }).show();
            }
        }

    }

    private void reconnect() {
        bluetoothService.stop();
        bluetoothService.connect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_switch);
            rl_main = (LinearLayout) findViewById(R.id.rl_main);
            context = this;
            pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
            //Button
            getWeightBtn = (Button) findViewById(R.id.btn_config);
            getWeightBtn.setOnClickListener(this);

            getWeightBtn = (Button) findViewById(R.id.btn_1);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_2);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_3);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_4);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_5);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_6);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_7);
            getWeightBtn.setOnClickListener(this);
            getWeightBtn = (Button) findViewById(R.id.btn_8);
            getWeightBtn.setOnClickListener(this);

            getWeightBtn = (Button) findViewById(R.id.devices_BTN);
            getWeightBtn.setOnClickListener(this);
            statusField = (TextView) findViewById(R.id.statusText);

            // Bluetooth initialization
            handler = new myHandler(SwitchControl.this);
            adapter = new ArrayList<>();
            bluetoothDevices = new ArrayList<>();
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter == null) {

                Log.e(Constants.TAG, "Device has no bluetooth");
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("No Bluetooth")
                        .setMessage("Your device has no bluetooth")
                        .setPositiveButton("Close app", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Log.d(Constants.TAG, "App closed");
                                finish();
                            }
                        }).show();

            }

            snackTurnOn = Snackbar.make(rl_main, "Bluetooth turned off", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Turn On", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enableBluetooth();
                        }
                    });

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ItemFragment()).commit();

            pahoMqttClient = new PahoMqttClient();
            client = pahoMqttClient.getMqttClient(getApplicationContext(),
                    "tcp://" + pref.getString("serverport",
                            "m13.cloudmqtt.com:16105"), ApiConstant.CLIENT_ID,
                    pref.getString("usr", "qnfcmgvm"), pref.getString("pwd", "_L5_GG0sesbX"));
            Intent intent = new Intent(this, MqttMessageService.class);
            startService(intent);
            pahoMqttClient.subscribe(client, ApiConstant.PUBLISH_TOPIC, 1);

            //sendMessage("Raja");

            if (!hasReadSmsPermission()) {
                requestReadAndSendSmsPermission();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.devices_BTN:
                    if (!BTWConnected) {
                        if (bluetoothAdapter.isEnabled()) {
                            // Bluetooth enabled
                            // startSearching();
                            ShowpairedDevicesOrSearch();
                        } else {
                            enableBluetooth();
                        }
                    }
                    break;
                case R.id.btn_config:
                    showaddDialog();
                    break;

                case R.id.btn_1:
                    pahoMqttClient.publishMessage(client, btn1 ? "1" : "a", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn1 ? "1".getBytes() : "a".getBytes());
                    }
                    break;
                case R.id.btn_2:
                    pahoMqttClient.publishMessage(client, btn1 ? "2" : "b", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn2 ? "2".getBytes() : "b".getBytes());
                    }
                    break;
                case R.id.btn_3:
                    pahoMqttClient.publishMessage(client, btn1 ? "3" : "c", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn3 ? "3".getBytes() : "c".getBytes());
                    }
                    break;
                case R.id.btn_4:
                    pahoMqttClient.publishMessage(client, btn1 ? "4" : "d", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn4 ? "4".getBytes() : "d".getBytes());
                    }
                    break;
                case R.id.btn_5:
                    pahoMqttClient.publishMessage(client, btn1 ? "5" : "e", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn5 ? "5".getBytes() : "e".getBytes());
                    }
                    break;
                case R.id.btn_6:
                    pahoMqttClient.publishMessage(client, btn1 ? "6" : "f", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn6 ? "6".getBytes() : "f".getBytes());
                    }
                    break;
                case R.id.btn_7:
                    pahoMqttClient.publishMessage(client, btn1 ? "7" : "g", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn7 ? "7".getBytes() : "g".getBytes());
                    }
                    break;
                case R.id.btn_8:
                    pahoMqttClient.publishMessage(client, btn1 ? "8" : "h", 1, ApiConstant.PUBLISH_TOPIC);
                    if (BTWConnected) {
                        bluetoothService.write(btn8 ? "8".getBytes() : "h".getBytes());
                    }
                    break;
                default:
                    break;
            }
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showaddDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.config, null);
        dialogBuilder.setView(dialogView);

        final EditText serverport = (EditText) dialogView.findViewById(R.id.server);
        final EditText usr = (EditText) dialogView.findViewById(R.id.username);
        final EditText pwd = (EditText) dialogView.findViewById(R.id.password);
        serverport.setText(pref.getString("serverport", ""));
        usr.setText(pref.getString("usr", ""));
        pwd.setText(pref.getString("pwd", ""));
        dialogBuilder.setTitle("Config");
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //do something with edt.getText().toString();
                if (TextUtils.isEmpty(serverport.getText().toString())) {
                    serverport.setError("Please enter path");
                    serverport.requestFocus();
                    return;
                } else {
                    serverport.setError(null);
                    pref.edit().putString("serverport", serverport.getText().toString()).commit();
                }
                if (TextUtils.isEmpty(usr.getText().toString())) {
                    usr.setError("Please enter path");
                    usr.requestFocus();
                    return;
                } else {
                    usr.setError(null);
                    pref.edit().putString("usr", usr.getText().toString()).commit();
                }
                if (TextUtils.isEmpty(pwd.getText().toString())) {
                    pwd.setError("Please enter path");
                    pwd.requestFocus();
                    return;
                } else {
                    serverport.setError(null);
                    pref.edit().putString("pwd", pwd.getText().toString()).commit();
                }

            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Log.d(Constants.TAG, "Registering receiver");
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);

            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mMessageReceiver, new IntentFilter("mqttdata"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            stopBTService();
            Log.d(Constants.TAG, "Receiver unregistered");
            unregisterReceiver(mReceiver);
            unregisterReceiver(mMessageReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopBTService() {
        try {
            if (bluetoothService != null) {
                bluetoothService.stop();
                Log.d(Constants.TAG, "Stopping");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableBluetooth() {
        Toast.makeText(this, "Enabling Bluetooth", Toast.LENGTH_SHORT).show();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                //Request location updates:
                // locationManager.requestLocationUpdates(provider, 400, 1, this);
            }
        }

    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        //Request location updates:
                        //  locationManager.requestLocationUpdates(provider, 400, 1, this);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            case MY_PERMISSIONS_REQUEST_SEND_SMS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
                break;
        }
    }

    @Override
    public void onListFragmentInteraction(HangingValue item) {

        list.remove(item);
        // Create fragment and give it an argument for the selected article
        ItemFragment newFragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ItemFragment.ARG_HANGINGVALUES, list);
        newFragment.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    protected void sendSMSMessage(String message) {
        phoneNo = pref.getString("num", "");
        this.message = message;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        } else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        }
    }

    protected void sendMessage(String message) {
        if (pref.getString("site", "").equals("")) {
            Toast.makeText(this, "Please add site", Toast.LENGTH_SHORT).show();
            return;
        }
        dialogposting = new ProgressDialog(context);
        dialogposting.setMessage("sending data, please wait.");
        dialogposting.show();
        new Get_Service(this, this).getData(message, pref.getString("site", ""));
    }

    @Override
    public void onReceivedsuccess(String msg) {
        if (dialogposting != null) {
            if (dialogposting.isShowing()) {
                dialogposting.dismiss();
            }
        }
        statusField.setText(msg);
    }

    @Override
    public void onReceivedError(String msg) {
        if (dialogposting != null) {
            if (dialogposting.isShowing()) {
                dialogposting.dismiss();
            }
        }
        statusField.setText(msg);
    }

    private static class myHandler extends Handler {

        private final WeakReference<SwitchControl> mActivity;

        public myHandler(SwitchControl activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            final SwitchControl activity = mActivity.get();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Constants.STATE_CONNECTED:
                            if (activity.BTDialog != null && activity.BTDialog.isShowing()) {
                                activity.BTDialog.dismiss();
                            }
                            activity.BTWConnected = true;
                            activity.setStatus("Connected");
                            activity.list.clear();
                            activity.statusField.setText(activity.device.getName());
                            break;
                        case Constants.STATE_CONNECTING:
                            activity.BTDialog.dismiss();
                            activity.setStatus("Connecting");
                            break;
                        case Constants.STATE_NONE:
                            activity.BTWConnected = false;
                            activity.setStatus("Not Connected");
                            break;
                        case Constants.STATE_ERROR:
                            activity.BTWConnected = false;
                            //activity.setStatus("Error");
                            break;
                        default:
                            activity.setStatus("Device");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    HangingValue messageWrite = new HangingValue("Me", writeMessage);
                    activity.statusField.setText(writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    if (readMessage != null) {
                        activity.messageRead = new HangingValue(activity.device.getName(),
                                readMessage.trim().replace("kg", ""));

                        activity.list.add(activity.messageRead);

                        ItemFragment newFragment = new ItemFragment();
                        Bundle args = new Bundle();
                        args.putParcelableArrayList(ItemFragment.ARG_HANGINGVALUES, activity.list);
                        newFragment.setArguments(args);
                        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();

                        // Replace whatever is in the fragment_container view with this fragment,
                        // and add the transaction to the back stack so the user can navigate back
                        transaction.replace(R.id.fragment_container, newFragment);
                        transaction.addToBackStack(null);

                        // Commit the transaction
                        transaction.commit();

                        activity.statusField.setText(readMessage);

                        if (new CheckInternetAvailability(activity).isNetworkAvailable()) {
                            //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
                            //intent.putExtra("sms_body", readMessage);
                            //activity.startActivity(intent);//"712876372,2183721873,message@"
                            activity.sendMessage(readMessage);
                        } else activity.statusField.setText("Internet not connected");
                    }
                    break;
                case Constants.MESSAGE_SNACKBAR:
                    Snackbar.make(activity.rl_main, msg.getData().getString(Constants.SNACKBAR),
                            Snackbar.LENGTH_LONG)
                            .setAction("Connect", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    activity.reconnect();
                                }
                            }).show();
                    break;
            }
        }
    }
}