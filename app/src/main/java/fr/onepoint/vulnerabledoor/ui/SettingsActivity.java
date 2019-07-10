package fr.onepoint.vulnerabledoor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import fr.onepoint.vulnerabledoor.R;
import fr.onepoint.vulnerabledoor.VulnerableDoorApp;
import fr.onepoint.vulnerabledoor.service.BluetoothLeService;

public class SettingsActivity extends BaseActivity {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    //-- Connection
    private BluetoothLeService bleService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Get my Bluetooth Low Energy service
        this.bleService = ((VulnerableDoorApp)getApplication()).getBluetoothLeService();
    }

    /**
     * Called when the user taps 'Validate telephone number in settings' button
     */
    public void saveSettingsAndGoToMainPage(View view) {
        Log.i(TAG, "goBackToMainPage() : 'validate' button clicked");

        // Save settings
        savePhoneNumberSettings();

        // Go to Main page
        goToMainPage();
    }

    // Get phone number settings + send it by BLE to iOT device
    private void savePhoneNumberSettings() {
        Log.i(TAG, "savePhoneNumberSettings()");

        String phoneNumber = ((EditText)findViewById(R.id.editTelephoneNumber)).getText().toString();
        Log.i(TAG, "phoneNumber = " + phoneNumber);

        // Send phone Number by BLE
        bleService.sendCommand("w:tel" + phoneNumber);
    }

    // Redirect to MainActivity
    private void goToMainPage() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish(); // Replace this activity
    }
}
