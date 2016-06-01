package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by brosander on 5/26/16.
 */
public class SystemtimeTypeNode extends VariantTypeNode {
    private final String value;

    public SystemtimeTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        int year = binaryReader.readWord();
        int month = binaryReader.readWord();
        int dayOfWeek = binaryReader.readWord();
        int day = binaryReader.readWord();
        int hour = binaryReader.readWord();
        int minute = binaryReader.readWord();
        int second = binaryReader.readWord();
        int millisecond = binaryReader.readWord();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
        value = getFormat().format(calendar.getTime());
    }

    public static final SimpleDateFormat getFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    @Override
    public String getValue() {
        return value;
    }
}
