package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by brosander on 6/2/16.
 */
public class StreamStartNodeTest extends BxmlNodeWithTokenTestBase {

    private StreamStartNode streamStartNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.put((byte) 1);
        testBinaryReaderBuilder.putWord(1);
        streamStartNode = new StreamStartNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Test
    public void testInit() {
        assertEquals(getToken(), streamStartNode.getToken());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        streamStartNode.accept(mock);
        verify(mock).visit(streamStartNode);
        verifyNoMoreInteractions(mock);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.START_OF_STREAM_TOKEN;
    }
}
