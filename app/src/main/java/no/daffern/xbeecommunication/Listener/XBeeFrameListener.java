package no.daffern.xbeecommunication.Listener;

import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandResponseFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;

/**
 * Created by Daffern on 05.07.2016.
 */
public abstract class XBeeFrameListener {
    public void onNodeUpdate(){}
    public void onChatMessage(XBeeReceiveFrame frame){}
    public void onTransmitStatus(XBeeStatusFrame frame){}
    public void onSmsMessage(XBeeReceiveFrame frame){}
    public void onSmsStatusMessage(XBeeReceiveFrame frame){}
    public void onVoiceMessage(XBeeReceiveFrame xBeeReceiveFrame){}
    public void onVoiceStatusMessage(XBeeReceiveFrame xBeeReceiveFrame){}
    public void onATCommandResponse(XBeeATCommandResponseFrame xBeeATCommandResponseFrame){}

}
