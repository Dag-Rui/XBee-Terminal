package no.daffern.xbeecommunication.Fragment;


import android.app.Activity;
import android.media.AudioManager;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.SeekBar;
import android.widget.TextView;


import no.daffern.xbeecommunication.Audio.PlaybackStreamHelper;
import no.daffern.xbeecommunication.Audio.RecordStreamHelper;
import no.daffern.xbeecommunication.DataTypes;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.MainActivity;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeTransmitFrame;
import no.daffern.xbeecommunication.XBeeService;

/**
 * Created by Daffern on 11.07.2016.
 */


public class VoiceFragment extends Fragment {

    private static final String TAG = VoiceFragment.class.getSimpleName();

    //message codes sent to a remote node
    private static final byte CALL = 1;
    private static final byte HANG_UP = 2;
    private static final byte ACCEPT = 3;
    private static final byte REFUSE = 4;

    Node currentNode;

    XBeeService xBeeService;

    TextView nodeText;
    Button talkButton;
    Button callButton;
    Button acceptButton;
    Button refuseButton;
    SeekBar seekBar;

    View pushToTalkContainer;
    View acceptRefuseContainer;

    private enum CallState {
        none,
        initiatingCall,
        receivingCall,
        inCall
    }
    CallState callState = CallState.none;

    boolean sending = false;

    PlaybackStreamHelper playbackStreamHelper;
    RecordStreamHelper recordStreamHelper;

    public VoiceFragment() {


        xBeeService = XBeeService.getInstance();
        xBeeService.addXBeeFrameListener(new XBeeFrameListener() {
            @Override
            public void onVoiceMessage(XBeeReceiveFrame xBeeReceiveFrame) {

                playbackStreamHelper.addFrame(xBeeReceiveFrame.getRfData());

            }

            @Override
            public void onVoiceStatusMessage(final XBeeReceiveFrame xBeeReceiveFrame) {

                int message = xBeeReceiveFrame.getRfData()[0];

                if (message == CALL) {

                    Node node = xBeeService.getNodeMap().get(Node.getKey(xBeeReceiveFrame.getAddress64()));
                    if (callState == CallState.none) {
                        callState = CallState.receivingCall;

                        setCurrentNode(node);

                        MainActivity.replaceFragment(VoiceFragment.this, true);
                        MainActivity.makeToast("Call from: " + node.getNodeIdentifier());


                        //play the calling sound
                        final MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.getContext(),R.raw.nokia);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mediaPlayer.release();
                            }
                        });

                    }
                    //if already in call, auto decline
                    else{
                        MainActivity.makeToast("Blocked a call from: " + node.getNodeIdentifier());
                    }

                } else if (message == HANG_UP) {
                    callState = CallState.none;
                    MainActivity.makeToast("Call ended");
                } else if (message == ACCEPT) {
                    callState = CallState.inCall;
                } else if (message == REFUSE) {
                    callState = CallState.none;
                }
                updateUI();
            }

        });


    }

    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_voice, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);


        pushToTalkContainer = getView().findViewById(R.id.pushToTalkContainer);
        acceptRefuseContainer = getView().findViewById(R.id.acceptRefuseContainer);

        nodeText = (TextView) getView().findViewById(R.id.nodeText);

        seekBar = (SeekBar)getView().findViewById(R.id.seekBar);

        callButton = (Button) getView().findViewById(R.id.button_call);
        talkButton = (Button) getView().findViewById(R.id.button_talk);
        acceptButton = (Button) getView().findViewById(R.id.button_accept);
        refuseButton = (Button) getView().findViewById(R.id.button_refuse);

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callState == CallState.none) {
                    sendStatusMessage(CALL);
                    callState = CallState.initiatingCall;
                } else {
                    sendStatusMessage(HANG_UP);
                    callState = CallState.none;
                }
                updateUI();
            }
        });
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendStatusMessage(ACCEPT);
                callState = CallState.inCall;
                updateUI();
            }
        });
        refuseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendStatusMessage(REFUSE);
                callState = CallState.none;
                updateUI();
            }
        });


        talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sending) {
                    stopSending();
                } else {
                    startSending();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                recordStreamHelper.setQuality(seekBar.getProgress());
            }
        });

        recordStreamHelper = new RecordStreamHelper();

        recordStreamHelper.setSpeexFrameListener(new RecordStreamHelper.SpeexFrameListener() {
            @Override
            public void onFrameEncoded(byte[] frame) {
                if (sending) {

                    XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(DataTypes.APP_VOICE_MESSAGE);
                    xBeeTransmitFrame.setStatusFrameEnabled(false);
                    xBeeTransmitFrame.setAddress64(currentNode.address64);

                    xBeeTransmitFrame.setRfData(frame);

                    xBeeService.sendFrame(xBeeTransmitFrame);
                }
            }
        });

        playbackStreamHelper = new PlaybackStreamHelper();
        playbackStreamHelper.start();

        recordStreamHelper.start(seekBar.getProgress());
    }


    private void startSending() {
        talkButton.setBackgroundColor(getResources().getColor(R.color.red));
        talkButton.setText("Cancel");
        sending = true;
    }

    private void stopSending() {
        talkButton.setBackgroundColor(getResources().getColor(R.color.green));
        talkButton.setText("Push to talk");
        sending = false;
    }

    private void sendStatusMessage(byte what) {

        XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(DataTypes.APP_VOICE_STATUS_MESSAGE);
        xBeeTransmitFrame.setRfData(new byte[]{what});
        xBeeTransmitFrame.setAddress64(currentNode.address64);

        xBeeService.sendFrame(xBeeTransmitFrame);
    }

    private void updateUI(){

        Activity activity = getActivity();

        if (activity != null){

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    nodeText.setText("Call to: " + currentNode.getNodeIdentifier());

                    switch (callState){
                        case none:
                            callButton.setText("Call");
                            callButton.setBackgroundColor(getResources().getColor(R.color.green));
                            callButton.setVisibility(View.VISIBLE);
                            pushToTalkContainer.setVisibility(View.GONE);
                            acceptRefuseContainer.setVisibility(View.GONE);
                            break;

                        case initiatingCall:
                            callButton.setText("Calling...");
                            callButton.setBackgroundColor(getResources().getColor(R.color.red));
                            pushToTalkContainer.setVisibility(View.GONE);
                            acceptRefuseContainer.setVisibility(View.GONE);

                            break;
                        case receivingCall:
                            callButton.setVisibility(View.GONE);
                            acceptRefuseContainer.setVisibility(View.VISIBLE);
                            pushToTalkContainer.setVisibility(View.GONE);

                            break;
                        case inCall:
                            callButton.setText("Hang up");
                            callButton.setBackgroundColor(getResources().getColor(R.color.red));
                            callButton.setVisibility(View.VISIBLE);
                            pushToTalkContainer.setVisibility(View.VISIBLE);
                            acceptRefuseContainer.setVisibility(View.GONE);

                            break;
                    }
                }
            });
        }

    }



    @Override
    public void onPause() {
        super.onPause();

        stopSending();

    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();

    }


}
