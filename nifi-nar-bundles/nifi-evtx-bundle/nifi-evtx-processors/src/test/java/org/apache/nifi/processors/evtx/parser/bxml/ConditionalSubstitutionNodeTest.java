package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by brosander on 6/1/16.
 */
public class ConditionalSubstitutionNodeTest extends BxmlNodeWithTokenTestBase {
    private ConditionalSubstitutionNode conditionalSubstitutionNode;
    private int index;
    private byte type;

    @Override
    public void setup() throws IOException {
        super.setup();
        index = 10;
        type = 5;
        testBinaryReaderBuilder.putWord(index);
        testBinaryReaderBuilder.put(type);
        conditionalSubstitutionNode = new ConditionalSubstitutionNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.CONDITIONAL_SUBSTITUTION_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(BxmlNode.CONDITIONAL_SUBSTITUTION_TOKEN, conditionalSubstitutionNode.getToken());
        assertEquals(index, conditionalSubstitutionNode.getIndex());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        conditionalSubstitutionNode.accept(mock);
        verify(mock).visit(conditionalSubstitutionNode);
        verifyNoMoreInteractions(mock);
    }
}
