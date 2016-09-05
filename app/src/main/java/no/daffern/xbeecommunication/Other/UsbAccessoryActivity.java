package no.daffern.xbeecommunication.Other;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import no.daffern.xbeecommunication.Fragments.ChatFragment;
import no.daffern.xbeecommunication.Listener.MessageListener;

/**
 * Created by Daffern on 28.05.2016.
 */
public class UsbAccessoryActivity extends AppCompatActivity {
/*
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String TAG = "xbeecommunication";



    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    private UsbManager usbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    boolean isConnected = false;
    ConnectedThread mConnectedThread;

    ChatFragment currentFragment;
    TextView isConnectedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        currentFragment = new ChatFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, currentFragment).commit();

        isConnectedText = (TextView)findViewById(R.id.isConnectedText);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        initUsbAccessory();

        currentFragment.setMessageListener(new MessageListener() {
            @Override
            public boolean onMessageReceived(byte[] bytes) {
                write(bytes);
                return true;
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        tryOpenUsbAccessory();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAccessory();
        unregisterReceiver(mUsbReceiver);
    }


    //start accessory
    private void initUsbAccessory() {
        //mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
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
                UsbAccessory accessory = usbManager.getAccessoryList()[0];
                if (accessory != null && accessory.equals(mAccessory))
                    closeAccessory();
            }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                setConnectionStatus(false);
            }
        }
    };

    public void tryOpenUsbAccessory() {
        if (mAccessory != null) {
            setConnectionStatus(true);
            return;
        }

        UsbAccessory[] accessories = usbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (usbManager.hasPermission(accessory))
                openAccessory(accessory);

            else {
                setConnectionStatus(false);
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        usbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            setConnectionStatus(false);
            Log.d(TAG, "mAccessory is null");
        }
    }


    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = usbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            mConnectedThread = new ConnectedThread(this);
            mConnectedThread.start();

            setConnectionStatus(true);

            Log.d(TAG, "Accessory opened");
        } else {
            setConnectionStatus(false);
            Log.d(TAG, "Accessory open failed");
        }
    }

    private void setConnectionStatus(boolean connected) {
        if (connected == true) {
            isConnectedText.setText(R.string.accessoryConnected);
        }else{
            isConnectedText.setText(R.string.notConnected);
        }
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



    public void write(byte[] bytes) {
        if (mOutputStream != null) {
            try {
                // mOutputStream.write(buffer);
                mOutputStream.write(bytes);


            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        Activity activity;
        byte[] buffer = new byte[1024];
        boolean running;

        ConnectedThread(Activity activity) {
            this.activity = activity;
            running = true;
        }

        public void run() {
            while (running) {
                try {
                    final int bytes = mInputStream.read(buffer);
                    if (bytes > 0) { // The message is 4 bytes long
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                //long timer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong();
                                //mTextView.setText(Long.toString(timer));

                                char[] c = new char[bytes];

                                for (int i = 0; i < bytes; i++) {
                                    c[i] = (char) buffer[i];
                                }

                                //currentFragment.receiveMessage(buffer);
                            }
                        });
                    }
                } catch (Exception ignore) {
                    setConnectionStatus(false);
                }
            }
        }

        public void cancel() {
            running = false;
        }
    }
    //stop accessory
    */
}
