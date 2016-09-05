package no.daffern.xbeecommunication.Listener;

/**
 * Created by Daffern on 16.08.2016.
 */
public interface UsbListener {

    void onDataReceived(byte[] bytes);
    void onConnectionStatusChanged(boolean connected);

}
