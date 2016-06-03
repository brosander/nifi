package org.apache.nifi.processors.evtx.parser;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.apache.nifi.processors.evtx.parser.bxml.EndOfStreamNode;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by brosander on 6/2/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class RecordTest {
    @Mock
    ChunkHeader chunkHeader;

    private Record record;
    private int recordNum = 120;

    public static void putNode(TestBinaryReaderBuilder testBinaryReaderBuilder, int recordNum, Date fileTime) {
        testBinaryReaderBuilder.putDWord(10794);
        int size = 20;
        testBinaryReaderBuilder.putDWord(size);
        testBinaryReaderBuilder.putQWord(recordNum);
        testBinaryReaderBuilder.putFileTime(new Date());

        testBinaryReaderBuilder.put((byte) BxmlNode.END_OF_STREAM_TOKEN);
        testBinaryReaderBuilder.putDWord(0);
        testBinaryReaderBuilder.putDWord(size);
    }

    @Before
    public void setup() throws IOException {
        TestBinaryReaderBuilder testBinaryReaderBuilder = new TestBinaryReaderBuilder();
        putNode(testBinaryReaderBuilder, recordNum, new Date());

        record = new Record(testBinaryReaderBuilder.build(), chunkHeader);
    }

    @Test
    public void testInit() {
        assertEquals(recordNum, record.getRecordNum().intValue());
        RootNode rootNode = record.getRootNode();
        List<BxmlNode> children = rootNode.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof EndOfStreamNode);
        assertEquals(0, rootNode.getSubstitutions().size());
    }
}
