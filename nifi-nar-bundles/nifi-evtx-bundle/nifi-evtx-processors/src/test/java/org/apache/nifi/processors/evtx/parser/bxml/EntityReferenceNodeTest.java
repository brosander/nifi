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
public class EntityReferenceNodeTest extends BxmlNodeWithTokenAndStringTestBase {
    public static final String AMP = "amp";
    private EntityReferenceNode entityReferenceNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        entityReferenceNode = new EntityReferenceNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected String getString() {
        return AMP;
    }

    @Override
    protected byte getToken() {
        return BxmlNode.ENTITY_REFERENCE_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(BxmlNode.ENTITY_REFERENCE_TOKEN, entityReferenceNode.getToken());
    }

    @Test
    public void testGetValue() {
        assertEquals("&" + AMP + ";", entityReferenceNode.getValue());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        entityReferenceNode.accept(mock);
        verify(mock).visit(entityReferenceNode);
        verifyNoMoreInteractions(mock);
    }
}
