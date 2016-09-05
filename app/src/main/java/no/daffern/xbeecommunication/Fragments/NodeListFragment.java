package no.daffern.xbeecommunication.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import no.daffern.xbeecommunication.Adapter.NodeListAdapter;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.Listener.NodeListListener;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandFrame;
import no.daffern.xbeecommunication.XBeeService;
import no.daffern.xbeecommunication.XBee.XBeeATNetworkDiscover;
import no.daffern.xbeecommunication.XBeeConfigVars;

/**
 * Created by Daffern on 12.06.2016.
 */
public class NodeListFragment extends Fragment {

    private static final String TAG = NodeListFragment.class.getSimpleName();

    NodeListAdapter nodeListAdapter;
    ListView nodeView;
    Button refreshButton;

    NodeListListener nodeListListener;

    XBeeService xBeeService;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_nodes, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        nodeView = (ListView) getActivity().findViewById(R.id.node_list);
        nodeView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        nodeView.setStackFromBottom(false);


        refreshButton = (Button) getActivity().findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XBeeATCommandFrame frame = new XBeeATCommandFrame(XBeeATNetworkDiscover.ND_COMMAND);


                xBeeService.sendFrame(frame);

                refreshButton.setEnabled(false);

                //button enabled after the timeoutdelay
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshButton.setEnabled(true);

                    }

                }, XBeeConfigVars.getNetworkDiscoveryTimeout());
            }
        });

        xBeeService = XBeeService.getInstance();
        xBeeService.addXBeeFrameListener(new XBeeFrameListener() {

            @Override
            public void onNodeUpdate() {
                nodeListAdapter.notifyDataSetChanged();
            }


        });

        nodeListAdapter = new NodeListAdapter(getActivity(), R.id.node_list, xBeeService.getNodeMap());
        nodeView.setAdapter(nodeListAdapter);


        nodeListAdapter.setNodeListListener(nodeListListener);
    }


    public void setNodeListListener(NodeListListener nodeListListener) {
        this.nodeListListener = nodeListListener;
        if (nodeListAdapter != null) {
            nodeListAdapter.setNodeListListener(nodeListListener);
        }
    }


}
