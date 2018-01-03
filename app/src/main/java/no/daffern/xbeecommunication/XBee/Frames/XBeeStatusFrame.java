package no.daffern.xbeecommunication.XBee.Frames;

/**
 * Created by Daffern on 31.05.2016.
 *
 * Parses an XBee status frame. These are used to check if an XBee Transmit frame was successfully sent or not
 */
public class XBeeStatusFrame extends XBeeFrame {

    //Delivery status codes
    public static final byte SUCCESS = 0x00;
    public static final byte MAC_ACK_FAILURE = 0x01;
    public static final byte COLLISION_AVOIDANCE_FAILURE = 0x02;
    public static final byte NETWORK_ACK_FAILURE = 0x21;
    public static final byte ROUTE_NOT_FOUND = 0x25;
    public static final byte INTERNAL_RESOURCE_ERROR = 0x31;
    public static final byte INTERNAL_ERROR = 0x32;
    public static final byte PAYLOAD_TOO_LARGE = 0x74;


    byte[] destinationAddress16 = new byte[2];
    byte transmitTries;
    byte deliveryStatus;
    byte discoveryStatus;

    public XBeeStatusFrame(byte[] bytes) {
        length = (short) (bytes[1] << 8 | bytes[2]);
        frameType = bytes[3];
        frameId = bytes[4];
        destinationAddress16[0] = bytes[5];
        destinationAddress16[1] = bytes[6];
        transmitTries = bytes[7];
        deliveryStatus = bytes[8];
        discoveryStatus = bytes[9];
    }

    public byte getDiscoveryStatus() {
        return discoveryStatus;
    }

    public byte[] getDestinationAddress16() {
        return destinationAddress16;
    }

    public byte getTransmitTries() {
        return transmitTries;
    }

    public byte getDeliveryStatus() {
        return deliveryStatus;
    }

}
