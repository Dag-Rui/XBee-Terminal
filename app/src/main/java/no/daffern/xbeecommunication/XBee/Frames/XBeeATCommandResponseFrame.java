package no.daffern.xbeecommunication.XBee.Frames;

/**
 * Created by Daffern on 12.06.2016.
 */
public class XBeeATCommandResponseFrame extends XBeeFrame {

    byte status;
    byte[] command = new byte[2];
    byte[] commandData;
    byte checksum;


    public XBeeATCommandResponseFrame(byte[] bytes){
        length = (short)(bytes[1] << 8 | bytes[2]);
        frameType = bytes[3];
        frameId = bytes[4];
        command[0] = bytes[5];
        command[1] = bytes[6];
        status = bytes[7];

        int commandDataLength = length - 5;
        commandData = new byte[commandDataLength];
        for (int i = 0 ; i < commandDataLength ; i++){
            commandData[i] = bytes[i+8];
        }
        checksum = bytes[8+commandDataLength];
    }

    public byte getStatus() {
        return status;
    }

    public byte[] getCommand() {
        return command;
    }

    public byte[] getCommandData() {
        return commandData;
    }
}
