package fr.onepoint.vulnerabledoor.service;

import android.bluetooth.BluetoothDevice;

public abstract class BluetoothConnectionCallBack {

    public abstract void bleConnectionStateHasChanged(DeviceConnectivityState newState);

    public abstract void deviceDetected(BluetoothDevice device);

    public abstract void commandSuccessful();

}
