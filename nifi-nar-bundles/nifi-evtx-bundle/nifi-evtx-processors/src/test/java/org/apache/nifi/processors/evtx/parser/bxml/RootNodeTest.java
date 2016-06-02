package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.bxml.value.VariantTypeNode;
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
public class RootNodeTest extends BxmlNodeTestBase {

    private String testString = "testString";
    private RootNode rootNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.put((byte) BxmlNode.END_OF_STREAM_TOKEN);
        testBinaryReaderBuilder.putDWord(1);

        testBinaryReaderBuilder.putWord(testString.length() + 1);
        testBinaryReaderBuilder.putWord(2);
        testBinaryReaderBuilder.putString(testString);
        testBinaryReaderBuilder.put((byte) 0);

        rootNode = new RootNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Test
    public void testInit() {
        List<BxmlNode> children = rootNode.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof EndOfStreamNode);

        List<VariantTypeNode> substitutions = rootNode.getSubstitutions();
        assertEquals(1, substitutions.size());
        assertEquals(testString, substitutions.get(0).getValue());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        rootNode.accept(mock);
        verify(mock).visit(rootNode);
        verifyNoMoreInteractions(mock);
    }
}
