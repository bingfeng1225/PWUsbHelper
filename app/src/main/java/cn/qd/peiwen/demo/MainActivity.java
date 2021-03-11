package cn.qd.peiwen.demo;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import cn.qd.peiwen.usb.IPWUsbListener;
import cn.qd.peiwen.usb.PWUsbHelper;

public class MainActivity extends AppCompatActivity implements IPWUsbListener {
    private UsbDevice device;
    private PWUsbHelper helper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.helper = new PWUsbHelper(this);
        this.helper.changeListener(this);
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.init:
                this.helper.init();
                break;
            case R.id.enable:
                this.helper.enalbe();
                break;
            case R.id.discovery:
                this.helper.discovery();
                break;
            case R.id.permission:
                this.helper.requestPermission(device);
                break;
            case R.id.disable:
                this.helper.disable();
                break;
            case R.id.release:
                this.helper.release();
                break;
        }
    }

    @Override
    public void onPrintMessage(String message) {
        Log.e("Main","" + message);
    }

    @Override
    public void onDeviceAttached(UsbDevice device) {
        Log.e("Main","DeviceAttached:" + device);
        this.device = device;
    }

    @Override
    public void onDeviceDetached(UsbDevice device) {
        Log.e("Main","DeviceDetached:" + device);
    }

    @Override
    public void onPermissionGranted(UsbDevice device) {
        Log.e("Main","PermissionGranted:" + device);
    }

    @Override
    public void onPermissionCanceled(UsbDevice device) {
        Log.e("Main","PermissionCanceled:" + device);
    }
}
