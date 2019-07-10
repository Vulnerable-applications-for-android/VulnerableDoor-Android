package fr.onepoint.vulnerabledoor.service;

public enum DeviceConnectivityState {

    DEVICE_NOT_DETECTED ("Device non détecté"),
    DEVICE_DETECTED_NOT_CONNECTED ("Device détecté et non connecté"),
    DEVICE_CONNECTING_BLE ("Connexion bluetooth en cours"),
    DEVICE_WAITING_FOR_READY ("Connexion bluetooth établie et en attente de READY dans la caractéristique"),
    DEVICE_DETECTED_CONNECTED ("Connecté au device");

    private String value;

    DeviceConnectivityState(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }
}
