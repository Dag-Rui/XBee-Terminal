package no.daffern.xbeecommunication.Fragments;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import no.daffern.xbeecommunication.Audio.PlaybackHelper;
import no.daffern.xbeecommunication.Audio.RecordHelper;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.Utility;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeTransmitFrame;
import no.daffern.xbeecommunication.XBee.XBeeService;
import no.daffern.xbeecommunication.XBee.XBeeFrameType;

/**
 * Created by Daffern on 11.07.2016.
 */


public class VoiceFragment extends Fragment {


    Node currentNode;

    RecordHelper recordHelper;
    PlaybackHelper playbackHelper;

    XBeeService XBeeService;

    TextView nodeText;

    TextView dataInBps;
    TextView bytesPerFrameOut;
    TextView dataOutBps;
    TextView bytePerFrameIn;

    Switch instantVoiceSwitch;
    Switch instantEncodeSwitch;
    Switch instantDecodeSwitch;

    ProgressBar progressBar;
    TextView progressText;

    public void setCurrentNode(Node node) {
        this.currentNode = node;
        if (nodeText != null) {
            if (node.nodeIdentifier.length() > 0)
                nodeText.setText("Talking to: "+node.nodeIdentifier);
            else
                nodeText.setText("Talking to: "+Utility.bytesToHex(node.address64));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_voice, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        nodeText = (TextView) getView().findViewById(R.id.nodeText);
        if (currentNode != null)
            if (currentNode.nodeIdentifier.length() > 0)
                nodeText.setText("Talking to: "+currentNode.nodeIdentifier);
            else
                nodeText.setText("Talking to: "+Utility.bytesToHex(currentNode.address64));

        dataInBps = (TextView) getView().findViewById(R.id.bps_in_text);
        bytePerFrameIn = (TextView) getView().findViewById(R.id.data_in_per_frame);
        dataOutBps = (TextView) getView().findViewById(R.id.bps_out_text);
        bytesPerFrameOut = (TextView) getView().findViewById(R.id.data_out_per_frame_text);

        Button button = (Button) getView().findViewById(R.id.button_talk);

        instantVoiceSwitch = (Switch) getView().findViewById(R.id.instantVoiceSwitch);
        instantEncodeSwitch = (Switch) getView().findViewById(R.id.instantEncodeSwitch);
        instantDecodeSwitch = (Switch) getView().findViewById(R.id.instantDecodeSwitch);

        progressBar = (ProgressBar)view.findViewById(R.id.codecProgressBar);
        progressBar.setMax(100);
        progressText = (TextView)view.findViewById(R.id.codecBarText);


        playbackHelper = new PlaybackHelper();
        recordHelper = new RecordHelper();


        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    recordHelper.startRecord();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    recordHelper.stopRecording();
                }

                return true;
            }
        });

        recordHelper.setRecordAudioListener(new RecordHelper.RecordListener() {
            @Override
            public void onRecordFinnish(ArrayList<byte[]> arrayList) {
                recordHelper.startEncoding();
            }

            @Override
            public void onEncodeProgress(int percent) {
                progressText.setText("Encoding message...");
                progressBar.setProgress(percent);
            }

            @Override
            public void onEncodeFinnish(ArrayList<byte[]> arrayList) {

                for (byte[] bytes : arrayList){

                    XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(XBeeFrameType.APP_VOICE_MESSAGE);
                    xBeeTransmitFrame.setRfData(bytes);

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });



        Button playButton = (Button)view.findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playbackHelper.startDecode();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }


    public VoiceFragment() {
        XBeeService = XBeeService.getInstance();
        XBeeService.addXBeeFrameListener(new XBeeFrameListener() {


            @Override
            public void onTransmitStatus(XBeeStatusFrame frame) {

            }

            @Override
            public void onVoiceMessage(XBeeReceiveFrame xBeeReceiveFrame) {
                playbackHelper.addDecoded(xBeeReceiveFrame.getRfData());

                progressText.setText("Received " + playbackHelper.getEncodedSamples().size() + " audio samples");


            }


            @Override
            public void onVoiceStatusMessage(XBeeReceiveFrame xBeeReceiveFrame) {

            }
        });


    }

    public void send(byte[] bytes) {

        int pointer = 0;
        while (pointer < bytes.length) {

            int length = XBeeTransmitFrame.MAX_RF_DATA;
            if (pointer + length > bytes.length) {
                length = bytes.length - pointer;
            }

            byte[] buffer = new byte[length];

            System.arraycopy(bytes, pointer, buffer, 0, length);

            XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(XBeeFrameType.APP_VOICE_MESSAGE);
            xBeeTransmitFrame.setRfData(buffer);

            XBeeService.sendFrame(xBeeTransmitFrame);

            pointer += length;
        }

    }


}
