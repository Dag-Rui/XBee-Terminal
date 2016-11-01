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

import no.daffern.xbeecommunication.Listener.UsbListener;
import no.daffern.xbeecommunication.MainActivity;

/**
 * Created by Daffern on 10.06.2016.
 */
public class UsbHostWrapper {

    private static final String ACTION_USB_HOST_PERMISSION = "com.android.example.USB_HOST_PERMISSION";

    private static final int BAUD_RATE = 57600;

    private Activity activity;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;

    private UsbListener usbListener;

    public UsbHostWrapper(Activity activity) {
        this.activity = activity;

        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);

        serialPortConnected = false;
        setFilter();

        findSerialPortDevice();
    }

    public void tryOpenUsbDevice(UsbDevice usbDevice) {
        if (usbManager.hasPermission(usbDevice)) {

            device = usbDevice;
            connection = usbManager.openDevice(device);
            new ConnectionThread().start();

        } else {
            requestUserPermission(usbDevice);
        }
    }
    public void onResume() {
        UsbDevice usbDevice = findSerialPortDevice();
        if (usbDevice != null){
            tryOpenUsbDevice(usbDevice);
        }
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();

            if (action.equals(ACTION_USB_HOST_PERMISSION)) {

                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    tryOpenUsbDevice(usbDevice);
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                if (!serialPortConnected) {

                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (usbDevice == null){
                        usbDevice = findSerialPortDevice();
                    }
                    tryOpenUsbDevice(usbDevice);

                }
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

        usbListener.onConnectionStatusChanged(connected);

        serialPortConnected = connected;
    }

    public void setUsbListener(UsbListener usbListener) {
        this.usbListener = usbListener;
    }

    private UsbDevice findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {

            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                //TODO check if device is ok to use

                return device;
            }
        }
        return null;
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_HOST_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        //filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        activity.registerReceiver(usbReceiver, filter);

    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission(UsbDevice usbDevice) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_HOST_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, permissionIntent);
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

                    setConnectionStatus(true);
                } else {
                    MainActivity.makeToast("Could not open USB device connection");
                }
            } else {
                MainActivity.makeToast("No driver for the USB device");
            }
        }
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            usbListener.onDataReceived(bytes);
        }
    };

    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {

            if (state)
                MainActivity.makeToast("CTS FALSE");
            else
                MainActivity.makeToast("CTS TRUE");

            //Not used
        }
    };

    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if (state)
                MainActivity.makeToast("DSR FALSE");
            else
                MainActivity.makeToast("DSR TRUE");

            //Not used
        }
    };

}
