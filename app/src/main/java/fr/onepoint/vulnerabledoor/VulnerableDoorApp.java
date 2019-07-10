package fr.onepoint.vulnerabledoor;

import android.app.Application;

import fr.onepoint.vulnerabledoor.service.BluetoothLeService;


/**
 * Android Application class. Used for accessing singletons.
 * Launched when application is started.
 */
public class VulnerableDoorApp extends Application {

    private final static String TAG = VulnerableDoorApp.class.getSimpleName();

    private AppExecutors mAppExecutors;
    private BluetoothLeService mBluetoothLeService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create application services
        mAppExecutors           = new AppExecutors();
        mBluetoothLeService     = new BluetoothLeService(this);
    }


    public BluetoothLeService getBluetoothLeService() { return mBluetoothLeService; }

    public AppExecutors getAppExecutors() { return mAppExecutors; }

}
