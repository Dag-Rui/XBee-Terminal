package no.daffern.xbeecommunication.XBee;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.Utility;

/**
 * Created by Daffern on 12.06.2016.
 */
public class XBeeATNetworkDiscover {

    private final static int MIN_ND_SIZE = 18;
    public final static byte[] ND_COMMAND = new byte[]{0x4E, 0x44}; //Network Discover (ND)

    ArrayList<Node> nodes;

    public XBeeATNetworkDiscover(byte[] bytes) {

        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

        nodes = new ArrayList<>();

        while(stream.available() >= MIN_ND_SIZE) {
            Node node = new Node();

            node.address16[0] = (byte)stream.read();
            node.address16[1] = (byte)stream.read();

            node.address64[0] = (byte)stream.read();
            node.address64[1] = (byte)stream.read();
            node.address64[2] = (byte)stream.read();
            node.address64[3] = (byte)stream.read();
            node.address64[4] = (byte)stream.read();
            node.address64[5] = (byte)stream.read();
            node.address64[6] = (byte)stream.read();
            node.address64[7] = (byte)stream.read();

            node.nodeIdentifier = Utility.readString(stream);

            node.parentNetworkAddress[0] = (byte)stream.read();
            node.parentNetworkAddress[1] = (byte)stream.read();

            node.deviceType = (byte)stream.read();
            node.status = (byte)stream.read();
            node.profileId[0] = (byte)stream.read();
            node.profileId[1] = (byte)stream.read();
            node.manufacturerId[0] = (byte)stream.read();
            node.manufacturerId[1] = (byte)stream.read();

            nodes.add(node);
        }

    }
/*
    private void something(byte[] bytes){
        address16[0] = bytes[0];
        address16[1] = bytes[1];

        address64[0] = bytes[2];
        address64[1] = bytes[3];
        address64[2] = bytes[4];
        address64[3] = bytes[5];
        address64[4] = bytes[6];
        address64[5] = bytes[7];
        address64[6] = bytes[8];
        address64[7] = bytes[9];

        nodeIdentifier = Utility.readString(bytes, 10);
        int bytesRead = nodeIdentifier.length();

        parentNetworkAddress[0] = bytes[bytesRead + 10];
        parentNetworkAddress[1] = bytes[bytesRead + 11];

        deviceType = bytes[bytesRead + 12];
        status = bytes[bytesRead + 13];
        profileId[0] = bytes[bytesRead + 14];
        profileId[1] = bytes[bytesRead + 15];
        manufacturerId[0] = bytes[bytesRead + 16];
        manufacturerId[1] = bytes[bytesRead + 17];
    }*/

    public ArrayList<Node> getNodes(){
        return nodes;
    }
}
