package no.daffern.xbeecommunication.XBee;

/**
 * Created by Daffern on 11.06.2016.
 */
public class XBeeFrameType {
    public static final byte XBEE_TRANSMIT_REQUEST = 0x10;
    public static final byte XBEE_RECEIVE = (byte)0x90;
    public static final byte XBEE_TRANSMIT_STATUS = (byte)0x8B;
    public static final byte XBEE_AT_COMMAND = (byte)0x08;
    public static final byte XBEE_AT_COMMAND_RESPONSE = (byte)0x88;

    public static final byte APP_CHAT_MESSAGE = 0x6d; //m
    public static final byte APP_VOICE_MESSAGE = 0x76; //v
    public static final byte APP_VOICE_STATUS_MESSAGE = 0x75;
    public static final byte APP_SMS_MESSAGE = 0x73; //s
    public static final byte APP_SMS_STATUS_MESSAGE = 0x74; //t

}
