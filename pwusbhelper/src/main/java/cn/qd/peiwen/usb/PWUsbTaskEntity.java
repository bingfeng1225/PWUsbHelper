package cn.qd.peiwen.usb;

import android.hardware.usb.UsbDevice;

class PWUsbTaskEntity {
    private int type;
    private UsbDevice device;

    public PWUsbTaskEntity(int type, UsbDevice device) {
        this.type = type;
        this.device = device;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public UsbDevice getDevice() {
        return device;
    }

    public void setDevice(UsbDevice device) {
        this.device = device;
    }
}
