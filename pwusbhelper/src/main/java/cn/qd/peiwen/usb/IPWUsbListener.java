package cn.qd.peiwen.usb;

import android.hardware.usb.UsbDevice;

public interface IPWUsbListener {
    void onPrintMessage(String message);
    void onDeviceAttached(UsbDevice device);
    void onDeviceDetached(UsbDevice device);
    void onPermissionGranted(UsbDevice device);
    void onPermissionCanceled(UsbDevice device);
}
