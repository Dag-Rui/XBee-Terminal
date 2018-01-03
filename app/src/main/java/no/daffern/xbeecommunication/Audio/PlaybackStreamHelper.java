package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import com.purplefrog.speexjni.FrequencyBand;
import com.purplefrog.speexjni.SpeexDecoder;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import no.daffern.xbeecommunication.MainActivity;

/**
 * Created by Daffern on 20.09.2016.
 *
 * Helper class for playing speex encoded sound through addFrame()
 */

public class PlaybackStreamHelper {

    private final static String TAG = PlaybackStreamHelper.class.getSimpleName();

    int streamType = AudioManager.STREAM_MUSIC;
    int sampleRate = 8000;
    int channel = AudioFormat.CHANNEL_OUT_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    AudioTrack audioTrack;

    SpeexDecoder speexDecoder;

    boolean playing = false;

    Queue<byte[]> frames = new ConcurrentLinkedQueue<>();

    public void addFrame(byte[] frame) {
        frames.add(frame);
    }

    public void start() {
        speexDecoder = new SpeexDecoder(FrequencyBand.NARROW_BAND);


        audioTrack = new AudioTrack(streamType, sampleRate, channel, audioFormat, 160, AudioTrack.MODE_STREAM);

        if (audioTrack.getState() != AudioRecord.STATE_INITIALIZED) {
            MainActivity.makeToast("Could not initialize AudioTrack");
            Log.e(TAG, "Could not initialize AudioTrack");
        }

        Thread playbackThread = new Thread(new Runnable() {
            @Override
            public void run() {

                audioTrack.play();
                playing = true;

                while (playing) {

                    byte[] encoded = frames.poll();

                    if (encoded != null) {


                        short[] frame = speexDecoder.decode(encoded);

                        audioTrack.write(frame, 0, frame.length);
                    }
                }
            }
        });
        playbackThread.start();
    }
}
