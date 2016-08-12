package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


import org.xiph.speex.SpeexDecoder;

import java.io.StreamCorruptedException;
import java.util.ArrayList;

/**
 * Created by Daffern on 11.07.2016.
 */
public class PlaybackHelper {

    AudioTrack audioTrack;
    int streamType = AudioManager.STREAM_MUSIC;
    int sampleRate = 8000;
    int channel = AudioFormat.CHANNEL_OUT_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize = 8000;

    SpeexDecoder speexDecoder;

    ArrayList<byte[]> encodedSamples;
    ArrayList<byte[]> audioSamples;

    PlaybackListener playbackListener;

    public void setPlaybackListener(PlaybackListener playbackListener){
        this.playbackListener = playbackListener;
    }


    public void addDecoded(byte[] bytes){
        if (encodedSamples == null)
            encodedSamples = new ArrayList<>();
        encodedSamples.add(bytes);
    }
    public ArrayList<byte[]> getEncodedSamples(){
        return encodedSamples;
    }


    public void startDecode(){

        audioSamples = new ArrayList<>();

        speexDecoder = new SpeexDecoder();
        speexDecoder.init(0,sampleRate, 1, false);


        Thread decodeThread = new Thread(new Runnable() {
            @Override
            public void run() {



                while (encodedSamples.size() > 0){
                    byte[] encoded = encodedSamples.remove(0);
                    byte[] decoded = decode(encoded,0,encoded.length);
                    audioSamples.add(decoded);

                }
            }
        });

        decodeThread.start();

    }

    public void startPlayer() {
        audioTrack = new AudioTrack(streamType, sampleRate, channel, audioFormat, 1000000, AudioTrack.MODE_STATIC);

        for (byte[] bytes : audioSamples){
            audioTrack.write(bytes,0,bytes.length);
        }
        audioTrack.play();
    }
    public void stopPlayer(){
        audioTrack.stop();
        audioTrack = null;
        speexDecoder = null;
    }

    public void playEncodedAudio(byte[] bytes, int offset, int length) {
        try {

            byte[] decoded = decode(bytes, offset, length);

            //if (audioTrack.getPlaybackRate() == AudioTrack.PLAYSTATE_PLAYING)
                audioTrack.write(decoded, 0, decoded.length);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] decode(byte[] bytes, int offset, int length) {
        try {
            speexDecoder.processData(bytes, offset, length);

        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        }
        int size = speexDecoder.getProcessedDataByteSize();
        byte[] newByte = new byte[size];

        speexDecoder.getProcessedData(newByte, 0);
        return newByte;
    }


    public interface PlaybackListener{
        void onPlayFinnish();
        void onEncodeFinnish();
        void onEncodeProgress(int percent);
    }
}
