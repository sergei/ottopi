package com.santacruzinstruments.ottopi.data;

import androidx.annotation.NonNull;

import java.util.Locale;

public class ConnectionState {

    public enum NetworkStatus {
        DISABLED,
        NOT_CONNECTED,
        CONNECTING_TO_WIFI,
        CONNECTING_TO_HOST,
        CONNECTED,
    }

    private final NetworkStatus state;
    private final String name;

    public ConnectionState(NetworkStatus state, String name) {
        this.state = state;
        this.name = name;
    }

    public NetworkStatus getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        switch (state){
            case NOT_CONNECTED:
                return "--";
            case CONNECTING_TO_WIFI:
            case CONNECTING_TO_HOST:
                return String.format(Locale.getDefault(), "Connecting to %s", name);
            case CONNECTED:
                return String.format(Locale.getDefault(), "Connected to %s", name);
        }
        return "";
    }
}
