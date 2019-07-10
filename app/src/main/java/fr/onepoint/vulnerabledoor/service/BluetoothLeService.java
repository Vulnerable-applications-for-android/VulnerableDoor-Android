package fr.onepoint.vulnerabledoor.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.onepoint.vulnerabledoor.R;


// A service that interacts with the Bluetooth Low Energy (BLE) device via the Android BLE API.
public class BluetoothLeService {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    //-- SCAN
    private boolean mScanning = false;

    private Context context;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bleDevice;
    private BluetoothConnectionCallBack uiCallback;

    private BluetoothGattCharacteristic characteristicBle;


    private DeviceConnectivityState deviceConnectionState = DeviceConnectivityState.DEVICE_NOT_DETECTED;

    //-- Scan
    private ScanCallback leScanCallback;
    private Handler handler;

    /**
     * Constructor
     * @param context
     */
    public BluetoothLeService(Context context) {
        this.context = context;
    }

    /**
     * Subscribe to be called by callback
     * @param uiCallback
     */
    public void subscribeBluetooth(BluetoothConnectionCallBack uiCallback) {
        displayMessage( "subscribeBluetooth()");
        this.uiCallback = uiCallback;

        // Initializes Bluetooth adapter.
        this.bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        initBleScanCallback();
    }

    /**
     * @return  true if bluetooth is enable
     */
    public boolean isBleEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    /**
     * @return current DeviceConnectivityState
     */
    public DeviceConnectivityState getCurrentState() {
        return deviceConnectionState;
    }

