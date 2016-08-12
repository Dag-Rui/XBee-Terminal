package no.daffern.xbeecommunication.Model;

/**
 * Created by Daffern on 24.07.2016.
 */
public class SmsMessage {

    //SMS status codes
    public final static byte SMS_SENT_TO_REMOTE = 0;
    public final static byte SMS_RECEIVED_BY_REMOTE = 1;
    public final static byte REMOTE_MOBILE_SENT_SMS = 2;
    public final static byte TARGET_MOBILE_RECEIVED_SMS = 3;

    public final static byte REMOTE_MOBILE_ERROR_GENERIC_FAILURE = 4;
    public final static byte REMOTE_MOBILE_RESULT_ERROR_NO_SERVICE = 5;
    public final static byte REMOTE_MOBILE_RESULT_ERROR_NULL_PDU = 6;
    public final static byte REMOTE_MOBILE_RESULT_ERROR_RADIO_OFF = 7;

    static byte smsIdCounter=0;

    String phoneNumber;
    String message;
    byte smsId;
    boolean sent;//True if the sms was initiated from this XBee node. False if the sms was sent from this Android device
    byte status;


    public SmsMessage(String phoneNumber, String message, boolean sent, byte status, byte smsId) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.sent = sent;
        this.status = status;
        this.smsId = smsId;
    }
    public SmsMessage(String phoneNumber, String message, boolean sent, byte status) {
        this(new String(phoneNumber), new String(message), sent, status, smsIdCounter++);
    }
    public SmsMessage(byte[] phoneNumber, byte[] message, boolean sent, byte status) {
        this(new String(phoneNumber), new String(message), sent, status);
    }


    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public byte getSmsId() {
        return smsId;
    }

    public boolean isSent() {
        return sent;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status){
        this.status = status;
    }
}
