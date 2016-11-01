package no.daffern.xbeecommunication;

/**
 * Created by Daffern on 07.08.2016.
 */
public class XBeeConfig {
    public static byte[] serialNumberLow;
    public static byte[] serialNumberHigh;
    public static byte[] firmwareVersion;
    public static byte[] hardwareVersion;

    public static byte[] operatingChannel;
    public static byte[] networkId;

    public static byte[] networkDiscoveryTimeout; //4 bytes long


    public static int getNetworkDiscoveryTimeout(){
        if (networkDiscoveryTimeout == null)
            return 0;
        int timeout = networkDiscoveryTimeout[0] << 32 | networkDiscoveryTimeout[1] << 16 | networkDiscoveryTimeout[2] << 8 | networkDiscoveryTimeout[3];
        return timeout;
    }
}
