package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public interface BxmlNodeFactory {
    BxmlNode create(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException;
}
