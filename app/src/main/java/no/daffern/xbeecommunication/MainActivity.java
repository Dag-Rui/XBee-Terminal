package no.daffern.xbeecommunication;


import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import no.daffern.xbeecommunication.Audio.RecordHelper;
import no.daffern.xbeecommunication.Fragments.ChatFragment;
import no.daffern.xbeecommunication.Fragments.NodeListFragment;
import no.daffern.xbeecommunication.Fragments.SmsFragment;
import no.daffern.xbeecommunication.Fragments.StartFragment;
import no.daffern.xbeecommunication.Fragments.VoiceFragment;
import no.daffern.xbeecommunication.Listener.MessageListener;
import no.daffern.xbeecommunication.Listener.NodeListListener;
import no.daffern.xbeecommunication.Listener.UsbListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.USB.UsbAccessoryWrapper;
import no.daffern.xbeecommunication.USB.UsbHostWrapper;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeFrame;
import no.daffern.xbeecommunication.XBee.XBeeFrameBuffer;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    UsbAccessoryWrapper usbAccessoryWrapper;
    UsbHostWrapper usbHostWrapper;

    ChatFragment chatFragment;
    NodeListFragment nodeListFragment;
    StartFragment startFragment;
    VoiceFragment voiceFragment;
    SmsFragment smsFragment;


    TextView isConnectedText;
    Toolbar toolbar;


    XBeeFrameBuffer xBeeFrameBuffer;
    XBeeService xBeeService;


    enum ConnectedState {
        notConnected,
        connectedAsHost,
        connectedAsAccessory
    }

    ConnectedState connectedState = ConnectedState.notConnected;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    static boolean init = false;

    //If the application is already open, the intent for usb will be received here
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(UsbManager.EXTRA_DEVICE)) {
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        } else if (intent.hasExtra(UsbManager.EXTRA_ACCESSORY)) {
            UsbAccessory usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        }

        usbAccessoryWrapper.tryOpenUsbAccessory();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        RecordHelper recordHelper = new RecordHelper();
        recordHelper.findAudioRecord();


    }


    private void initInterface() {
        setContentView(R.layout.activity_main);

        isConnectedText = (TextView) findViewById(R.id.isConnectedText);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

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
        fr.add(R.id.fragment_container, startFragment, "nodeList");
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
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                }
            });
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

    public void bufferFrame(byte[] bytes) {
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


    public void storeData() {/*
        File cacheDir = getCacheDir();
        File messageFile = new File(cacheDir, "data");


        try {
            FileOutputStream fileOutputStream = new FileOutputStream(messageFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(networkHandler.getMessageMap());
            objectOutputStream.writeObject(networkHandler.getNodeMap());
            objectOutputStream.flush();
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void loadData() {/*
        File cacheDir = getCacheDir();
        File messageFile = new File(cacheDir, "data");

        if (messageFile.exists()) {

            try {
                FileInputStream fileInputStream = new FileInputStream(messageFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                networkHandler.setMessageMap((LinkedHashMap<Integer, Node>) objectInputStream.readObject());
                networkHandler.setNodeMap((LinkedHashMap<Integer, ArrayList<ChatMessage>>) objectInputStream.readObject());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (StreamCorruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();

            }
        }*/
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        String backStateName = fragment.getClass().getName();

        FragmentManager manager = getSupportFragmentManager();

        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped) { //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.fragment_container, fragment);
            if (addToBackStack)
                ft.addToBackStack(backStateName);
            ft.commit();
        }
    }
/*
    private void switchFragment(Fragment fragment) {

        if (fragment == currentFragment) {
            return;
        }
        if (fragment == chatFragment) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, chatFragment, "chat").commit();

        } else if (fragment == nodeListFragment) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, nodeListFragment, "chat").commit();
        }
        currentFragment = fragment;
    }
*/

    @Override
    public void onResume() {
        super.onResume();
        if (connectedState == ConnectedState.connectedAsAccessory) {
            usbAccessoryWrapper.tryOpenUsbAccessory();
        } else {

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (connectedState == ConnectedState.connectedAsHost) {

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //usbAccessoryWrapper.onDestroy();

        //storeData();
    }

}
