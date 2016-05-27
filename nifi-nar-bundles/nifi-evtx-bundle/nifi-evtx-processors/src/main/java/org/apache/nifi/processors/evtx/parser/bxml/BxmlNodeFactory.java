package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/25/16.
 */
public interface BxmlNodeFactory {
    BxmlNode create(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException;
}
