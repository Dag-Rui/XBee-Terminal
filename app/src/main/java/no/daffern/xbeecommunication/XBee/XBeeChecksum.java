package no.daffern.xbeecommunication.XBee;

/**
 * Created by Daffern on 11.07.2016.
 */
public class XBeeChecksum {

    private int checkSumCounter=0;

    public void add(byte b){
        checkSumCounter +=b;
    }

    public void add(byte[] bytes){
        for (byte b : bytes){
            checkSumCounter += b;
        }
    }
    public byte generate(){
        checkSumCounter = checkSumCounter & 0xFF;
        return (byte)(0xFF - checkSumCounter);
    }
}
