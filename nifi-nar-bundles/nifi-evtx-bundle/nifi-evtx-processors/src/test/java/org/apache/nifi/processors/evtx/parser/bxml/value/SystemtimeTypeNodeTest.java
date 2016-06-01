package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SystemtimeTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testSystemtimeTypeNode() throws IOException {
        Calendar calendar = Calendar.getInstance();
        assertEquals(SystemtimeTypeNode.getFormat().format(calendar.getTime()),
                new SystemtimeTypeNode(testBinaryReaderBuilder.putSystemtime(calendar).build(), chunkHeader, parent, -1).getValue());
    }
}
