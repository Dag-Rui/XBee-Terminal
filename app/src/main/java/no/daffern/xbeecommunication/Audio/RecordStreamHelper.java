package no.daffern.xbeecommunication.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;

import com.purplefrog.speexjni.FrequencyBand;
import com.purplefrog.speexjni.SpeexEncoder;

import no.daffern.xbeecommunication.MainActivity;

/**
 * Created by Daffern on 20.09.2016.
 *
 * Helper class for recording sound and encoding into the Speex format
 * Use setSpeexFrameListener to obtain encoded data
 */

public class RecordStreamHelper {

    private final static String TAG = RecordStreamHelper.class.getSimpleName();

    int sampleRate = 8000;
    int channel = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    final int speexFrameSize = 160; //The speex framesize for narrowband in shorts (320 bytes)
    short[] buffer = new short[speexFrameSize];
    ;

    AudioRecord recorder;
    SpeexEncoder speexEncoder;

    RecordThread recordThread;
    boolean recording = false;

    SpeexFrameListener speexFrameListener;
    static AudioAmplitudePeakListener audioAmplitudePeakListener;

    double currentAmplitude = 0;
    static double peakAmplitude = 0;
    static long peakAmplitudeTime = 0;

    public void setSpeexFrameListener(SpeexFrameListener speexFrameListener) {
        this.speexFrameListener = speexFrameListener;
    }

    public static void setAudioAmplitudePeakListener(AudioAmplitudePeakListener listener) {
        audioAmplitudePeakListener = listener;
    }

    public void start(int quality) {

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, audioFormat, bufferSize);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            MainActivity.makeToast("Could not initialize audio recorder");
            Log.e(TAG, "Could not initialize AudioRecord");
        }

        AcousticEchoCanceler acousticEchoCanceler = AcousticEchoCanceler.create(recorder.getAudioSessionId());

        if (acousticEchoCanceler != null && !acousticEchoCanceler.getEnabled()) {
            MainActivity.makeToast("Could not enable AcousticEchoCanceler");
            Log.e(TAG, "Could not enable AcousticEchoCanceler");
        }

        speexEncoder = new SpeexEncoder(FrequencyBand.NARROW_BAND, quality);

        recordThread = new RecordThread();
        Thread t = new Thread(recordThread);
        t.start();
    }

    public void setQuality(int quality) {
        if (quality > 10 || quality < 0) {
            MainActivity.makeToast("Wrong quality set for some reason");
        } else
            speexEncoder = new SpeexEncoder(FrequencyBand.NARROW_BAND, quality);
    }

    public void stop() {
        recordThread.cancel();
    }


    private class RecordThread implements Runnable {

        @Override
        public void run() {

            recorder.startRecording();
            recording = true;

            while (recording) {

                recorder.read(buffer, 0, buffer.length);

                if (audioAmplitudePeakListener != null) {
                    currentAmplitude = measureMagnitude(buffer);
                    if (currentAmplitude > peakAmplitude) {
                        peakAmplitude = currentAmplitude;
                        peakAmplitudeTime = System.currentTimeMillis();
                        audioAmplitudePeakListener.onAudioAmplitudePeak(peakAmplitude, peakAmplitudeTime);
                    }
                }

                byte[] encoded = speexEncoder.encode(buffer);

                speexFrameListener.onFrameEncoded(encoded);
            }
        }

        public void cancel() {
            recording = false;
            recorder.stop();
            recorder = null;
            speexEncoder = null;
        }
    }

    private double measureMagnitude(short[] samples) {
        double sum = 0;

        for (short s : samples)
            sum += s * s;

        double amplitude = Math.sqrt(sum / samples.length);
        return amplitude;
    }

    public static void resetPeakAmplitude() {
        peakAmplitudeTime = 0;
        peakAmplitude = 0;
    }

    public interface SpeexFrameListener {
        void onFrameEncoded(byte[] frame);
    }

    public interface AudioAmplitudePeakListener {
        void onAudioAmplitudePeak(double amplitude, long time);
    }
}
