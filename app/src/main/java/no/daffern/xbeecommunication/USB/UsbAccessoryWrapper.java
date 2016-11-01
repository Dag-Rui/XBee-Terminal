package no.daffern.xbeecommunication.USB;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.daffern.xbeecommunication.Listener.UsbListener;

/**
 * Created by Daffern on 10.06.2016.
 */
public class UsbAccessoryWrapper  {

    private static final String TAG = UsbAccessoryWrapper.class.getSimpleName();

    private static final String ACTION_USB_ACCESSORY_PERMISSION = "com.android.example.USB_ACCESSORY_PERMISSION";


    private Activity activity;


    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    private UsbManager usbManager;

    private boolean mPermissionRequestPending;
    boolean isConnected = false;
    ConnectedThread mConnectedThread;

    UsbListener usbListener;

    public UsbAccessoryWrapper(Activity activity) {
        this.activity = activity;

        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);

        initUsbAccessory();
    }


    //start accessory
    private void initUsbAccessory() {
        //mUsbManager = UsbManager.getInstance(this);

        IntentFilter filter = new IntentFilter(ACTION_USB_ACCESSORY_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        activity.registerReceiver(mUsbReceiver, filter);
    }

    public void setUsbListener(UsbListener usbListener) {
        this.usbListener = usbListener;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_ACCESSORY_PERMISSION.equals(action)) {
                synchronized (this) {


                    //UsbAccessory accessory = UsbManager.getAccessory(intent);
                    UsbAccessory accessory = usbManager.getAccessoryList()[0];
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        openAccessory(accessory);
                    else {
                        Log.d(TAG, "Permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                //UsbAccessory accessory = UsbManager.getAccessory(intent);
                setConnectionStatus(false);
                //UsbAccessory accessory = usbManager.getAccessoryList()[0];
                //if (accessory != null && accessory.equals(mAccessory))
                closeAccessory();
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbAccessory accessory = usbManager.getAccessoryList()[0];
                requestUserPermission(accessory);

            }
        }
    };

    public void tryOpenUsbAccessory() {
        if (mAccessory != null) {
            setConnectionStatus(true);
            return;
        }

        UsbAccessory[] accessories = usbManager.getAccessoryList();
        UsbAccessory accessory = null;
        if (accessories != null) {
            accessory = accessories[0];
        }
        if (accessory != null) {
            if (usbManager.hasPermission(accessory))
                openAccessory(accessory);

            else {
                setConnectionStatus(false);
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        PendingIntent intent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_ACCESSORY_PERMISSION), 0);
                        usbManager.requestPermission(accessory, intent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            setConnectionStatus(false);
            Log.d(TAG, "mAccessory is null");
        }
    }

    private void requestUserPermission(UsbAccessory accessory) {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_ACCESSORY_PERMISSION), 0);
        usbManager.requestPermission(accessory, mPendingIntent);
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = usbManager.openAccessory(accessory);

        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);


            mConnectedThread = new ConnectedThread();
            mConnectedThread.start();

            setConnectionStatus(true);

            Log.d(TAG, "Accessory opened");
        } else {
            setConnectionStatus(false);
            Log.d(TAG, "Accessory open failed");
        }
    }

    private void setConnectionStatus(boolean connected) {

        usbListener.onConnectionStatusChanged(connected);

        isConnected = connected;
    }

    private void closeAccessory() {


        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Close all streams
        try {
            if (mInputStream != null)
                mInputStream.close();
        } catch (Exception ignored) {
        } finally {
            mInputStream = null;
        }
        try {
            if (mOutputStream != null)
                mOutputStream.close();
        } catch (Exception ignored) {
        } finally {
            mOutputStream = null;
        }
        try {
            if (mFileDescriptor != null)
                mFileDescriptor.close();
        } catch (IOException ignored) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    ConcurrentLinkedQueue<byte[]> outBuffer = new ConcurrentLinkedQueue<>();
    boolean lock = false;


    public void write(byte[] bytes) {


        outBuffer.add(bytes);
        try {


            while (outBuffer.size() > 0) {
                if (outBuffer.peek() != null)
                    mOutputStream.write(outBuffer.poll());


            }


        } catch (IOException e) {
            Log.e(TAG, "write failed", e);
        }


    }


    private class ConnectedThread extends Thread {

        byte[] buffer = new byte[16384];
        boolean running;

        ConnectedThread() {
            running = true;
        }

        public void run() {
            while (running) {


                try {
                    final int bytes = mInputStream.read(buffer);

                    if (bytes > 0) {
                        final byte[] newBuffer = new byte[bytes];
                        System.arraycopy(buffer, 0, newBuffer, 0, bytes);

                        usbListener.onDataReceived(newBuffer);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "read failed", e);
                    setConnectionStatus(false);
                    cancel();
                }



            }
        }

        public void cancel() {
            running = false;
        }
    }
}