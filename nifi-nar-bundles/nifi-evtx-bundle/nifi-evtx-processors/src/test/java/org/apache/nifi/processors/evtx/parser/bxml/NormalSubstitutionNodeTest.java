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
public class NormalSubstitutionNodeTest extends BxmlNodeWithTokenTestBase {
    private int index = 30;
    private byte type = 20;
    private NormalSubstitutionNode normalSubstitutionNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.putWord(index);
        testBinaryReaderBuilder.put(type);
        normalSubstitutionNode = new NormalSubstitutionNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.NORMAL_SUBSTITUTION_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(BxmlNode.NORMAL_SUBSTITUTION_TOKEN, normalSubstitutionNode.getToken());
        assertEquals(index, normalSubstitutionNode.getIndex());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        normalSubstitutionNode.accept(mock);
        verify(mock).visit(normalSubstitutionNode);
        verifyNoMoreInteractions(mock);
    }
}
