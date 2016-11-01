package no.daffern.xbeecommunication.Fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import no.daffern.xbeecommunication.Audio.RecordStreamHelper;
import no.daffern.xbeecommunication.Listener.XBeeFrameListener;
import no.daffern.xbeecommunication.MainActivity;
import no.daffern.xbeecommunication.R;
import no.daffern.xbeecommunication.Utility;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeATCommandResponseFrame;
import no.daffern.xbeecommunication.XBee.Frames.XBeeReceiveFrame;
import no.daffern.xbeecommunication.XBeeService;

/**
 * Created by Daffern on 30.09.2016.
 */

public class PerformanceFragment extends Fragment {

    private static final String TAG = PerformanceFragment.class.getSimpleName();

    int GPS_PERMISSION = 1;

    boolean updating = false;

    TextView RSSI;  //The RSSI of the XBee (command DB)
    TextView TR;
    TextView UA;
    TextView EA;
    TextView GD;
    TextView BC;


    View timeContainer;
    TextView gpsTimeText;
    TextView peakText;
    TextView peakTimeText;

    boolean synced = false;
    static long deviceGpsTimeDifference = 0;
    long gpsTime = 0;

    Thread timeThread;

    XBeeService xBeeService;
    XBeeFrameListener xBeeFrameListener;

    UpdateParametersRunnable updateParametersRunnable;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_performance, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        xBeeService = XBeeService.getInstance();

        RSSI = (TextView) view.findViewById(R.id.text_rssi);
        TR = (TextView) view.findViewById(R.id.text_tr);
        UA = (TextView) view.findViewById(R.id.text_ua);
        EA = (TextView) view.findViewById(R.id.text_ea);
        GD = (TextView) view.findViewById(R.id.text_gd);
        BC = (TextView) view.findViewById(R.id.text_bc);

        timeContainer = (View) view.findViewById(R.id.timeContainer);
        gpsTimeText = (TextView) view.findViewById(R.id.text_gps_time);
        peakText = (TextView) view.findViewById(R.id.text_audio_peak);
        peakTimeText = (TextView) view.findViewById(R.id.text_audio_peak_time);

        timeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordStreamHelper.resetPeakAmplitude();
            }
        });

        initGpsListener(getContext());

        timeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    gpsTime = System.currentTimeMillis() + deviceGpsTimeDifference;


                    final String sGpsTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS").format(gpsTime);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gpsTimeText.setText(sGpsTime);

                        }
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });



        RecordStreamHelper.setAudioAmplitudePeakListener(new RecordStreamHelper.AudioAmplitudePeakListener() {
            @Override
            public void onAudioAmplitudePeak(final double amplitude, long time) {

                final String sPeakTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS").format(time + deviceGpsTimeDifference);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        peakText.setText((int)amplitude + "");
                        peakTimeText.setText(sPeakTime);
                    }
                });


            }
        });
    }
    private class UpdateParametersRunnable implements Runnable  {
        int sleepTime = 2000;
        boolean running = false;
        @Override
        public void run (){
            running = true;
            while (running) {
                try {
                    sendATCommand("DB");
                    Thread.sleep(sleepTime);
                    sendATCommand("TR");
                    Thread.sleep(sleepTime);
                    sendATCommand("UA");
                    Thread.sleep(sleepTime);
                    sendATCommand("EA");
                    Thread.sleep(sleepTime);
                    sendATCommand("GD");
                    Thread.sleep(sleepTime);
                    sendATCommand("BC");
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public void cancel(){
            running = false;
        }
    }


    @Override
    public void onResume(){


        updateParametersRunnable = new UpdateParametersRunnable();
        Thread thread = new Thread(updateParametersRunnable);
        thread.start();

        xBeeFrameListener = new XBeeFrameListener() {
            @Override
            public void onATCommandResponse(XBeeATCommandResponseFrame xBeeATCommandResponseFrame) {

                byte[] command = xBeeATCommandResponseFrame.getCommand();
                String response = Utility.bytesToHex(xBeeATCommandResponseFrame.getCommandData());



                if (Arrays.equals(command, "DB".getBytes())) {
                    updateUI(RSSI, "RSSI: " + response);
                } else if (Arrays.equals(command, "TR".getBytes())) {
                    updateUI(TR, "Transmission fail: " + response);
                } else if (Arrays.equals(command, "UA".getBytes())) {
                    updateUI(UA, "Uni attempts: " + response);
                } else if (Arrays.equals(command, "EA".getBytes())) {
                    updateUI(EA, "ACK timeouts: " + response);
                } else if (Arrays.equals(command, "GD".getBytes())) {
                    updateUI(GD, "Good packets received: " + response);
                } else if (Arrays.equals(command, "BC".getBytes())) {
                    updateUI(BC, "Bytes transmitted: " + response);
                }

            }


        };
        xBeeService.addXBeeFrameListener(xBeeFrameListener);
        super.onResume();
    }
    @Override
    public void onPause(){
        xBeeService.removeXBeeFrameListener(xBeeFrameListener);

        updateParametersRunnable.cancel();

        super.onPause();
    }

    private void updateUI(final TextView textView, final String string){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(string);
            }
        });
    }

    private void sendATCommand(String command){
        XBeeATCommandFrame xBeeATCommandFrame = new XBeeATCommandFrame(command);
        xBeeService.sendFrame(xBeeATCommandFrame);
    }


    private void syncTime(long gpsTime) {
        long deviceTimeAtSync = System.currentTimeMillis();
        deviceGpsTimeDifference = gpsTime - deviceTimeAtSync;

        if (!synced) {
            timeThread.start();
            synced = true;
        }
    }

    private void initGpsListener(Context context) {

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {
                //dd.MM.yyyy HH:mm:ss:SSSSSSS
                //dd/MM/yyyy HH:mm:ss.SSS
                //String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSS").format(location.getTime());


                syncTime(location.getTime());

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION);

            MainActivity.makeToast("GPS was disabled - restart app");

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        // Note: To Stop listening use: locationManager.removeUpdates(locationListener)
    }
}
