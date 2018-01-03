package no.daffern.xbeecommunication.Model;

/**
 * Created by Daffern on 12.06.2016.
 * Contains the data of a received or sent chat message
 */

public class ChatMessage {
    public boolean left;
    public byte frameId;
    public String comment;
    public String status;
    public long time;

    public ChatMessage(boolean left, String comment, String status, byte frameId, long time) {
        this.left = left;
        this.comment = comment;
        this.status = status;
        this.frameId = frameId;
        this.time = time;
    }

}