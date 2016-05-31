package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public abstract class BxmlNodeWithToken extends BxmlNode {
    private final int token;

    public BxmlNodeWithToken(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        token = binaryReader.read();
    }

    public int getToken() {
        return token;
    }

    public int getFlags() {
        return getToken() >> 4;
    }
}
