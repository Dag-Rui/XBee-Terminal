package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


import com.purplefrog.speexjni.FrequencyBand;
import com.purplefrog.speexjni.SpeexDecoder;

import java.io.StreamCorruptedException;
import java.util.ArrayList;

/**
 * Created by Daffern on 11.07.2016.
 */
public class PlaybackHelper {

    final static int STATE_PLAYING = 0;
    final static int STATE_STOPPED = 1;
    final static int STATE_BUFFERING = 2;

    int state = STATE_STOPPED;

    AudioTrack audioTrack;
    int streamType = AudioManager.STREAM_MUSIC;
    int sampleRate = 8000;
    int channel = AudioFormat.CHANNEL_OUT_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize = 8000;

    SpeexDecoder speexDecoder;

    ArrayList<short[]> audioSamples;

    PlaybackListener playbackListener;



    public void setPlaybackListener(PlaybackListener playbackListener){
        this.playbackListener = playbackListener;
    }


    public void addDecoded(byte[] bytes){

        if (state == STATE_STOPPED) {
            audioSamples = new ArrayList<>();
            speexDecoder = new SpeexDecoder(FrequencyBand.NARROW_BAND);
        }

        short[] encoded = speexDecoder.decode(bytes);
        audioSamples.add(encoded);
    }


    public void start(){

        audioSamples = new ArrayList<>();

        audioTrack = new AudioTrack(streamType, sampleRate, channel, audioFormat, 1000000, AudioTrack.MODE_STATIC);

        Thread decodeThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (audioSamples.size() > 0){
                    short[] samples = audioSamples.remove(0);
                    audioTrack.write(samples,0,samples.length);
                }
            }
        });

        decodeThread.start();

    }


    public void stopPlayer(){
        audioTrack.stop();
        audioTrack = null;
        speexDecoder = null;
    }




    public interface PlaybackListener{
        void onPlayFinnish();
        void onEncodeFinnish();
        void onEncodeProgress(int percent);
    }
}
