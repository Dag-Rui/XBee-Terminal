package no.daffern.xbeecommunication;


import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import no.daffern.xbeecommunication.Fragment.ChatFragment;
import no.daffern.xbeecommunication.Fragment.NodeListFragment;
import no.daffern.xbeecommunication.Fragment.PerformanceFragment;
import no.daffern.xbeecommunication.Fragment.SmsFragment;
import no.daffern.xbeecommunication.Fragment.StartFragment;
import no.daffern.xbeecommunication.Fragment.VoiceFragment;
import no.daffern.xbeecommunication.Listener.MessageListener;
import no.daffern.xbeecommunication.Listener.NodeListListener;
import no.daffern.xbeecommunication.Listener.UsbListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.USB.UsbAccessoryWrapper;
import no.daffern.xbeecommunication.USB.UsbHostWrapper;
import no.daffern.xbeecommunication.XBee.Frames.XBeeFrame;
import no.daffern.xbeecommunication.XBee.XBeeFrameBuffer;

/**
 * Created by Daffern on 04.06.2016.
 *
 * Initializes the UI together with the fragments and the USB wrappers
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private UsbAccessoryWrapper usbAccessoryWrapper;
    private UsbHostWrapper usbHostWrapper;

    private ChatFragment chatFragment;
    private NodeListFragment nodeListFragment;
    private StartFragment startFragment;
    private VoiceFragment voiceFragment;
    private SmsFragment smsFragment;

    private PerformanceFragment performanceFragment;

    private TextView isConnectedText;
    private Toolbar toolbar;


    private XBeeFrameBuffer xBeeFrameBuffer;
    private XBeeService xBeeService;

    public static Context context;
    public static MainActivity mainActivity;

    private enum ConnectedState {
        notConnected,
        connectedAsHost,
        connectedAsAccessory
    }

    private ConnectedState connectedState = ConnectedState.notConnected;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //If the application is already open, the intent for usb will be received here
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(UsbManager.EXTRA_DEVICE)) {
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        } else if (intent.hasExtra(UsbManager.EXTRA_ACCESSORY)) {
            UsbAccessory usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Overrides exception handling caused by annoying debug apps
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ex.printStackTrace();
            }
        });

        xBeeFrameBuffer = new XBeeFrameBuffer();
        xBeeService = XBeeService.getInstance();

        xBeeService.setMessageListener(new MessageListener() {
            @Override
            public boolean onMessageReceived(byte[] bytes) {
                return handleOutgoingMessage(bytes);
            }
        });

        initInterface();
        initFragments();
        initUsbHandlers();

        mainActivity = this;
        context = getBaseContext();
    }

    private void initInterface() {
        setContentView(R.layout.activity_main);

        isConnectedText = (TextView) findViewById(R.id.isConnectedText);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.menu_performance:
                        if (item.isChecked()) {
                            item.setChecked(false);

                            FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                            fr.remove(performanceFragment);
                            fr.commit();

                        } else {
                            item.setChecked(true);

                            performanceFragment = new PerformanceFragment();
                            FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                            fr.add(R.id.fragment_container2, performanceFragment, "performanceFragment");

                            //fr.addToBackStack(startFragment.getClass().getSimpleName());
                            fr.commit();

                        }
                        break;
                }
                return false;
            }
        });
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                }
        );
    }

    private void initFragments() {
        chatFragment = new ChatFragment();
        nodeListFragment = new NodeListFragment();
        startFragment = new StartFragment();
        voiceFragment = new VoiceFragment();
        smsFragment = new SmsFragment();

        FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
        fr.add(R.id.fragment_container, startFragment, "startFragment");
        //fr.addToBackStack(startFragment.getClass().getSimpleName());
        fr.commit();

        startFragment.setButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceFragment(nodeListFragment, true);
            }
        });

        nodeListFragment.setNodeListListener(new NodeListListener() {

            @Override
            public void onChatClicked(Node node) {
                chatFragment.setCurrentNode(node);
                replaceFragment(chatFragment, true);
            }

            @Override
            public void onSmsClicked(Node node) {
                smsFragment.setCurrentNode(node);
                replaceFragment(smsFragment, true);
            }

            @Override
            public void onCallClicked(Node node) {
                voiceFragment.setCurrentNode(node);
                replaceFragment(voiceFragment, true);
            }
        });
    }

    private boolean handleOutgoingMessage(byte[] bytes) {
        if (connectedState == ConnectedState.connectedAsAccessory) {
            usbAccessoryWrapper.write(bytes);
            return true;
        } else if (connectedState == ConnectedState.connectedAsHost) {
            usbHostWrapper.write(bytes);
            return true;
        } else {

            makeToast("USB not Connected");

            return false;
        }
    }

    private boolean isUsbConnected() {
        if (connectedState == ConnectedState.connectedAsAccessory) {
            return true;
        } else if (connectedState == ConnectedState.connectedAsHost) {
            return true;
        } else {
            return false;
        }
    }

    private void initUsbHandlers() {
        usbAccessoryWrapper = new UsbAccessoryWrapper(this);
        usbAccessoryWrapper.setUsbListener(new UsbListener() {
            @Override
            public void onDataReceived(byte[] bytes) {
                bufferFrame(bytes);

            }

            @Override
            public void onConnectionStatusChanged(final boolean connected) {

                //Needs to run on the UI thread
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (connected) {
                            connectedState = ConnectedState.connectedAsAccessory;
                            isConnectedText.setText(R.string.accessoryConnected);
                            startFragment.startReadXBeeParameters();
                        } else {
                            connectedState = ConnectedState.notConnected;
                            isConnectedText.setText(R.string.notConnected);
                        }
                    }
                });
            }
        });

        usbHostWrapper = new UsbHostWrapper(this);
        usbHostWrapper.setUsbListener(new UsbListener() {
            @Override
            public void onDataReceived(byte[] bytes) {
                bufferFrame(bytes);

            }

            @Override
            public void onConnectionStatusChanged(final boolean connected) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connected) {
                            connectedState = ConnectedState.connectedAsHost;
                            isConnectedText.setText(R.string.hostConnected);
                            startFragment.startReadXBeeParameters();
                        } else {
                            connectedState = ConnectedState.notConnected;
                            isConnectedText.setText(R.string.notConnected);
                        }
                    }
                });
            }
        });
    }
    //buffers data received through USB
    private void bufferFrame(byte[] bytes) {
        ArrayList<XBeeFrame> frames;
        try {
            frames = xBeeFrameBuffer.putAndCheck(bytes);

            for (XBeeFrame frame : frames) {
                xBeeService.receiveFrame(frame);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void replaceFragment(final Fragment fragment, final boolean addToBackStack) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println("entering replaceFramgnet");
                String backStateName = fragment.getClass().getName();

                FragmentManager manager = mainActivity.getSupportFragmentManager();

                boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

                if (!fragmentPopped) { //fragment not in back stack, create it.
                    FragmentTransaction ft = manager.beginTransaction();
                    ft.replace(R.id.fragment_container, fragment);
                    if (addToBackStack)
                        ft.addToBackStack(backStateName);
                    ft.commit();
                }
            }
        });
    }

    //static method to make a Toast(notice message on the screen)
    private static long lastToastTime = 0;
    private static String lastToast;

    public static void makeToast(final String toast) {

        if (System.currentTimeMillis() > lastToastTime + 5000 || !lastToast.equals(toast)) {
            lastToastTime = System.currentTimeMillis();
            lastToast = toast;

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainActivity, toast, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static Context getContext() {
        return context;
    }

    @Override
    public void onResume() {
        super.onResume();
        usbHostWrapper.onResume();
        //usbAccessoryWrapper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //usbAccessoryWrapper.onDestroy();
        //storeData();
    }
}
