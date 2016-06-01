package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Created by brosander on 5/26/16.
 */
public class FiletimeTypeNode extends VariantTypeNode {
    private final String value;

    public FiletimeTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        value = getFormat().format(binaryReader.readFileTime());
    }

    public static final SimpleDateFormat getFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    @Override
    public String getValue() {
        return value.toString();
    }
}
