package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.net.rtp.AudioCodec;
import android.os.Handler;
import android.util.Log;

import com.purplefrog.speexjni.FrequencyBand;
import com.purplefrog.speexjni.SpeexEncoder;


import java.util.ArrayList;

/**
 * Created by Daffern on 11.07.2016.
 */
public class RecordHelper {

    private static final String TAG = RecordHelper.class.getSimpleName();

    private int sampleRate;
    private short audioFormat;
    private short channelConfig;

    private int recordBufferSize = 160; //320 bytes per sample / 160 shorts per sample


    ArrayList<byte[]> encodedSamples;

    AudioRecord recorder;
    SpeexEncoder speexEncoder;

    boolean isRecording = false;


    public void startRecord() {
        if (isRecording)
            return;

        recorder = findAudioRecord();

        int sessionId = recorder.getAudioSessionId();

        AcousticEchoCanceler acousticEchoCanceler = AcousticEchoCanceler.create(sessionId);
        acousticEchoCanceler.setEnabled(true);

        if (acousticEchoCanceler.getEnabled()){
            Log.d(TAG,"AEC enabled");
        }else{
            Log.d(TAG,"could not enable AEC");
        }


        Thread recordingThread = new Thread(new Runnable() {

            public void run() {

                isRecording = true;
                recorder.startRecording();

                while (isRecording) {

                    short[] buffer = new short[recordBufferSize];
                    int bytes = recorder.read(buffer, 0, recordBufferSize);

                    byte[] encoded = speexEncoder.encode(buffer);

                    encodedSamples.add(encoded);
                }

                recorder.stop();
                recorder.release();
                recorder = null;

            }

        });

        recordingThread.start();
    }


    public void stop() {
        isRecording = false;

    }

    public byte[] getNextSample(){
        return null;
    }


    public void printCodecs() {
        AudioCodec[] codecs = AudioCodec.getCodecs();
        for (AudioCodec codec : codecs) {
            Log.e(TAG, "Audio Codec: " + codec.toString());

        }

        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j] != null)
                    Log.e(TAG, "Media Codec: " + types[j]);
            }
        }
    }


    private static int[] sampleRates = new int[]{8000, 16000, 32000};
    private static short[] audioFormats = new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_DEFAULT, AudioFormat.ENCODING_AC3, AudioFormat.ENCODING_PCM_FLOAT};

    public AudioRecord findAudioRecord() {
        for (int rate : sampleRates) {
            for (short audioFormat : audioFormats) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        Log.e(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                this.sampleRate = rate;
                                this.audioFormat = audioFormat;
                                this.channelConfig = channelConfig;

                                Log.e(TAG, "Using rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                                return recorder;
                            }

                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.", e);
                    }
                }
            }
        }
        return null;
    }



}
