package no.daffern.xbeecommunication.Fragments;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import no.daffern.xbeecommunication.DataTypes;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.Model.SmsMessage;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeTransmitFrame;
import no.daffern.xbeecommunication.XBeeService;

/**
 * Created by Daffern on 07.07.2016.
 */
public class SmsFragment extends Fragment {

    private final static String TAG = SmsFragment.class.getSimpleName();

    Node node;
    XBeeService xBeeService;

    EditText phoneField;
    EditText smsTextField;

    TextView statusText;
    Button sendButton;


    Map<Integer, Map<Byte, SmsMessage>> smsMessageMap;  //node address hashkey, sms id, sms message


    public SmsFragment() {
        smsMessageMap = new HashMap<>();

        xBeeService = XBeeService.getInstance();
        xBeeService.addXBeeFrameListener(new XBeeFrameListener() {

            @Override
            public void onSmsMessage(XBeeReceiveFrame frame) {
                byte[] rfData = frame.getRfData();

                byte smsId = rfData[0];
                byte numberSize = rfData[1];

                String number = new String(rfData, 2, numberSize);

                String message = new String(rfData, numberSize + 2, rfData.length - numberSize - 2);

                int key = Node.getKey(frame.getAddress64());

                Map<Byte, SmsMessage> smsMap = smsMessageMap.get(key);
                if (smsMap == null) {
                    smsMap = new HashMap<Byte, SmsMessage>();
                }

                SmsMessage smsMessage = new SmsMessage(number, message, false, SmsMessage.SMS_RECEIVED_BY_REMOTE, smsId);
                smsMap.put(smsId, smsMessage);


                sendSms(smsId, number, message);
            }

            @Override
            public void onSmsStatusMessage(XBeeReceiveFrame frame) {


                byte[] rfData = frame.getRfData();
                byte smsId = rfData[0];
                byte status = rfData[1];

                Map<Byte, SmsMessage> smsMap = smsMessageMap.get(Node.getKey(frame.getAddress64()));
                if (smsMap == null) {
                    Log.e(TAG, "Error, received status message from unknown node");
                    return;
                }

                SmsMessage smsMessage = smsMap.get(smsId);
                smsMessage.setStatus(status);



                switch (status) {
                    case SmsMessage.REMOTE_MOBILE_SENT_SMS:
                        statusText.append(getString(R.string.REMOTE_MOBILE_SENT_SMS));
                        break;
                    case SmsMessage.REMOTE_MOBILE_ERROR_GENERIC_FAILURE:
                        statusText.append(getString(R.string.REMOTE_MOBILE_ERROR_GENERIC_FAILURE));
                        break;
                    case SmsMessage.REMOTE_MOBILE_RESULT_ERROR_NO_SERVICE:
                        statusText.append(getString(R.string.REMOTE_MOBILE_RESULT_ERROR_NO_SERVICE));
                        break;
                    case SmsMessage.REMOTE_MOBILE_RESULT_ERROR_NULL_PDU:
                        statusText.append(getString(R.string.REMOTE_MOBILE_RESULT_ERROR_NULL_PDU));
                        break;
                    case SmsMessage.REMOTE_MOBILE_RESULT_ERROR_RADIO_OFF:
                        statusText.append(getString(R.string.REMOTE_MOBILE_RESULT_ERROR_RADIO_OFF));
                        break;
                    case SmsMessage.TARGET_MOBILE_RECEIVED_SMS:
                        statusText.append(getString(R.string.TARGET_MOBILE_RECEIVED_SMS));
                        break;
                }

                statusText.append("\n");

            }

            @Override
            public void onTransmitStatus(XBeeStatusFrame xBeeStatusFrame) {
                //statusText.append();

            }

        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_sms, container, false);
    }

    Activity activity;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        activity = getActivity();

        initUI();

    }

    public void setCurrentNode(Node node) {
        this.node = node;
    }

