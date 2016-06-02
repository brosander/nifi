package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.bxml.value.NullTypeNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by brosander on 6/2/16.
 */
public class ValueNodeTest extends BxmlNodeWithTokenTestBase {
    private ValueNode valueNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.put((byte)0);
        valueNode = new ValueNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.VALUE_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(getToken(), valueNode.getToken());
        List<BxmlNode> children = valueNode.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof NullTypeNode);
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        valueNode.accept(mock);
        verify(mock).visit(valueNode);
        verifyNoMoreInteractions(mock);
    }
}
