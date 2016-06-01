package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by brosander on 6/1/16.
 */
public class CloseStartElementNodeTest extends BxmlNodeWithTokenTestBase {
    private CloseStartElementNode closeStartElementNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        closeStartElementNode = new CloseStartElementNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.CLOSE_START_ELEMENT_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(BxmlNode.CLOSE_START_ELEMENT_TOKEN, closeStartElementNode.getToken());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        closeStartElementNode.accept(mock);
        verify(mock).visit(closeStartElementNode);
        verifyNoMoreInteractions(mock);
    }
}
