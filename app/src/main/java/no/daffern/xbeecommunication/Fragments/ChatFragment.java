package no.daffern.xbeecommunication.Fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import no.daffern.xbeecommunication.Adapter.ChatAdapter;
import no.daffern.xbeecommunication.DataTypes;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.Model.ChatMessage;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.Utility;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeStatusFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeTransmitFrame;
import no.daffern.xbeecommunication.XBeeService;

/**
 * Created by Daffern on 27.05.2016.
 */
public class ChatFragment extends Fragment {

    Button sendButton;
    EditText writeText;

    ListView chatView;
    ChatAdapter chatAdapter;

    TextView nodeText;

    LinkedHashMap<Integer, ArrayList<ChatMessage>> messageMap;

    ArrayList<ChatMessage> unAcknowledgedFrames;

    Node currentNode;
    XBeeService xBeeService;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        initializeInterface();

    }

    public ChatFragment() {
        super();
        xBeeService = XBeeService.getInstance();
        messageMap = new LinkedHashMap<>();
        messageMap.put(xBeeService.broadcastNode.getKey(), new ArrayList<ChatMessage>());

        unAcknowledgedFrames = new ArrayList<>();

        xBeeService.addXBeeFrameListener(new XBeeFrameListener() {

            @Override
            public void onChatMessage(XBeeReceiveFrame frame) {
                int key = Node.getKey(frame.getAddress64());

                ArrayList<ChatMessage> messages;
                if (frame.isBroadcast()) {
                    messages = messageMap.get(xBeeService.broadcastNode.getKey());

                } else {
                    messages = messageMap.get(key);
                }

                if (messages == null) {
                    messages = new ArrayList<>();
                    messageMap.put(key, messages);
                }
                ChatMessage chatMessage = new ChatMessage(true, new String(frame.getRfData()), "", frame.getFrameId());

                messages.add(chatMessage);

                updateUI();
            }

            @Override
            public void onTransmitStatus(XBeeStatusFrame frame) {

                int frameId = frame.getFrameId();

                for (int i = unAcknowledgedFrames.size() - 1; i >= 0 ; i--) {
                    if (unAcknowledgedFrames.get(i).frameId == frameId) {

                        if (frame.getDeliveryStatus() == XBeeStatusFrame.SUCCESS) {
                            unAcknowledgedFrames.get(i).status = "Sent!";
                        } else {
                            unAcknowledgedFrames.get(i).status = "Failed with code: " + frame.getDeliveryStatus();
                        }
                        updateUI();
                        unAcknowledgedFrames.remove(i);
                        break;
                    }
                }

            }

        });
    }

    public void setCurrentNode(Node node) {
        this.currentNode = node;
        if (chatAdapter != null) {
            chatAdapter.setMessages(messageMap.get(currentNode.getKey()));
        }
    }
    public void updateUI(){
        if (getActivity() == null)
            return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (chatAdapter != null)
                    chatAdapter.notifyDataSetChanged();
            }
        });
    }


    public void sendMessage(String message) {
        byte[] bytes = message.getBytes();

        XBeeTransmitFrame transmitFrame = new XBeeTransmitFrame(DataTypes.APP_CHAT_MESSAGE);
        transmitFrame.setRfData(bytes);
        transmitFrame.setAddress64(currentNode.address64);
        transmitFrame.setDataType(DataTypes.APP_CHAT_MESSAGE);

        //check if send was successful
        if (xBeeService.sendFrame(transmitFrame)) {


            ChatMessage chatMessage = new ChatMessage(false, message, "Sending...", transmitFrame.getFrameId());
            unAcknowledgedFrames.add(chatMessage);

            ArrayList<ChatMessage> messages = messageMap.get(currentNode.getKey());
            if (messages == null) {
                messages = new ArrayList<>();
                messageMap.put(currentNode.getKey(), messages);
            }

            messages.add(chatMessage);
            updateUI();
        }

        writeText.getText().clear();

    }

    private void initializeInterface() {
        writeText = (EditText) getView().findViewById(R.id.editText);
        writeText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        InputFilter inputFilter = new InputFilter.LengthFilter(XBeeTransmitFrame.MAX_RF_DATA-1);
        writeText.setFilters(new InputFilter[]{inputFilter});
        writeText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    String message = writeText.getText().toString();
                    if (message.length() > 0) {
                        sendMessage(message);
                        return true;
                    }
                }
                return false;
            }
        });

        //need to fix this
        nodeText = (TextView) getView().findViewById(R.id.nodeText);
        if (currentNode.nodeIdentifier == null || currentNode.nodeIdentifier.length() == 0) {
            nodeText.setText("Chatting with: " + Utility.bytesToHex(currentNode.address64));
        } else {
            nodeText.setText("Chatting with: " + currentNode.nodeIdentifier);

        }

        chatView = (ListView) getView().findViewById(R.id.chatList);
        chatView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        chatView.setStackFromBottom(true);

        chatAdapter = new ChatAdapter(getActivity(), R.id.chatList);
        chatView.setAdapter(chatAdapter);


        ArrayList<ChatMessage> messages = messageMap.get(currentNode.getKey());

        chatAdapter.setMessages(messages);

    }


}
