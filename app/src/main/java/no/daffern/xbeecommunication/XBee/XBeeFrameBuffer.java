package no.daffern.xbeecommunication.XBee;

import android.util.Log;

import java.util.ArrayList;

import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandResponseFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;

/**
 * Created by Daffern on 04.06.2016.
 */
public class XBeeFrameBuffer {

    private static final String TAG = XBeeFrameBuffer.class.getSimpleName();

    //size in bytes of start delimiter + length(2 bytes) + checksum
    public final static int FRAME_BASE_SIZE = 4;

    public final static int bufferSize =  10000;

    byte[] buffer = new byte[bufferSize]; //max framesize is 97
    int byteCount = 0;
    int position = 0;



    /*
    Insert data into the buffer, returns any frames parsed
    If the buffer decides that a frame is complete, parse and return it.
     */
    public ArrayList<XBeeFrame> putAndCheck(byte[] bytes) throws Exception {
        if (bytes == null)
            return null;

        ArrayList<XBeeFrame> xBeeFrames = new ArrayList<>();

        System.arraycopy(bytes, 0, buffer, byteCount, bytes.length);
        byteCount = byteCount + bytes.length;

        //if the frame is too short, return
        if (byteCount < FRAME_BASE_SIZE)
            return xBeeFrames;

        while (position < byteCount) {

            //check if the start delimiter is ok
            if (buffer[position] != XBeeFrame.START_DELIMITER) {
                Log.e(TAG, "Start delimiter was wrong, searching...");

                position = 0;

                //search for the start delimiter
                for (int i = position ; i < byteCount ; i++){
                    if (buffer[i] == XBeeFrame.START_DELIMITER){

                        Log.e(TAG, "Found start delimiter, skipping " + i + " bytes");


                        System.arraycopy(buffer, i, buffer, 0, bufferSize - i);
                        byteCount = byteCount - i;
                        position = 0;

                        break;
                    }
                }
                if (position == 0){

                    Log.e(TAG, "could not find start delimiter, resetting");
                    return xBeeFrames;
                }


                //throw new Exception("The start delimiter is wrong: " + buffer[0] + ". Resetting buffer");
            }

            //get the frame length from the buffer
            int length = buffer[position + 1] << 8 | buffer[position + 2];

            //
            int frameEnd = position + length + FRAME_BASE_SIZE;

            //If there is exactly one frame in the buffer, parse it and return
            if (frameEnd == byteCount) {

                XBeeFrame frame = createFrame(buffer, position, length + FRAME_BASE_SIZE);
                xBeeFrames.add(frame);

                //Reset the buffer
                position = 0;
                byteCount = 0;
                break;
            }
            //If there are more than one frame in the buffer
            else if (frameEnd < byteCount){
                XBeeFrame frame = createFrame(buffer, position, length + FRAME_BASE_SIZE);
                xBeeFrames.add(frame);

                position += length + FRAME_BASE_SIZE;
            }
            //if there are less than on frame in the buffer, move the bytes to the start of the buffer
            else if (frameEnd > byteCount) {

                System.arraycopy(buffer, position, buffer, 0, byteCount - position);
                byteCount = byteCount - position;
                position = 0;
                break;
            }
        }

        return xBeeFrames;
    }

    private XBeeFrame createFrame(byte[] bytes, int offset, int length) {
        byte[] buf = new byte[length];
        System.arraycopy(bytes, offset, buf, 0, length);

        byte frameType = buf[3];
        switch (frameType) {
            case XBeeFrameType.XBEE_RECEIVE:
                return new XBeeReceiveFrame(buf);

            case XBeeFrameType.XBEE_TRANSMIT_STATUS:
                return new XBeeStatusFrame(buf);

            case XBeeFrameType.XBEE_AT_COMMAND_RESPONSE:
                return new XBeeATCommandResponseFrame(buf);

            default:
                return null;
        }
    }

}
