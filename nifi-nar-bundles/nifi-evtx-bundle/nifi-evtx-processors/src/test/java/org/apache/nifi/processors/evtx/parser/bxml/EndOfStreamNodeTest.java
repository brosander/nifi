package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by brosander on 6/1/16.
 */
public class EndOfStreamNodeTest extends BxmlNodeWithTokenTestBase {
    private EndOfStreamNode endOfStreamNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        endOfStreamNode = new EndOfStreamNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.END_OF_STREAM_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(BxmlNode.END_OF_STREAM_TOKEN, endOfStreamNode.getToken());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        endOfStreamNode.accept(mock);
        verify(mock).visit(endOfStreamNode);
        verifyNoMoreInteractions(mock);
    }
}
