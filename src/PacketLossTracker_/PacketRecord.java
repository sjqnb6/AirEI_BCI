package PacketLossTracker_;

import java.util.List;

public class PacketRecord {
    public int numLost;
    public int numReceived;

    PacketRecord(int _numLost, int _numReceived) {
        numLost = _numLost;
        numReceived = _numReceived;
    }

    public int getNumExpected() {
        return numLost + numReceived;
    }

    public float getLostPercent() {
        if(getNumExpected() == 0) {
            return 0.f;
        }

        return numLost * 100.f / getNumExpected();
    }

    public void appendAll(List<PacketRecord> toAppend) {
        for (PacketRecord record : toAppend) {
            numLost += record.numLost;
            numReceived += record.numReceived;
        }
    }
}