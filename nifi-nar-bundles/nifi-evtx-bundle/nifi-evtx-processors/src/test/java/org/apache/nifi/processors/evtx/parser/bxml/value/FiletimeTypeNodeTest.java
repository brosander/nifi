package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class FiletimeTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testFiletimeTypeNode() throws IOException {
        Date date = new Date();
        assertEquals(FiletimeTypeNode.getFormat().format(date),
                new FiletimeTypeNode(testBinaryReaderBuilder.putFileTime(date).build(), chunkHeader, parent, -1).getValue());
    }
}
