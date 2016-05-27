package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/25/16.
 */
public abstract class BxmlNodeWithToken extends BxmlNode {
    private final int token;

    public BxmlNodeWithToken(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        token = read();
    }

    public int getToken() {
        return token;
    }

    public int getFlags() {
        return getToken() >> 4;
    }
}
