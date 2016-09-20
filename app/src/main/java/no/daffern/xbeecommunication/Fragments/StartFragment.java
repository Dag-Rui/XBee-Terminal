package no.daffern.xbeecommunication.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import java.util.Arrays;

import no.daffern.xbeecommunication.Listener.XBeeFrameListener;

import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.Utility;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandResponseFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;
import no.daffern.xbeecommunication.XBeeService;
import no.daffern.xbeecommunication.XBeeConfigVars;

/**
 * Created by Daffern on 25.06.2016.
 */
public class StartFragment extends Fragment {

    View.OnClickListener tempOnClickListener;

    TextView hardware;
    TextView firmware;
    TextView address;
    TextView operatingChannel;
    TextView networkId;


    XBeeService XBeeService;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_start, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        Button button = (Button) getActivity().findViewById(R.id.start_nodeList);
        button.setOnClickListener(tempOnClickListener);

        hardware = (TextView) getActivity().findViewById(R.id.hardware);
        firmware = (TextView) getActivity().findViewById(R.id.firmware);
        address = (TextView) getActivity().findViewById(R.id.address);
        operatingChannel = (TextView) getActivity().findViewById(R.id.operatingChannel);
        networkId = (TextView) getActivity().findViewById(R.id.networkId);


        updateUI();
    }

    public void updateUI(){

        //Run ui updates on ui thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                address.setText("Serial number: "+Utility.bytesToHex(XBeeConfigVars.serialNumberHigh) + " " + Utility.bytesToHex(XBeeConfigVars.serialNumberLow));
                firmware.setText("Firmware version: "+Utility.bytesToHex(XBeeConfigVars.firmwareVersion));
                hardware.setText("Hardware version: "+Utility.bytesToHex(XBeeConfigVars.hardwareVersion));
                operatingChannel.setText("Operating channel: "+Utility.bytesToHex(XBeeConfigVars.operatingChannel));
                networkId.setText("Network ID: "+Utility.bytesToHex(XBeeConfigVars.networkId));
            }
        });
    }

    public StartFragment() {

        XBeeService = XBeeService.getInstance();

        XBeeService.addXBeeFrameListener(new XBeeFrameListener() {
            @Override
            public void onNodeUpdate() {
            }

            @Override
            public void onChatMessage(XBeeReceiveFrame frame) {
            }

            @Override
            public void onTransmitStatus(XBeeStatusFrame frame) {
            }

            @Override
            public void onSmsMessage(XBeeReceiveFrame frame) {
            }

            @Override
            public void onVoiceMessage(XBeeReceiveFrame xBeeReceiveFrame) {
            }

            @Override
            public void onATCommandResponse(XBeeATCommandResponseFrame frame) {

                if (Arrays.equals(frame.getCommand(), "SH".getBytes())) {
                    XBeeConfigVars.serialNumberHigh = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "SL".getBytes())) {
                    XBeeConfigVars.serialNumberLow = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "VR".getBytes())) {
                    XBeeConfigVars.firmwareVersion = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "HV".getBytes())) {
                    XBeeConfigVars.hardwareVersion = frame.getCommandData();}
                else if (Arrays.equals(frame.getCommand(), "CH".getBytes())) {
                    XBeeConfigVars.operatingChannel = frame.getCommandData();
                }else if (Arrays.equals(frame.getCommand(), "ID".getBytes())) {
                    XBeeConfigVars.networkId = frame.getCommandData();
                }else if (Arrays.equals(frame.getCommand(), "N?".getBytes())) {
                    XBeeConfigVars.networkDiscoveryTimeout = frame.getCommandData();
                }

                updateUI();
            }

        });

    }

    public void setButtonListener(View.OnClickListener onClickListener) {
        this.tempOnClickListener = onClickListener;
    }

    public void startReadXBeeParameters() {

        XBeeATCommandFrame shFrame = new XBeeATCommandFrame("SH");
        XBeeATCommandFrame slFrame = new XBeeATCommandFrame("SL");
        XBeeATCommandFrame firmwareFrame = new XBeeATCommandFrame("VR");
        XBeeATCommandFrame hardwareFrame = new XBeeATCommandFrame("HV");
        XBeeATCommandFrame channelFrame = new XBeeATCommandFrame("CH");
        XBeeATCommandFrame networkIdFrame = new XBeeATCommandFrame("ID");
        XBeeATCommandFrame discoveryTimeoutFrame = new XBeeATCommandFrame("N?");

        XBeeService.sendFrame(shFrame);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XBeeService.sendFrame(slFrame);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XBeeService.sendFrame(firmwareFrame);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XBeeService.sendFrame(hardwareFrame);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XBeeService.sendFrame(channelFrame);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XBeeService.sendFrame(networkIdFrame);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        XBeeService.sendFrame(discoveryTimeoutFrame);


    }

}
