package no.daffern.xbeecommunication.XBee;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import no.daffern.xbeecommunication.Model.Node;
import no.daffern.xbeecommunication.Utility;

/**
 * Created by Daffern on 12.06.2016.
 *
 * Parses an XBee AT command, called network discover (ND)
 *
 * Outputs a list of nodes with the data retrieved from the command
 */
public class XBeeATNetworkDiscover {

    private final static int MIN_ND_SIZE = 18;//The node discover command response is at least 18 bytes long, even if there are no nodes on the network
    public final static byte[] ND_COMMAND = new byte[]{0x4E, 0x44}; //Network Discover (ND)

    private ArrayList<Node> nodes;

    public XBeeATNetworkDiscover(byte[] bytes) {

        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

        nodes = new ArrayList<>();

        while (stream.available() >= MIN_ND_SIZE) {
            Node node = new Node();

            node.address16[0] = (byte) stream.read();
            node.address16[1] = (byte) stream.read();

            node.address64[0] = (byte) stream.read();
            node.address64[1] = (byte) stream.read();
            node.address64[2] = (byte) stream.read();
            node.address64[3] = (byte) stream.read();
            node.address64[4] = (byte) stream.read();
            node.address64[5] = (byte) stream.read();
            node.address64[6] = (byte) stream.read();
            node.address64[7] = (byte) stream.read();

            node.nodeIdentifier = Utility.readString(stream);

            node.parentNetworkAddress[0] = (byte) stream.read();
            node.parentNetworkAddress[1] = (byte) stream.read();

            node.deviceType = (byte) stream.read();
            node.status = (byte) stream.read();
            node.profileId[0] = (byte) stream.read();
            node.profileId[1] = (byte) stream.read();
            node.manufacturerId[0] = (byte) stream.read();
            node.manufacturerId[1] = (byte) stream.read();

            nodes.add(node);
        }

    }
    public ArrayList<Node> getNodes() {
        return nodes;
    }
}
