package no.daffern.xbeecommunication.Adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import no.daffern.xbeecommunication.Model.ChatMessage;
import no.daffern.xbeecommunication.R;


public class ChatAdapter extends ArrayAdapter<ChatMessage> {

    private List<ChatMessage> messages;


    int maxCount=100;

    @Override
    public void add(ChatMessage object) {
        messages.add(object);

        if (messages.size() >= maxCount){
            messages.remove(0);
        }

        super.add(object);
    }

    public ChatAdapter( Activity activity, int resourceId) {
        super(activity, resourceId);

    }
    public void setMessages(ArrayList<ChatMessage> messages){
        this.messages = messages;
        notifyDataSetChanged();
    }

    public int getCount() {
        if (messages == null)
            return 0;
        return this.messages.size();
    }

    public ChatMessage getItem(int index) {
        return this.messages.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.listitem_chat, parent, false);

        }

        LinearLayout container = (LinearLayout) row.findViewById(R.id.gravityContainer);
        LinearLayout backgroundContainer = (LinearLayout) container.findViewById(R.id.backgroundContainer);

        TextView status = (TextView) row.findViewById(R.id.status);
        TextView comment = (TextView) row.findViewById(R.id.message);
        TextView time = (TextView)row.findViewById(R.id.time);

        ChatMessage message = getItem(position);
        comment.setText(message.comment);
        status.setText(message.status);

        if (message.time == 0){
            time.setText("");
        }else{
            String sTime = new SimpleDateFormat("HH.mm.ss").format(message.time);
            time.setText(sTime);
        }

        if (message.status.equals(""))
            status.setVisibility(View.GONE);
        else{
            status.setVisibility(View.VISIBLE);
        }

        backgroundContainer.setBackgroundResource(message.left ? R.drawable.bubble_yellow : R.drawable.bubble_green);
        container.setGravity(message.left ? Gravity.LEFT : Gravity.RIGHT);

        return row;
    }

    public void setMaxCount(int maxCount){
        this.maxCount=maxCount;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

}