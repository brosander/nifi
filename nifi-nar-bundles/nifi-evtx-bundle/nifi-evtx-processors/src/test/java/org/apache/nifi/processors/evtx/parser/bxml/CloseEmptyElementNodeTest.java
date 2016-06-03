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
public class CloseEmptyElementNodeTest extends BxmlNodeWithTokenTestBase {
    private CloseEmptyElementNode closeEmptyElementNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        closeEmptyElementNode = new CloseEmptyElementNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.CLOSE_EMPTY_ELEMENT_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(BxmlNode.CLOSE_EMPTY_ELEMENT_TOKEN, closeEmptyElementNode.getToken());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        closeEmptyElementNode.accept(mock);
        verify(mock).visit(closeEmptyElementNode);
        verifyNoMoreInteractions(mock);
    }
}
