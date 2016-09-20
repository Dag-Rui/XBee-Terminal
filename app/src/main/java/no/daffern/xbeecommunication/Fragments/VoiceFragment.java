package no.daffern.xbeecommunication.Fragments;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import no.daffern.xbeecommunication.Audio.PlaybackHelper;
import no.daffern.xbeecommunication.Audio.PlaybackStreamHelper;
import no.daffern.xbeecommunication.Audio.RecordHelper;
import no.daffern.xbeecommunication.Audio.RecordStreamHelper;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.Utility;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeTransmitFrame;
import no.daffern.xbeecommunication.XBeeService;
import no.daffern.xbeecommunication.XBee.XBeeFrameType;

/**
 * Created by Daffern on 11.07.2016.
 */


public class VoiceFragment extends Fragment {


    Node currentNode;

    XBeeService xBeeService;

    TextView nodeText;

    TextView dataInBps;
    TextView bytesPerFrameOut;
    TextView dataOutBps;
    TextView bytePerFrameIn;

    boolean recording = false;
    PlaybackStreamHelper playbackStreamHelper;
    RecordStreamHelper recordStreamHelper;

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

        final Button talkButton = (Button) getView().findViewById(R.id.button_talk);



        talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording){
                    talkButton.setBackgroundColor(getResources().getColor(R.color.green));

                    recordStreamHelper.stop();
                    recording = false;

                }else{
                    talkButton.setBackgroundColor(getResources().getColor(R.color.red));

                    recordStreamHelper.start();
                    recording = true;

                }
            }
        });

        recordStreamHelper = new RecordStreamHelper();

        recordStreamHelper.setSpeexFrameListener(new RecordStreamHelper.SpeexFrameListener() {
            @Override
            public void onFrameRecorded(byte[] frame) {


                XBeeTransmitFrame xBeeTransmitFrame = new XBeeTransmitFrame(XBeeFrameType.APP_VOICE_MESSAGE);
                xBeeTransmitFrame.setStatusFrameEnabled(false);
                xBeeTransmitFrame.setRfData(frame);
                xBeeTransmitFrame.setAddress64(currentNode.address64);

                xBeeService.sendFrame(xBeeTransmitFrame);
            }
        });

        playbackStreamHelper = new PlaybackStreamHelper();
        playbackStreamHelper.start();

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
        xBeeService = XBeeService.getInstance();
        xBeeService.addXBeeFrameListener(new XBeeFrameListener() {


            @Override
            public void onTransmitStatus(XBeeStatusFrame frame) {

            }

            @Override
            public void onVoiceMessage(XBeeReceiveFrame xBeeReceiveFrame) {
                playbackStreamHelper.addFrame(xBeeReceiveFrame.getRfData());

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

            xBeeService.sendFrame(xBeeTransmitFrame);

            pointer += length;
        }

    }




}
