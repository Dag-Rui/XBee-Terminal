package no.daffern.xbeecommunication.XBee.Frames;

import no.daffern.xbeecommunication.XBee.XBeeFrameType;

/**
 * Created by Daffern on 11.06.2016.
 *
 * Generates an XBee AT command frame through the GenerateFrame() method
 *
 * See https://www.digi.com/resources/documentation/digidocs/pdfs/90000991.pdf for a complete list of all AT commands
 */
public class XBeeATCommandFrame extends XBeeFrame {

    byte[] command;
    byte[] parameter;

    private static final int MIN_PAYLOAD = 4;

    public XBeeATCommandFrame(byte[] command) {
        frameType = XBeeFrameType.XBEE_AT_COMMAND;
        frameId = getNextFrameId();
        if (command.length == 2)
            this.command = command;
    }

    public XBeeATCommandFrame(String command) {
        this(command.getBytes());
    }


    public void setParameter(byte[] parameter) {
        this.parameter = parameter;
    }

    @Override
    public byte[] generateFrame() {
        int bytes = FRAME_BASE_SIZE + MIN_PAYLOAD;
        int payloadSize = MIN_PAYLOAD;
        if (parameter != null) {
            bytes += parameter.length;
            payloadSize += parameter.length;
        }

        byte[] frame = new byte[bytes];
        frame[0] = START_DELIMITER;
        frame[1] = (byte) ((payloadSize >> 8) & 0xff);
        frame[2] = (byte) (payloadSize & 0xFF);
        frame[3] = frameType;
        frame[4] = frameId;
        frame[5] = command[0];
        frame[6] = command[1];

        if (parameter != null) {
            for (int i = 0; i < parameter.length; i++) {
                frame[7 + i] = parameter[i];
            }
        }

        checksum.add(frameType);
        checksum.add(frameId);
        checksum.add(command);
        if (parameter != null) {
            checksum.add(parameter);
            frame[7 + parameter.length] = checksum.generate();
        } else {
            frame[7] = checksum.generate();
        }

        return frame;
    }
}
