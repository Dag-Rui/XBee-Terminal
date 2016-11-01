package no.daffern.xbeecommunication.Model;

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