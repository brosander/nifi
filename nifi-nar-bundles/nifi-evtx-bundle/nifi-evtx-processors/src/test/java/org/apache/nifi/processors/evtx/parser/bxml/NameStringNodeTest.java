package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by brosander on 6/1/16.
 */
public class NameStringNodeTest extends BxmlNodeTestBase {
    private int nextOffset = 300;
    private int hash = 100;
    private String string = "test string";
    private NameStringNode nameStringNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.putDWord(nextOffset);
        testBinaryReaderBuilder.putWord(hash);
        testBinaryReaderBuilder.putWord(string.length());
        testBinaryReaderBuilder.putWString(string);
        nameStringNode = new NameStringNode(testBinaryReaderBuilder.build(), chunkHeader);
    }

    @Test
    public void testInit() {
        assertEquals(UnsignedInteger.valueOf(nextOffset), nameStringNode.getNextOffset());
        assertEquals(hash, nameStringNode.getHash());
        assertEquals(string, nameStringNode.getString());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        nameStringNode.accept(mock);
        verify(mock).visit(nameStringNode);
        verifyNoMoreInteractions(mock);
    }
}
