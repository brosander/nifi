package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by brosander on 6/1/16.
 */
public class CloseElementNodeTest extends BxmlNodeWithTokenTestBase {
    private CloseElementNode closeElementNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        closeElementNode = new CloseElementNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.CLOSE_ELEMENT_TOKEN;
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        closeElementNode.accept(mock);
        verify(mock).visit(closeElementNode);
        verifyNoMoreInteractions(mock);
    }
}
