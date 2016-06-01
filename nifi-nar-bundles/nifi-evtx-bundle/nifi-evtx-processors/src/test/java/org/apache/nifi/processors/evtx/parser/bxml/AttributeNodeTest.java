package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.bxml.value.NullTypeNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by brosander on 6/1/16.
 */
public class AttributeNodeTest extends BxmlNodeWithTokenAndStringTestBase {
    public static final String ATTRIBUTE_NAME = "AttributeName";
    private BinaryReader binaryReader;
    private AttributeNode attributeNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.put((byte) BxmlNode.VALUE_TOKEN);
        testBinaryReaderBuilder.put((byte) 0);
        attributeNode = new AttributeNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.ATTRIBUTE_TOKEN;
    }

    @Override
    protected String getString() {
        return ATTRIBUTE_NAME;
    }

    @Test
    public void testInit() {
        assertEquals(ATTRIBUTE_NAME, attributeNode.getAttributeName());
        BxmlNode attributeNodeValue = attributeNode.getValue();
        assertTrue(attributeNodeValue instanceof ValueNode);
        List<BxmlNode> children = ((ValueNode) attributeNodeValue).getChildren();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof NullTypeNode);
    }

    @Test
    public void testVisit() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        attributeNode.accept(mock);
        verify(mock).visit(attributeNode);
        verifyNoMoreInteractions(mock);
    }
}
