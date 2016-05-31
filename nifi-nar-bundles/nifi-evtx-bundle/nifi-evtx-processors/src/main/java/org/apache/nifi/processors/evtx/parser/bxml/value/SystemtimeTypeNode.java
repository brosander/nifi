package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;
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

    public SystemtimeTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        int year = readWord().intValue();
        int month = readWord().intValue();
        int dayOfWeek = readWord().intValue();
        int day = readWord().intValue();
        int hour = readWord().intValue();
        int minute = readWord().intValue();
        int second = readWord().intValue();
        int microsecond = readWord().intValue();
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
