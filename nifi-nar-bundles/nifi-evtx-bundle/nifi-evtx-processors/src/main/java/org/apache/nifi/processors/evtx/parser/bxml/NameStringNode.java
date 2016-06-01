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
public class NameStringNode extends BxmlNode {
    private final UnsignedInteger nextOffset;
    private final int hash;
    private final String string;
    private final int stringLength;

    public NameStringNode(BinaryReader binaryReader, ChunkHeader chunkHeader) throws IOException {
        super(binaryReader, chunkHeader, null);
        nextOffset = binaryReader.readDWord();
        hash = binaryReader.readWord();
        stringLength = binaryReader.readWord();
        string = binaryReader.readWString(stringLength);
        binaryReader.skip(2);
        init();
    }

    public UnsignedInteger getNextOffset() {
        return nextOffset;
    }

    public int getHash() {
        return hash;
    }

    public String getString() {
        return string;
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
