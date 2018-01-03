package no.daffern.xbeecommunication;


import java.io.ByteArrayInputStream;


/**
 * Created by Daffern on 12.06.2016.
 */
public class Utility {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return new String();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte b) {
        byte[] bytes = {b};
        return bytesToHex(bytes);
    }

    //Reads from a byte array into target byte array until 0x00 is reached
    public static String readString(byte[] buffer, int offset) {

        StringBuilder sb = new StringBuilder();

        int i = offset;
        while (buffer[i] != 0x00 && i < buffer.length) {
            sb.append((char) buffer[i]);
            i++;
        }

        return sb.toString();
    }

    //Reads from a byte array into target byte array until 0x00 is reached
    public static String readString(ByteArrayInputStream stream) {

        StringBuilder sb = new StringBuilder();

        int value = 0xFF;
        while (value != 0x00) {
            value = stream.read();
            sb.append((char) value);
        }

        return sb.toString();
    }

    public static byte[] shortsToBytes(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    public static int setBit(int byt, int bit, boolean set) {
        if (set)
            byt = byt | (1 << bit);
        else
            byt = byt & ~(1 << bit);
        return byt;
    }
}