    //*************************************************************************************
    //-------------------------------       SCAN BLE        -------------------------------
    //*************************************************************************************
    /**  Init BLUETOOTH SCAN callback when others devices detected in Bluetooth **/
    private void initBleScanCallback() {
        displayMessage( "initBleScanCallback()");

        handler = new Handler();

        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                displayMessage("onScanResult() : callbackType=" + callbackType + "; " + result.getDevice());

                // Get device
                bleDevice = result.getDevice();
                uiCallback.deviceDetected(bleDevice);

                // Already in state DEVICE_DETECTED_NOT_CONNECTED ?
                if (deviceConnectionState != DeviceConnectivityState.DEVICE_DETECTED_NOT_CONNECTED) {
                    deviceConnectionState = DeviceConnectivityState.DEVICE_DETECTED_NOT_CONNECTED;
                    uiCallback.bleConnectionStateHasChanged(deviceConnectionState);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                displayMessage("onBatchScanResults()" + results.toString());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                displayMessage("onScanFailed() : errorCode=" + errorCode);

                deviceConnectionState = DeviceConnectivityState.DEVICE_NOT_DETECTED;
                uiCallback.bleConnectionStateHasChanged(deviceConnectionState);
            }
        };
    }

    /** SCAN BLUETOOTH devices (with filter on my iOT device) **/
    public void scanBleDevice(final boolean enable) {
        displayMessage("scanBleDevice(" + enable +")");

        // Scanning demand has changed ?
        if (enable == mScanning) {
            displayMessage("scanBleDevice(" + enable +") : do nothing. Already in this state.");
            return;
        }

        if (enable) {
            mScanning = true;
            startScan(context.getResources().getString(R.string.iOT_vulndoor_service_uuid), leScanCallback);
            displayMessage("START scanning BLUETOOTH devices");

        } else {
            mScanning = false;
            stopScan(leScanCallback);
            displayMessage("STOP scanning wanted.");
        }
    }

    /**
     * Start scanning
     * @param serviceUUID
     * @param scanCallback
     */
    public void startScan(String serviceUUID, ScanCallback scanCallback) {
        this.leScanCallback = scanCallback;
        ScanSettings scanSettings = new ScanSettings.Builder().build();
        List<ScanFilter> scanLeFilters = buildScanLeFilters(serviceUUID);

        bluetoothAdapter.getBluetoothLeScanner().startScan(scanLeFilters, scanSettings, leScanCallback);
    }

    /** Filter on my iOT device */
    private List<ScanFilter> buildScanLeFilters(String serviceUUID) {
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(serviceUUID))
                .build());
        return filters;
    }

    /**
     * Stop scanning
     * @param leScanCallback
     */
    public void stopScan(ScanCallback leScanCallback) {
        bluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
    }

    //*************************************************************************************
    //----------------------------       CONNECT to BLE        ----------------------------
    //*************************************************************************************
    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        deviceConnectionState = DeviceConnectivityState.DEVICE_WAITING_FOR_READY;
                        displayMessage( "Connected to GATT server.");
                        gatt.discoverServices();
                        uiCallback.bleConnectionStateHasChanged(deviceConnectionState);


                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        deviceConnectionState = DeviceConnectivityState.DEVICE_DETECTED_NOT_CONNECTED;
                        displayMessage( "Disconnected from GATT server.");
                        uiCallback.bleConnectionStateHasChanged(deviceConnectionState);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    displayMessage("onCharacteristicChanged()");

                    String characteristicUuidString = context.getResources().getString(R.string.iOT_vulndoor_characteristic_uuid);
                    UUID characteristicUuid = UUID.fromString(characteristicUuidString);

                    if (characteristicUuid.equals(characteristic.getUuid())) {
                        characteristicBle = characteristic;
                        String value = characteristic.getStringValue(0);
                        displayMessage("Characteritic Value:" + value);

                        if (value.startsWith("READY")){
                            deviceConnectionState = DeviceConnectivityState.DEVICE_DETECTED_CONNECTED;
                            uiCallback.bleConnectionStateHasChanged(deviceConnectionState);
                        } else if (value.startsWith(encrypt("OK"))){
                            displayMessage("Success");
                            uiCallback.commandSuccessful();
                        }
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status){
                    displayMessage("onServicesDiscovered()");

                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        // Handle the error
                        displayMessage( "Error "+String.valueOf(status));
                        return;
                    }

                    // S'abonner à la Notification // TODO MFU se desabonner à un moement ???!?
                    String serviceUuidString = context.getResources().getString(R.string.iOT_vulndoor_service_uuid);
                    UUID serviceUuid = UUID.fromString(serviceUuidString);
                    String characteristicUuidString = context.getResources().getString(R.string.iOT_vulndoor_characteristic_uuid);
                    UUID characteristicUuid = UUID.fromString(characteristicUuidString);
                    String descriptorUuidString = context.getResources().getString(R.string.iOT_vulndoor_descriptor_uuid);
                    UUID descriptorUuid = UUID.fromString(descriptorUuidString);

                    // Get the vulndoor characteristic
                    BluetoothGattCharacteristic characteristic = gatt
                            .getService(serviceUuid)
                            .getCharacteristic(characteristicUuid);

                    // Enable notifications for this characteristic locally
                    gatt.setCharacteristicNotification(characteristic, true);

                    // Write on the config descriptor to be notified when the value changes
                    BluetoothGattDescriptor descriptor =
                            characteristic.getDescriptor(descriptorUuid);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);

                }
            };

    /**
     * Connect to device by Bluetooth Low Energy
     */
    public void connect() {
        displayMessage("Try to connect BLE to device " + bleDevice.getName());
        this.bluetoothGatt = bleDevice.connectGatt(context, false, gattCallback);
    }

    /**
     * Disconnect from device
     */
    public void disconnect() {
        displayMessage("Try to disconnect BLE from device " + bleDevice.getName());
        if (bluetoothGatt != null) {
            Log.i(TAG, "bluetoothGatt.disconnect() called");
            bluetoothGatt.disconnect();
        }
    }

    /**
     * @return true if device is connected by Bluetooth Low Energy
     */
    public boolean isConnected() {
        if (bleDevice != null) {
            int bluetoothState = bluetoothManager.getConnectionState(bleDevice, BluetoothProfile.GATT);
            return bluetoothState == BluetoothProfile.STATE_CONNECTED;
        }
        return false;
    }

    public void close() {
        Log.i(TAG, "close()");
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    //*************************************************************************************
    //------------------------------       EXCHANGE         -------------------------------
    //*************************************************************************************
    public void sendCommand(String command) {
        displayMessage("Try to send command " + command);
        if (bluetoothGatt != null && characteristicBle != null) {
            displayMessage("Try to send command " + command + " to device " + bleDevice.getName());

            // Encrypt string command
            String encryptedCommand = encrypt(command);
            Log.i(TAG, "encryptedCommand = " + encryptedCommand);

            characteristicBle.setValue(encryptedCommand);
            bluetoothGatt.writeCharacteristic(characteristicBle);
        }
    }


    //*************************************************************************************
    //-------------------------       ENCRYPT / DECRYPT       -----------------------------
    //*************************************************************************************
    private String xorWithKey(String input) {
        String keyString = "MySup3rK3y";
        String output = "";

        for(int i = 0; i < input.length(); i++) {

            int cryptKeyInt = (int) keyString.charAt(i % keyString.length());
            int inputCharInt = (int) input.charAt(i);
            output+=(char) (inputCharInt ^ cryptKeyInt);
        }

        return output;
    }

    private String encrypt(String plainText) {
        return Base64.encodeToString(xorWithKey(plainText).getBytes(), Base64.DEFAULT);
    }

    private String decrypt(String cypherText) {
        byte[] decoded = Base64.decode(cypherText, Base64.DEFAULT);
        return xorWithKey(new String(decoded));
    }

    //*************************************************************************************
    //-------------------------------       MESSAGE         -------------------------------
    //*************************************************************************************
    /**
     * Display message
     * @param message
     */
    public void displayMessage(String message) {
        Log.i(TAG, message);
    }

}
