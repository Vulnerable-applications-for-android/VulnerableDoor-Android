package fr.onepoint.vulnerabledoor.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import fr.onepoint.vulnerabledoor.R;
import fr.onepoint.vulnerabledoor.VulnerableDoorApp;
import fr.onepoint.vulnerabledoor.service.BluetoothConnectionCallBack;
import fr.onepoint.vulnerabledoor.service.BluetoothLeService;
import fr.onepoint.vulnerabledoor.service.DeviceConnectivityState;


public class MainActivity extends BaseActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    static final int REQUEST_ENABLE_BT = 1;  // The request code


    private BluetoothDevice bleDevice; // TODO useful ?


    //-- Connection
    private BluetoothLeService bleService;
    private DeviceConnectivityState deviceConnectionState = DeviceConnectivityState.DEVICE_NOT_DETECTED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Permissions : Location
        checkPermissionsLocation();

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Subscribe to my BLE Service
        this.bleService = ((VulnerableDoorApp) getApplication()).getBluetoothLeService();
        bleService.subscribeBluetooth(new BluetoothConnectionCallBack() {
            @Override
            public void bleConnectionStateHasChanged(DeviceConnectivityState newState) {
                deviceConnectionState = newState;
                updateGUI();
            }

            @Override
            public void deviceDetected(BluetoothDevice device) {
                bleDevice = device;
            }

            @Override
            public void commandSuccessful() {
                displayMessage("Numéro de tél configuré.", true);
            }

        });

        // Enable BLUETOOTH
        enableBle();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update with current device connection state
        deviceConnectionState = bleService.getCurrentState();
        updateGUI();

        if(bleService.isBleEnabled()) {

            // Already connected ? (quand l'application était déjà allumé et le device connecté)
            if (!bleService.isConnected()) {
                startScanningBluetoothLE();
            }
        }
    }


    //*************************************************************************************
    //-----------------------------       AUTHORIZATION       -----------------------------
    //*************************************************************************************
    private void checkPermissionsLocation() {
        displayMessage( "checkPermissionsLocation()");

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }
    }


    /**  Enable Bluetooth on device : activate bluetooth. */
    private void enableBle() {
        displayMessage("enableBle()");

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bleService.isBleEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    //*************************************************************************************
    //-------------------------------       SCAN BLE        -------------------------------
    //*************************************************************************************
    private void startScanningBluetoothLE() {
        bleService.scanBleDevice(true);
    }
    private void stopScanningBluetoothLE() {
        bleService.scanBleDevice(false);
    }

    //*************************************************************************************
    //----------------------------       CONNECT to BLE        ----------------------------
    //*************************************************************************************
    public void connectOrDisconnectBleToDevice(View view) {
        if (deviceConnectionState == DeviceConnectivityState.DEVICE_DETECTED_NOT_CONNECTED) {

            displayMessage("Try to connect BLE to device ", true);
            deviceConnectionState = DeviceConnectivityState.DEVICE_CONNECTING_BLE;
            updateGUI();
            bleService.connect();

        } else if (deviceConnectionState == DeviceConnectivityState.DEVICE_DETECTED_CONNECTED) {

            displayMessage("Try to disconnect BLE to device ", true);
            bleService.disconnect();
        }
    }


    private void updateGUI() {
        displayMessage("updateGUI() : " + deviceConnectionState, true);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                //-- Stuff that updates the UI --

                // Change texts and buttons
                TextView alarmDoorText = (TextView) findViewById(R.id.textAlarmDoorState);
                TextView instructionsText = (TextView) findViewById(R.id.textInstruction);
                Button settingsButton = (Button)findViewById(R.id.openSettingsButton);
                ImageButton vulnerableDoorButton = (ImageButton)findViewById(R.id.imageButtonDoor);
                ProgressBar pgsBar = (ProgressBar)findViewById(R.id.progressBar);


                if (deviceConnectionState == DeviceConnectivityState.DEVICE_DETECTED_CONNECTED) {

                    // Stop scanning when connected to device
                    stopScanningBluetoothLE();

                    // No progressBar
                    pgsBar.setVisibility(View.GONE);

                    // Texts
                    alarmDoorText.setText(getResources().getText(R.string.alarm_deactivated));
                    instructionsText.setText(getResources().getText(R.string.alarm_info_to_activate_alarm));

                    // Settings button ACTIVE
                    settingsButton.setClickable(true);
                    settingsButton.setVisibility(View.VISIBLE);

                    // VulnerableDoor button : GREEN
                    vulnerableDoorButton.setVisibility(View.VISIBLE);
                    vulnerableDoorButton.setClickable(true);
                    vulnerableDoorButton.setBackgroundColor(getResources().getColor(R.color.colorGreen, getTheme()));

                } else if (deviceConnectionState == DeviceConnectivityState.DEVICE_NOT_DETECTED) {
                    // No progressBar
                    pgsBar.setVisibility(View.GONE);

                    // Texts
                    alarmDoorText.setText(getResources().getText(R.string.alarm_not_found));
                    instructionsText.setText(getResources().getText(R.string.alarm_info_to_wait));

                    // Settings button does not appear
                    settingsButton.setClickable(false);
                    settingsButton.setVisibility(View.GONE);

                    // VulnerableDoor button : GREY
                    vulnerableDoorButton.setVisibility(View.VISIBLE);
                    vulnerableDoorButton.setClickable(false);
                    vulnerableDoorButton.setBackgroundColor(getResources().getColor(R.color.colorGrey, getTheme()));

                    // Start scanning
                    startScanningBluetoothLE();

                } else if (deviceConnectionState == DeviceConnectivityState.DEVICE_DETECTED_NOT_CONNECTED) {
                    // Start scanning
                    startScanningBluetoothLE();

                    // No progressBar
                    pgsBar.setVisibility(View.GONE);

                    // Texts
                    alarmDoorText.setText(getResources().getText(R.string.alarm_activated));
                    instructionsText.setText(getResources().getText(R.string.alarm_info_to_desctivate_alarm));

                    // Settings button does not appear
                    settingsButton.setClickable(false);
                    settingsButton.setVisibility(View.GONE);

                    // VulnerableDoor button : RED
                    vulnerableDoorButton.setVisibility(View.VISIBLE);
                    vulnerableDoorButton.setClickable(true);
                    vulnerableDoorButton.setBackgroundColor(getResources().getColor(R.color.colorRed, getTheme()));

                } else if (deviceConnectionState == DeviceConnectivityState.DEVICE_CONNECTING_BLE) {
                    // Display progressBar
                    pgsBar.setVisibility(View.VISIBLE);

                    // Texts
                    alarmDoorText.setText(getResources().getText(R.string.alarm_deactivated_connecting));
                    instructionsText.setText(getResources().getText(R.string.alarm_info_to_wait_during_connecting));

                    // VulnerableDoor button : hidden
                    vulnerableDoorButton.setVisibility(View.GONE);

                } else if (deviceConnectionState == DeviceConnectivityState.DEVICE_WAITING_FOR_READY) {
                    // Do nothing
                }

            }
        });
    }

    //*************************************************************************************
    //-----------------------       EXCHANGE with DEVICE iOT         ----------------------
    //*************************************************************************************
    public void sendCommandToDevice() {





    }

    //*************************************************************************************
    //-------------------------------       MESSAGE         -------------------------------
    //*************************************************************************************
    public void displayMessage(String message, boolean inToast) {
        if (inToast) {
            //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        displayMessage(message);
    }

    public void displayMessage(String message) {
        Log.i(TAG, message);
    }

    /**
     * Called when the user taps 'Configure' button
     */
    public void displaySettingsView(View view) {
        displayMessage("displaySettingsView() : 'configure' button clicked");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        this.finish(); // Replace this activity
    }
}
