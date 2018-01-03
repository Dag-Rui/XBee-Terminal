package no.daffern.xbeecommunication.XBee.Frames;

/**
 * Created by Daffern on 31.05.2016.
 *
 * Parses an XBee receive frame, use getRfData() to retrieve data.
 * This frame is received when another node send a message using the XBeeTransmitFrame
 */
public class XBeeReceiveFrame extends XBeeFrame {

    private final static String TAG = XBeeReceiveFrame.class.getSimpleName();


    private byte[] address64 = new byte[8];
    private byte[] address16 = new byte[2];
    private byte options = 0x0;// 01 packet was ACK, 02 packet was broadcast, 20 packet encrypted with APS, 40 packet sent from end device
    private byte[] rfData;
    private byte dataType;
    private byte checksum;

    public XBeeReceiveFrame(byte[] bytes) {
        length = (short) (bytes[1] << 8 | bytes[2]);

        frameType = bytes[3];

        for (int i = 0; i < 8; i++) {
            address64[i] = bytes[4 + i];
        }

        address16[0] = bytes[12];
        address16[1] = bytes[13];

        options = bytes[14];

        dataType = bytes[15];

        int rfDataLength = length - 13;
        rfData = new byte[rfDataLength];
        for (int i = 0; i < rfDataLength; i++) {
            rfData[i] = bytes[16 + i];
        }
        checksum = bytes[16 + rfDataLength];
    }

    public byte[] getAddress64() {
        return address64;
    }

    public byte[] getAddress16() {
        return address16;
    }

    public boolean isBroadcast() {
        if ((options & 0x02) == 0x02) {
            return true;
        }
        return false;
    }

    public byte[] getRfData() {
        return rfData;
    }

    public byte getChecksum() {
        return checksum;
    }

    public byte getDataType() {
        return dataType;
    }

    public boolean isAck() {
        if ((options & 1) != 0) {
            return true;
        } else return false;

    }
}
