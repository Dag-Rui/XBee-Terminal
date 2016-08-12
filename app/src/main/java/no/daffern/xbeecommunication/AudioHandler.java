package no.daffern.xbeecommunication;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;


import no.daffern.xbeecommunication.Listener.MessageListener;

/**
 * Created by Daffern on 29.06.2016.
 */
public class AudioHandler {

    private final static String TAG = AudioHandler.class.getSimpleName();

    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
    private static short[] audioFormats = new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_DEFAULT, AudioFormat.ENCODING_AC3, AudioFormat.ENCODING_PCM_FLOAT };

    private int sampleRate;
    private short audioFormat;
    private short channelConfig;

    AudioRecord recorder;
    boolean isRecording = false;

    short[] buffer = new short[500000];
    int pointer = 0;

    MessageListener messageListener;

    public AudioHandler(){

    }
    public void setMessageListener(MessageListener messageListener){
        this.messageListener = messageListener;
    }

    private void startRecording() {

        recorder = findAudioRecord();

        recorder.startRecording();
        isRecording = true;

        Thread recordingThread = new Thread(new Runnable() {

            public void run() {
                int tempBufferSize = 1024;
                short sData[] = new short[tempBufferSize];

                while (isRecording) {
                    // gets the voice output from microphone to byte format

                    int shorts = recorder.read(buffer, pointer, tempBufferSize);
                    pointer += shorts;

                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }
    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }
    public void playAudio(byte[] bytes){
        try{
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,  AudioFormat.ENCODING_PCM_16BIT, 5000000, AudioTrack.MODE_STREAM );


            //while (input.available() > 0){

            audioTrack.write(bytes, 0, 500000);

            //audioTrack.play();
            //}

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : audioFormats) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.e(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
                                this.sampleRate = rate;
                                this.audioFormat = audioFormat;
                                this.channelConfig = channelConfig;

                                Log.e(TAG, "Using rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                                return recorder;
                            }

                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }
}
