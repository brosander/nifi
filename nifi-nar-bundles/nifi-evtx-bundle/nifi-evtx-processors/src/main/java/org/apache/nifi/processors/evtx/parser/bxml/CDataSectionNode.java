package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class CDataSectionNode extends BxmlNodeWithToken {
    private final UnsignedInteger stringLength;
    private final String cdata;

    public CDataSectionNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        if (getFlags() != 0) {
            throw new IOException("Invalid flags");
        }
        if (getToken() != C_DATA_SECTION_TOKEN) {
            throw new IOException("Invalid CDataSectionToken");
        }
        stringLength = binaryReader.readWord();
        cdata = binaryReader.readWString(stringLength.intValue() - 2);
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    public String getCdata() {
        return cdata;
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
