package cn.qd.peiwen.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PWUsbHelper {
    private Context context;
    private UsbManager manager;

    private PWUsbHandler handler;
    private HandlerThread thread;
    private WeakReference<IPWUsbListener> listener;

    private boolean register = false;
    private Object syncHandler = new Object();

    private final static int USB_MSG_INSERT = 0x01;
    private final static int USB_MSG_FINISH = 0x02;
    private final static int USB_MSG_PROCESS = 0x03;

    private final static int USB_TASK_ENABLE = 0x01;
    private final static int USB_TASK_DISABLE = 0x02;
    private final static int USB_TASK_RELEASE = 0x03;
    private final static int USB_TASK_ATTACHED = 0x04;
    private final static int USB_TASK_DETACHED = 0x05;
    private final static int USB_TASK_DISCOVERY = 0x06;
    private final static int USB_TASK_PERMISSION = 0x07;
    private final static int USB_TASK_PERMISSION_GRANTED = 0x08;
    private final static int USB_TASK_PERMISSION_CANCELED = 0x09;

    private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";

    public PWUsbHelper(Context context) {
        this.context = context;
    }

    public void init() {
        this.createHandler();
        this.manager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
    }

    public void enalbe() {
        this.insertTask(USB_TASK_ENABLE);
    }

    public void disable() {
        this.insertTask(USB_TASK_DISABLE);
    }

    public void discovery() {
        this.insertTask(USB_TASK_DISCOVERY);
    }

    public void release() {
        this.insertTask(USB_TASK_RELEASE);
    }

    public void changeListener(IPWUsbListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    public void requestPermission(UsbDevice device) {
        this.insertTask(USB_TASK_PERMISSION, device);
    }

    private void createHandler() {
        synchronized (syncHandler) {
            if (this.thread == null) {
                this.thread = new HandlerThread("PWUsbHelper");
                this.thread.start();
                this.handler = new PWUsbHandler(this.thread.getLooper());
            }
        }
    }

    private void destroyHandler() {
        synchronized (syncHandler) {
            if (null != this.thread) {
                this.thread.quitSafely();
                this.thread = null;
                this.handler = null;
            }
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        this.context.registerReceiver(receiver, filter);
        this.register = true;
    }

    private void unregisterReceiver() {
        if (this.register) {
            this.context.unregisterReceiver(this.receiver);
        }
    }

    private void insertTask(int type) {
        this.insertTask(type, null);
    }

    private void insertTask(int type, UsbDevice device) {
        synchronized (syncHandler) {
            Message msg = Message.obtain();
            msg.what = USB_MSG_INSERT;
            msg.obj = new PWUsbTaskEntity(type, device);
            if (null != this.handler) {
                this.handler.sendMessage(msg);
            }
        }
    }

    private void processTask() {
        synchronized (syncHandler) {
            if (null != this.handler) {
                this.handler.sendEmptyMessage(USB_MSG_PROCESS);
            }
        }
    }

    private void finishTask() {
        synchronized (syncHandler) {
            if (null != this.handler) {
                this.handler.sendEmptyMessage(USB_MSG_FINISH);
            }
        }
    }

    private void printMessage(String message) {
        if(this.listener != null && this.listener.get() != null) {
            this.listener.get().onPrintMessage(message);
        }
    }

    private void processUsbTask(PWUsbTaskEntity task) {
        switch (task.getType()) {
            case USB_TASK_RELEASE:
                this.unregisterReceiver();
                this.destroyHandler();
                break;
            case USB_TASK_ENABLE:
                this.finishTask();
                this.registerReceiver();
                break;
            case USB_TASK_DISABLE:
                this.finishTask();
                this.unregisterReceiver();
                break;
            case USB_TASK_DISCOVERY:
                this.finishTask();
                this.processUsbDiscovery();
                break;
            case USB_TASK_ATTACHED:
                this.finishTask();
                if(this.listener != null && this.listener.get() != null) {
                    this.listener.get().onDeviceAttached(task.getDevice());
                }
                break;
            case USB_TASK_DETACHED:
                this.finishTask();
                if(this.listener != null && this.listener.get() != null) {
                    this.listener.get().onDeviceDetached(task.getDevice());
                }
                break;
            case USB_TASK_PERMISSION:
                this.finishTask();
                if(this.manager.hasPermission(task.getDevice())) {
                    if(this.listener != null && this.listener.get() != null) {
                        this.listener.get().onPermissionGranted(task.getDevice());
                    }
                } else {
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this.context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    this.manager.requestPermission(task.getDevice(), pendingIntent); // 该代码执
                }
                break;
            case USB_TASK_PERMISSION_GRANTED:
                this.finishTask();
                if(this.listener != null && this.listener.get() != null) {
                    this.listener.get().onPermissionGranted(task.getDevice());
                }
                break;
            case USB_TASK_PERMISSION_CANCELED:
                this.finishTask();
                if(this.listener != null && this.listener.get() != null) {
                    this.listener.get().onPermissionCanceled(task.getDevice());
                }
                break;
        }
    }

    private void processUsbDiscovery() {
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        for (UsbDevice device : devices.values()) {
            this.insertTask(USB_TASK_ATTACHED, device);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra("device");
                if (device == null) {
                    return;
                }
                if (intent.getBooleanExtra("permission", false)) {
                    PWUsbHelper.this.insertTask(USB_TASK_PERMISSION_GRANTED, device);
                } else {
                    PWUsbHelper.this.insertTask(USB_TASK_PERMISSION_CANCELED, device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra("device");
                if (device == null) {
                    return;
                }
                PWUsbHelper.this.insertTask(USB_TASK_ATTACHED, device);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra("device");
                if (device == null) {
                    return;
                }
                PWUsbHelper.this.insertTask(USB_TASK_DETACHED, device);
            }
        }
    };


    private class PWUsbHandler extends Handler {
        private boolean running = false;
        private PWUsbTaskEntity task;
        private List<PWUsbTaskEntity> tasks = new ArrayList<>();

        public PWUsbHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case USB_MSG_INSERT: {
                    PWUsbTaskEntity task = (PWUsbTaskEntity) msg.obj;
                    this.tasks.add(task);
                    PWUsbHelper.this.printMessage("添加任务:" + task.getType());
                    if (!this.running) {
                        PWUsbHelper.this.processTask();
                    }
                    break;
                }
                case USB_MSG_FINISH: {
                    this.running = false;
                    PWUsbHelper.this.printMessage("结束任务:" + this.task.getType());
                    if (!this.tasks.isEmpty()) {
                        PWUsbHelper.this.processTask();
                    }
                    break;
                }
                case USB_MSG_PROCESS: {
                    if (this.running) {
                        PWUsbHelper.this.printMessage("队列繁忙，跳过");
                        break;
                    }
                    if (this.tasks.isEmpty()) {
                        PWUsbHelper.this.printMessage("队列空，跳过");
                        break;
                    }
                    this.running = true;
                    this.task = this.tasks.remove(0);
                    PWUsbHelper.this.printMessage("开始执行任务:" + this.task.getType());
                    PWUsbHelper.this.processUsbTask(this.task);
                    break;
                }
            }
        }
    }
}
