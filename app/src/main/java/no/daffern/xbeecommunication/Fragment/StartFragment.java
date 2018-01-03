package no.daffern.xbeecommunication.Fragment;


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
import no.daffern.xbeecommunication.XBeeConfig;
import no.daffern.xbeecommunication.XBeeService;

/**
 * Created by Daffern on 25.06.2016.
 *
 * Start screen of the app. Tests the USB connection by reading a few parameters (for example the serial number) from a connected XBee module
 */
public class StartFragment extends Fragment {

    View.OnClickListener tempOnClickListener;

    TextView hardware;
    TextView firmware;
    TextView address;
    TextView operatingChannel;
    TextView networkId;

    XBeeService xBeeService;

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

    public void updateUI() {

        //Run ui updates on ui thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                address.setText("Serial number: " + Utility.bytesToHex(XBeeConfig.serialNumberHigh) + " " + Utility.bytesToHex(XBeeConfig.serialNumberLow));
                firmware.setText("Firmware version: " + Utility.bytesToHex(XBeeConfig.firmwareVersion));
                hardware.setText("Hardware version: " + Utility.bytesToHex(XBeeConfig.hardwareVersion));
                operatingChannel.setText("Operating channel: " + Utility.bytesToHex(XBeeConfig.operatingChannel));
                networkId.setText("Network ID: " + Utility.bytesToHex(XBeeConfig.networkId));
            }
        });
    }

    public StartFragment() {

        xBeeService = XBeeService.getInstance();

        xBeeService.addXBeeFrameListener(new XBeeFrameListener() {
            @Override
            public void onATCommandResponse(XBeeATCommandResponseFrame frame) {

                if (Arrays.equals(frame.getCommand(), "SH".getBytes())) {
                    XBeeConfig.serialNumberHigh = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "SL".getBytes())) {
                    XBeeConfig.serialNumberLow = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "VR".getBytes())) {
                    XBeeConfig.firmwareVersion = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "HV".getBytes())) {
                    XBeeConfig.hardwareVersion = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "CH".getBytes())) {
                    XBeeConfig.operatingChannel = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "ID".getBytes())) {
                    XBeeConfig.networkId = frame.getCommandData();
                } else if (Arrays.equals(frame.getCommand(), "N?".getBytes())) {
                    XBeeConfig.networkDiscoveryTimeout = frame.getCommandData();
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

        //The XBee may process some commands slowly, so wait a little between each
        try {
            xBeeService.sendFrame(shFrame);
            Thread.sleep(100);
            xBeeService.sendFrame(slFrame);
            Thread.sleep(100);
            xBeeService.sendFrame(firmwareFrame);
            Thread.sleep(100);
            xBeeService.sendFrame(hardwareFrame);
            Thread.sleep(100);
            xBeeService.sendFrame(channelFrame);
            Thread.sleep(100);
            xBeeService.sendFrame(networkIdFrame);
            Thread.sleep(100);
            xBeeService.sendFrame(discoveryTimeoutFrame);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
