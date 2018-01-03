package no.daffern.xbeecommunication.Adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedHashMap;

import no.daffern.xbeecommunication.Listener.NodeListListener;
import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.Utility;


public class NodeListAdapter extends ArrayAdapter<Node> {

    private final static String TAG = NodeListAdapter.class.getSimpleName();

    private LinkedHashMap<Integer, Node> nodes;
    private ListView nodeView;


    private NodeListListener nodeListListener;

    public NodeListAdapter(Activity activity, int resourceId, LinkedHashMap<Integer, Node> nodes) {
        super(activity, resourceId);
        this.nodes = nodes;
        nodeView = (ListView) activity.findViewById(resourceId);

    }

    public void setNodeListListener(NodeListListener nodeListListener) {
        this.nodeListListener = nodeListListener;
    }

    public int getCount() {
        return this.nodes.size();
    }

    public Node getItem(int index) {
        return nodes.values().toArray(new Node[nodes.size()])[index];
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.listitem_node, parent, false);
        }

        TextView nodeName = (TextView) row.findViewById(R.id.node_name);
        TextView address64 = (TextView) row.findViewById(R.id.node_address64);


        Button chatButton = (Button) row.findViewById(R.id.button_chat);
        Button smsButton = (Button) row.findViewById(R.id.button_sms);
        Button callButton = (Button) row.findViewById(R.id.button_voice);

        chatButton.setOnClickListener(onChatClickListener);
        smsButton.setOnClickListener(onSmsClickListener);
        callButton.setOnClickListener(onCallClickListener);


        Node node = getItem(position);

        nodeName.setText(node.nodeIdentifier);
        address64.setText(Utility.bytesToHex(node.address64));


        row.setTag(node);

        return row;
    }

    public View.OnClickListener onChatClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = nodeView.getPositionForView((View) v.getParent());
            Node node = getItem(position);
            nodeListListener.onChatClicked(node);
        }
    };
    public View.OnClickListener onSmsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = nodeView.getPositionForView((View) v.getParent());
            Node node = getItem(position);
            nodeListListener.onSmsClicked(node);
        }
    };

    public View.OnClickListener onCallClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = nodeView.getPositionForView((View) v.getParent());
            Node node = getItem(position);
            nodeListListener.onCallClicked(node);
        }
    };
}