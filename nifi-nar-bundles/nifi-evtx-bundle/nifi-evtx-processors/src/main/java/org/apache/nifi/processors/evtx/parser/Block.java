package org.apache.nifi.processors.evtx.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/24/16.
 */
public abstract class Block extends BinaryReader {
    private final long offset;
    private boolean initialized = false;

    public Block(InputStream inputStream, long offset) {
        super(inputStream);
        this.offset = offset;
    }

    public long getOffset() {
        return offset;
    }

    public long getCurrentOffset() {
        return offset + getImplicitOffset();
    }

    protected void init() throws IOException {
        if (initialized) {
            throw new IOException("Initialize should only be called once");
        } else {
            initialized = true;
        }
        long skipAmount = getEndOfHeader() - getImplicitOffset();
        if (skipAmount > 0) {
            skip(skipAmount);
        }
    }

    protected int getEndOfHeader() {
        return 0;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