    private void initUI() {
        phoneField = (EditText) getActivity().findViewById(R.id.phoneTextField);
        smsTextField = (EditText) getActivity().findViewById(R.id.smsTextField);
        statusText = (TextView) getActivity().findViewById(R.id.smsStatusText);
        sendButton = (Button) getActivity().findViewById(R.id.sendButton);


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] number = phoneField.getText().toString().getBytes();
                byte[] message = smsTextField.getText().toString().getBytes();

                if (number.length == 0) {
                    Toast.makeText(getActivity(), "No number", Toast.LENGTH_SHORT).show();
                    return;
                } else if (number.length > 15) {
                    Toast.makeText(getActivity(), "Phone number is too long", Toast.LENGTH_SHORT).show();
                    return;
                } else if (message.length == 0) {
                    Toast.makeText(getActivity(), "No Message", Toast.LENGTH_SHORT).show();
                    return;
                } else if (message.length > XBeeTransmitFrame.MAX_RF_DATA - number.length - 1) {
                    Toast.makeText(getActivity(), "Message is too long", Toast.LENGTH_SHORT).show();
                    return;
                }


                Map<Byte, SmsMessage> smsMap = smsMessageMap.get(node.getKey());
                if (smsMap == null) {
                    smsMap = new HashMap<Byte, SmsMessage>();
                    smsMessageMap.put(node.getKey(), smsMap);
                }

                SmsMessage smsMessage = new SmsMessage(number, message, true, SmsMessage.SMS_SENT_TO_REMOTE);

                smsMap.put(smsMessage.getSmsId(), smsMessage);


                byte[] rfData = new byte[2 + number.length + message.length];
                rfData[0] = smsMessage.getSmsId();
                rfData[1] = (byte) number.length;//store the length of the phone number in the second byte
                System.arraycopy(number, 0, rfData, 2, number.length);//store the phone number
                System.arraycopy(message, 0, rfData, number.length + 2, message.length);//store the message


                XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(DataTypes.APP_SMS_MESSAGE);
                xBeeTransmitFrame.setAddress64(node.address64);
                xBeeTransmitFrame.setRfData(rfData);


                xBeeService.sendFrame(xBeeTransmitFrame);
            }
        });

    }


    private void sendSms(final byte smsId, final String number, final String message) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String SMS_SENT = "SMS_SENT";
                String SMS_DELIVERED = "SMS_DELIVERED";
                PendingIntent sentIntent = PendingIntent.getBroadcast(activity, 0, new Intent(SMS_SENT), 0);

                activity.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int resultCode = getResultCode();
                        switch (resultCode) {
                            case Activity.RESULT_OK:
                                sendStatusMessage(smsId, SmsMessage.REMOTE_MOBILE_SENT_SMS);
                                break;
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                sendStatusMessage(smsId, SmsMessage.REMOTE_MOBILE_ERROR_GENERIC_FAILURE);
                                break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE:
                                sendStatusMessage(smsId, SmsMessage.REMOTE_MOBILE_RESULT_ERROR_NO_SERVICE);
                                break;
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                sendStatusMessage(smsId, SmsMessage.REMOTE_MOBILE_RESULT_ERROR_NULL_PDU);
                                break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                                sendStatusMessage(smsId, SmsMessage.REMOTE_MOBILE_RESULT_ERROR_RADIO_OFF);
                                break;
                        }
                    }
                }, new IntentFilter(SMS_SENT));


                PendingIntent deliveredIntent = PendingIntent.getBroadcast(activity, 0, new Intent(SMS_DELIVERED), 0);

                activity.registerReceiver(new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        sendStatusMessage(smsId, SmsMessage.TARGET_MOBILE_RECEIVED_SMS);
                    }

                }, new IntentFilter(SMS_DELIVERED));



                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number, null, message, sentIntent, deliveredIntent);
            }
        });




    }

    private void sendStatusMessage(byte smsId, byte status) {

        XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(DataTypes.APP_SMS_STATUS_MESSAGE);
        xBeeTransmitFrame.setAddress64(node.address64);
        xBeeTransmitFrame.setRfData(new byte[]{smsId, status});
        xBeeService.sendFrame(xBeeTransmitFrame);
    }
}
