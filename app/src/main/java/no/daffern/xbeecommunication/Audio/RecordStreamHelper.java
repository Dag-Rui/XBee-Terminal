package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;

import com.purplefrog.speexjni.FrequencyBand;
import com.purplefrog.speexjni.SpeexEncoder;


import static no.daffern.xbeecommunication.XBee.XBeeFrameBuffer.bufferSize;

/**
 * Created by Daffern on 20.09.2016.
 */

public class RecordStreamHelper {

    private final static String TAG = RecordStreamHelper.class.getSimpleName();

    int sampleRate = 8000;
    int channel = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    int bufferSize;
    short[] buffer;

    AudioRecord recorder;
    SpeexEncoder speexEncoder;

    RecordThread recordThread;
    boolean recording = false;

    SpeexFrameListener speexFrameListener;

    public void setSpeexFrameListener(SpeexFrameListener speexFrameListener){
        this.speexFrameListener = speexFrameListener;
    }


    public void start(){

        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat);
        buffer = new short[160];

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, audioFormat, bufferSize);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e(TAG,"Could not initialize AudioRecord");
        }

        //
        AcousticEchoCanceler.create(recorder.getAudioSessionId());


        speexEncoder = new SpeexEncoder(FrequencyBand.NARROW_BAND,1);


        recordThread = new RecordThread();
        Thread t = new Thread(recordThread);
        t.start();
    }
    public void stop(){
        recordThread.cancel();
    }

    private class RecordThread implements Runnable {

        @Override
        public void run() {

            recorder.startRecording();
            recording = true;

            while(recording){

                recorder.read(buffer,0, buffer.length);

                byte[] encoded = speexEncoder.encode(buffer);

                speexFrameListener.onFrameRecorded(encoded);
            }
        }
        public void cancel(){
            recording = false;
            recorder.stop();
            recorder = null;
            speexEncoder = null;
        }
    };



    public interface SpeexFrameListener{
        void onFrameRecorded(byte[] frame);
    }
}
