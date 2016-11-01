package no.daffern.xbeecommunication.Fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import no.daffern.xbeecommunication.R;

/**
 * Created by Daffern on 28.06.2016.
 */
public class TerminalFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_chat, container, false);
    }


}
