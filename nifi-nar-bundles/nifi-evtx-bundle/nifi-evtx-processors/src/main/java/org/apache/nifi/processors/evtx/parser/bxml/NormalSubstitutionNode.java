package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class NormalSubstitutionNode extends BxmlNodeWithToken {
    private final UnsignedInteger index;
    private final int type;

    public NormalSubstitutionNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        index = readWord();
        type = read();
        if (getFlags() != 0) {
            throw new IOException("Invalid flags");
        }
        if (getToken() != NORMAL_SUBSTITUTION_TOKEN) {
            throw new IOException("Invalid token");
        }
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }
}
