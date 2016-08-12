package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaRecorder;
import android.net.rtp.AudioCodec;
import android.util.Log;

import org.xiph.speex.SpeexEncoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Daffern on 11.07.2016.
 */
public class RecordHelper {

    private static final String TAG = RecordHelper.class.getSimpleName();

    private int sampleRate;
    private short audioFormat;
    private short channelConfig;

    private int recordBufferSize = 320; //320 bytes per sample / 160 shorts per sample


    ArrayList<byte[]> audioSamples;
    ArrayList<byte[]> encodedSamples;

    AudioRecord recorder;
    SpeexEncoder speexEncoder;

    boolean isRecording = false;
    boolean isEncoding = false;

    RecordListener recordListener;

    public void setRecordAudioListener(RecordListener recordListener) {
        this.recordListener = recordListener;
    }


    public void startRecord() {
        if (isRecording)
            return;

        recorder = findAudioRecord();

        audioSamples = new ArrayList<>();

        Thread recordingThread = new Thread(new Runnable() {

            public void run() {

                isRecording = true;
                recorder.startRecording();

                while (isRecording) {

                    byte[] buffer = new byte[recordBufferSize];
                    int bytes = recorder.read(buffer, recordBufferSize, AudioRecord.READ_BLOCKING);

                    audioSamples.add(buffer);
                }

                recorder.stop();
                recorder.release();
                recorder = null;

                recordListener.onRecordFinnish(audioSamples);
            }
        });

        recordingThread.start();
    }


    public void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            audioSamples = null;

        }
    }


    public void startEncoding() {
        if (isEncoding)
            return;

        speexEncoder = new SpeexEncoder();
        speexEncoder.init(0, 1, sampleRate, 1);

        encodedSamples = new ArrayList<>();

        Thread encodingThread = new Thread(new Runnable() {

            public void run() {

                isEncoding = true;
                for (int i = 0 ; i < audioSamples.size() ; i++){

                    byte[] samples = audioSamples.get(i);

                    byte[] encoded = encode(samples, 0, recordBufferSize);

                    encodedSamples.add(encoded);

                    recordListener.onEncodeProgress(i/audioSamples.size());

                }
                isEncoding = false;

                recordListener.onEncodeFinnish(encodedSamples);
            }
        });

        encodingThread.start();
    }

    byte[] encode(byte[] data, int offset, int length) {

        speexEncoder.processData(data, offset, length);
        int size = speexEncoder.getProcessedDataByteSize();
        byte[] newByte = new byte[size];
        speexEncoder.getProcessedData(newByte, 0);

        return newByte;
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


    private static int[] sampleRates = new int[]{8000, 11025, 16000, 22050, 44100};
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


    public interface RecordListener{
        void onRecordFinnish(ArrayList<byte[]> arrayList);
        void onEncodeProgress(int percent);
        void onEncodeFinnish(ArrayList<byte[]> arrayList);
    }
}
