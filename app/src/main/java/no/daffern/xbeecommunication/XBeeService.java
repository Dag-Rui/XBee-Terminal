package no.daffern.xbeecommunication;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import no.daffern.xbeecommunication.Listener.MessageListener;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandResponseFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeTransmitFrame;
import no.daffern.xbeecommunication.XBee.XBeeATNetworkDiscover;
import no.daffern.xbeecommunication.XBee.XBeeFrameType;

/**
 * Created by Daffern on 22.06.2016.
 *
 * Singleton class for sending and receiving XBeeFrames from the XBee module through the USB connection.
 * Keeps track of all nodes on the network by reading the addresses of received frames
 */
public class XBeeService {

    private final static String TAG = XBeeService.class.getSimpleName();

    private static XBeeService singleton;

    public static XBeeService getInstance() {
        if (singleton == null)
            singleton = new XBeeService();
        return singleton;
    }

    private ArrayList<XBeeFrameListener> xBeeFrameListeners;

    private LinkedHashMap<Integer, Node> nodeMap;

    public Node broadcastNode;

    private MessageListener messageListener;

    private XBeeService() {
        xBeeFrameListeners = new ArrayList<>();

        nodeMap = new LinkedHashMap<>();


        //Broadcast entry, uses the broadcast address as (a unique) key even though a message is never received from the broadcast address
        broadcastNode = new Node(XBeeTransmitFrame.address16Broadcast, XBeeTransmitFrame.address64Broadcast, "Broadcast");
        nodeMap.put(broadcastNode.getKey(), broadcastNode);

    }

    //outgoing data is written to the message listener (see MainAcitivty.onCreate())
    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    //outgoing data is sent to the messageListener
    public boolean sendFrame(XBeeFrame xBeeFrame) {
        return messageListener.onMessageReceived(xBeeFrame.generateFrame());
    }

    /*
    * Only called from the MainActivity (See MainActivity.bufferFrame())
    * Handles frames by keeping track of nodes and distributing frames to each fragment through the XBeeFrameListeners
     */
    public void receiveFrame(XBeeFrame frame) {

        switch (frame.getFrameType()) {
            case XBeeFrameType.XBEE_RECEIVE: {

                XBeeReceiveFrame receiveFrame = (XBeeReceiveFrame) frame;

                //Add the node (if not already added) to the nodeList only if the frame was acknowledged (to prevent errors in stored addresses)
                if (receiveFrame.isAck()) {
                    int key = Node.getKey(receiveFrame.getAddress64());
                    Node node = nodeMap.get(key);
                    if (node == null) {
                        node = new Node(receiveFrame.getAddress64());
                        nodeMap.put(node.getKey(), node);

                        for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                            xBeeFrameListener.onNodeUpdate();
                        }
                    }
                }

                switch (receiveFrame.getDataType()) {
                    case DataTypes.APP_CHAT_MESSAGE:
                        for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                            xBeeFrameListener.onChatMessage(receiveFrame);
                        }
                        break;
                    case DataTypes.APP_SMS_MESSAGE:
                        for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                            xBeeFrameListener.onSmsMessage(receiveFrame);
                        }
                        break;
                    case DataTypes.APP_SMS_STATUS_MESSAGE:
                        for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                            xBeeFrameListener.onSmsStatusMessage(receiveFrame);
                        }
                        break;
                    case DataTypes.APP_VOICE_MESSAGE:
                        for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                            xBeeFrameListener.onVoiceMessage(receiveFrame);
                        }
                        break;
                    case DataTypes.APP_VOICE_STATUS_MESSAGE:
                        for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                            xBeeFrameListener.onVoiceStatusMessage(receiveFrame);
                        }
                        break;
                }

                break;
            }

            case XBeeFrameType.XBEE_TRANSMIT_STATUS:

                Log.e(TAG, "Received a Transmit Status Frame");

                XBeeStatusFrame status = (XBeeStatusFrame) frame;

                for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                    xBeeFrameListener.onTransmitStatus(status);
                }

                break;

            case XBeeFrameType.XBEE_AT_COMMAND_RESPONSE:

                Log.e(TAG, "Received an AT Command Response Frame");

                XBeeATCommandResponseFrame response = (XBeeATCommandResponseFrame) frame;
                if (Arrays.equals(response.getCommand(), XBeeATNetworkDiscover.ND_COMMAND)) {
                    XBeeATNetworkDiscover networkDiscover = new XBeeATNetworkDiscover(response.getCommandData());
                    ArrayList<Node> nodes = networkDiscover.getNodes();

                    for (Node node : nodes) {
                        nodeMap.put(node.getKey(), node);
                    }

                    for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                        xBeeFrameListener.onNodeUpdate();
                    }
                } else {

                    for (XBeeFrameListener xBeeFrameListener : xBeeFrameListeners) {
                        xBeeFrameListener.onATCommandResponse(response);
                    }
                }
                break;
            default:
                Log.e(TAG, "Received an unhandled XBee Frame, with frame type: " + frame.getFrameType());
                break;

        }
    }

    public LinkedHashMap<Integer, Node> getNodeMap() {
        return nodeMap;
    }

    public void addXBeeFrameListener(XBeeFrameListener xBeeFrameListener) {
        xBeeFrameListeners.add(xBeeFrameListener);

    }

    public void removeXBeeFrameListener(XBeeFrameListener xBeeFrameListener) {
        xBeeFrameListeners.remove(xBeeFrameListener);
    }
}
