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
    private static final SimpleDateFormat FORMAT = initFormat();
    private final String value;

    private static final SimpleDateFormat initFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public SystemtimeTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        int year = binaryReader.readWord().intValue();
        int month = binaryReader.readWord().intValue();
        int dayOfWeek = binaryReader.readWord().intValue();
        int day = binaryReader.readWord().intValue();
        int hour = binaryReader.readWord().intValue();
        int minute = binaryReader.readWord().intValue();
        int second = binaryReader.readWord().intValue();
        int microsecond = binaryReader.readWord().intValue();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, microsecond / 1000);
        value = FORMAT.format(calendar);
    }

    @Override
    public String getValue() {
        return value;
    }
}
