package no.daffern.xbeecommunication.XBee.Frames;

import no.daffern.xbeecommunication.XBee.XBeeChecksum;

/**
 * Created by Daffern on 04.06.2016.
 */
public abstract class XBeeFrame {

    public static final byte START_DELIMITER = 0x7E;

    public static final byte FRAME_BASE_SIZE = 0x04; // start delimiter + length + checksum
    public static final byte MAX_DATA_SIZE = 73; //0x49

    private static byte frameIdCounter = 0x01;

    protected short length;
    protected byte frameType;
    protected byte frameId;

    protected XBeeChecksum checksum = new XBeeChecksum();


    public byte getFrameType(){
        return frameType;
    }

    protected byte getNextFrameId(){
        if (frameIdCounter == 0xFF) {
            frameIdCounter = 0x01;
        }else{
            frameIdCounter++;
        }
        return frameIdCounter;
    }

    public byte getFrameId() {
        return frameId;
    }

    //Method overriden by subclasses
    public byte[] generateFrame(){
        return null;
    }
}
