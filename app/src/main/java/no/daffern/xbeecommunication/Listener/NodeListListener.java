package no.daffern.xbeecommunication.Listener;

import no.daffern.xbeecommunication.Model.Node;

/**
 * Created by Daffern on 15.06.2016.
 */
public interface NodeListListener {
    void onChatClicked(Node node);
    void onSmsClicked(Node node);
    void onCallClicked(Node node);
}
