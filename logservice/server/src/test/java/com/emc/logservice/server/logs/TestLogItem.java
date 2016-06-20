package com.emc.logservice.server.logs;

import com.emc.logservice.common.*;
import com.emc.logservice.server.LogItem;

import java.io.*;

/**
 * Test LogItem implementation that allows injecting serialization errors.
 */
class TestLogItem implements LogItem {
    private final long seqNo;
    private final byte[] data;
    private double failAfterCompleteRatio;
    private IOException exception;

    public TestLogItem(long seqNo, byte[] data) {
        this.seqNo = seqNo;
        this.data = data;
        this.failAfterCompleteRatio = -1;
    }

    public TestLogItem(InputStream input) throws SerializationException {
        DataInputStream dataInput = new DataInputStream(input);
        try {
            this.seqNo = dataInput.readLong();
            this.data = new byte[dataInput.readInt()];
            int readBytes = StreamHelpers.readAll(dataInput, this.data, 0, this.data.length);
            assert readBytes == this.data.length;
        }
        catch (IOException ex) {
            throw new SerializationException("TestLogItem.deserialize", ex.getMessage(), ex);
        }
        this.failAfterCompleteRatio = -1;
    }

    public void failSerializationAfterComplete(double ratio, IOException exception) {
        if (exception != null) {
            Exceptions.throwIfIllegalArgument(0 <= ratio && ratio < 1, "ratio");
        }

        this.failAfterCompleteRatio = ratio;
        this.exception = exception;
    }

    public byte[] getData() {
        return this.data;
    }

    public byte[] getFullSerialization() {
        byte[] result = new byte[Long.BYTES + Integer.BYTES + this.data.length];
        try {
            this.serialize(new FixedByteArrayOutputStream(result, 0, result.length));
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }

    @Override
    public long getSequenceNumber() {
        return this.seqNo;
    }

    @Override
    public void serialize(OutputStream output) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(output);
        dataOutput.writeLong(seqNo);
        dataOutput.writeInt(data.length);
        if (this.exception == null) {
            dataOutput.write(data);
        }
        else {
            int breakPoint = (int) (data.length * this.failAfterCompleteRatio);
            dataOutput.write(data, 0, breakPoint);
            throw this.exception;
        }
    }
}