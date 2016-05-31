package org.apache.nifi.processors.evtx.parser;

import java.io.IOException;

/**
 * Created by brosander on 5/24/16.
 */
public abstract class Block {
    private final long offset;
    private final int initialPosition;
    private BinaryReader binaryReader;
    private boolean initialized = false;

    public Block(BinaryReader binaryReader) {
        this(binaryReader, binaryReader.getPosition());
    }

    public Block(BinaryReader binaryReader, long offset) {
        this.binaryReader = binaryReader;
        this.initialPosition = binaryReader.getPosition();
        this.offset = offset;
    }

    public BinaryReader getBinaryReader() {
        return binaryReader;
    }

    public long getOffset() {
        return offset;
    }

    /*public long getCurrentOffset() {
        return offset + getImplicitOffset();
    }*/

    protected void init(boolean clearBinaryReader) throws IOException {
        if (initialized) {
            throw new IOException("Initialize should only be called once");
        } else {
            initialized = true;
        }
        int skipAmount = getHeaderLength() - (binaryReader.getPosition() - initialPosition);
        if (skipAmount > 0) {
            binaryReader.skip(skipAmount);
        }

        if (clearBinaryReader) {
            clearBinaryReader();
        }
    }

    protected void init() throws IOException {
        init(true);
    }

    protected void clearBinaryReader() {
        this.binaryReader = null;
    }

    protected int getHeaderLength() {
        return 0;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getInitialPosition() {
        return initialPosition;
    }
}
