package no.daffern.xbeecommunication.Model;

import java.util.Arrays;

import no.daffern.xbeecommunication.Utility;

/**
 * Created by Daffern on 12.06.2016.
 *
 * Contains the data of a XBee node connected through the XBee network
 */
public class Node {

    public byte[] address16 = new byte[2];
    public byte[] address64 = new byte[8];
    public String nodeIdentifier;//max size is 20 bytes
    public byte[] parentNetworkAddress = new byte[2];
    public byte deviceType;
    public byte status;
    public byte[] profileId = new byte[2];
    public byte[] manufacturerId = new byte[2];

    public Node() {
    }

    public Node(byte[] address64) {
        this.address64 = address64;
    }

    public Node(byte[] address16, byte[] address64, String name) {
        this.address16 = address16;
        this.address64 = address64;
        this.nodeIdentifier = name;
    }

    public String toString() {
        return "{64 bit address is: " + Utility.bytesToHex(address64) + "; 16 bit address is: " + Utility.bytesToHex(address16) + "; Name is: " + nodeIdentifier + "}";
    }

    //returns the nodeIdentifier, or the address if not available
    public String getNodeIdentifier() {
        if (nodeIdentifier != null && nodeIdentifier.length() > 0) {
            return nodeIdentifier;
        } else if (address64 != null && address64.length > 0) {
            return Utility.bytesToHex(address64);
        } else return "";
    }

    //HashCode used in mapping the Node
    public int getKey() {
        return Arrays.hashCode(address64);
    }

    public static int getKey(byte[] bytes) {
        return Arrays.hashCode(bytes);
    }

}
