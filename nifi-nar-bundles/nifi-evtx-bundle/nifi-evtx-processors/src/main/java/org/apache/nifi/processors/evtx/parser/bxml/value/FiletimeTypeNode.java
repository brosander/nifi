package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by brosander on 5/26/16.
 */
public class FiletimeTypeNode extends VariantTypeNode {
    private static final SimpleDateFormat FORMAT = initFormat();
    private final String value;

    private static final SimpleDateFormat initFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public FiletimeTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        value = FORMAT.format(readFileTime());
    }

    @Override
    public String getValue() {
        return value.toString();
    }
}
