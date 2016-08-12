package no.daffern.xbeecommunication.USB;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daffern on 10.06.2016.
 */
public class  UsbHostWrapper {

    private static final String ACTION_USB_HOST_PERMISSION = "com.android.example.USB_HOST_PERMISSION";


    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    public static final int CONNECTED = 3;
    public static final int DISCONNECTED = 4;

    private static final int BAUD_RATE = 57600;

    private Activity activity;

    private UsbManager usbManager;
    private Handler mHandler;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;

    public UsbHostWrapper(Activity activity) {
        this.activity = activity;

        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);

        serialPortConnected = false;
        setFilter();
        findSerialPortDevice();
    }


    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            if (mHandler != null)
                mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, bytes).sendToTarget();
        }
    };

    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if (mHandler != null)
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();

        }
    };

    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if (mHandler != null)
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        }
    };
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_USB_HOST_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                UsbDevice us = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (us != null) {
                    device = us;
                }
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    //findSerialPortDevice();

                    connection = usbManager.openDevice(device);
                    setConnectionStatus(true);
                    new ConnectionThread().run();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {

                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                setConnectionStatus(false);
                if (serialPort != null)
                    serialPort.close();
            }
        }
    };

    public void write(byte[] data) {
        if (serialPort != null) {

            serialPort.write(data);


        }

    }

    private void setConnectionStatus(boolean connected) {

        if (connected)
            mHandler.obtainMessage(CONNECTED).sendToTarget();
        else
            mHandler.obtainMessage(DISCONNECTED).sendToTarget();
        serialPortConnected = connected;
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                if (deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    if (!usbManager.hasPermission(device))
                        requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.

            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity

        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_HOST_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        activity.registerReceiver(usbReceiver, filter);

    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_HOST_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }


    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);

                    serialPort.getCTS(ctsCallback);
                    serialPort.getDSR(dsrCallback);
                    // Everything went as expected. Send an intent to MainActivity

                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {

                    } else {

                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded

            }
        }
    }

}
