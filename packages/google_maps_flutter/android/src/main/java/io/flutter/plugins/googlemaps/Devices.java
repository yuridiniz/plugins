package io.flutter.plugins.googlemaps;

import androidx.annotation.Nullable;

public class Devices {
    private String device;
    private int os;
    private String product;

    public Devices(String device, int os) {
        this.device = device;
        this.os = os;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getOs() {
        return os;
    }

    public void setOs(int os) {
        this.os = os;
    }


    public void setProduct(String product) {
        this.product = product;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null)
            return false;

        Devices _devices = (Devices)obj;
        if(_devices.os == this.os && (this.device.equals(_devices.device) || this.device.equals(_devices.product)))
            return true;

        return false;
    }

}
