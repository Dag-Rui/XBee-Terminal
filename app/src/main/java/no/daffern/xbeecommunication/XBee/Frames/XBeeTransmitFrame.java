package no.daffern.xbeecommunication.XBee.Frames;


import no.daffern.xbeecommunication.Utility;
import no.daffern.xbeecommunication.XBee.XBeeFrameType;

/**
 * Created by Daffern on 31.05.2016.
 */
public class XBeeTransmitFrame extends XBeeFrame {

    public final static byte[] address64Broadcast = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF};
    public final static byte[] address16Broadcast = new byte[]{(byte) 0xFF, (byte) 0xFE};

    byte[] address64;
    byte[] reserved = new byte[2];
    byte broadcastRadius = 0x0;//0 defaults to configuration on xbee
    byte options = 0x0;//Bit 1 - Disable ACK, Bit 2 - disable network address discovery


    byte[] rfData;

    byte dataType;

    public static int MAX_RF_DATA = 73; //Default max rfData size for the XBee-PRO DigiMesh 2.4
    private static int MIN_PAYLOAD = 14; //size without any rfData

    public XBeeTransmitFrame(byte dataType) {
        frameType = XBeeFrameType.XBEE_TRANSMIT_REQUEST;
        frameId = getNextFrameId();
        this.dataType = dataType;
    }


    public void setRfData(byte[] bytes){
        this.rfData = bytes;
    }

    @Override
    public byte[] generateFrame(){
        int bytes = MIN_PAYLOAD + rfData.length + FRAME_BASE_SIZE + 1;
        int payloadSize = MIN_PAYLOAD + rfData.length + 1;
        byte[] frame = new byte[bytes];

        frame[0] = START_DELIMITER;
        frame[1] = (byte)((payloadSize >> 8) & 0xff);
        frame[2] = (byte)(payloadSize & 0xFF);
        frame[3] = frameType;
        frame[4] = frameId;
        frame[5] = address64[0];
        frame[6] = address64[1];
        frame[7] = address64[2];
        frame[8] = address64[3];
        frame[9] = address64[4];
        frame[10] = address64[5];
        frame[11] = address64[6];
        frame[12] = address64[7];
        frame[13] = reserved[0];
        frame[14] = reserved[1];
        frame[15] = broadcastRadius;
        frame[16] = options;

        frame[17] = dataType;

        for (int i = 0; i < rfData.length ; i++){
            frame[18+i] = rfData[i];
        }

        checksum.add(frameType);
        checksum.add(frameId);
        checksum.add(address64);
        checksum.add(reserved);
        checksum.add(broadcastRadius);
        checksum.add(options);
        checksum.add(dataType);
        checksum.add(rfData);
        frame[18 + rfData.length] = checksum.generate();

        return frame;
    }

    public void setAck(boolean ack){
        options = (byte)Utility.setBit(options, 0, !ack);
    }

    public void setAddress64(byte[] address64) {
        this.address64  = address64;
    }

    public void setDataType(byte dataType){
        this.dataType = dataType;
    }
}
