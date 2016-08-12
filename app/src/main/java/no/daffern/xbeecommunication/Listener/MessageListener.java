package no.daffern.xbeecommunication.Listener;

/**
 * Created by Daffern on 10.06.2016.
 */
public interface MessageListener {
    boolean onMessageReceived(byte[] bytes);
}
