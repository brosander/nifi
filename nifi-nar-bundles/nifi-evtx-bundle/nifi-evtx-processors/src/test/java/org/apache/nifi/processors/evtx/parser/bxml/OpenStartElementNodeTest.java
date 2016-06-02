package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.TestBinaryReaderBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by brosander on 6/2/16.
 */
public class OpenStartElementNodeTest extends BxmlNodeWithTokenTestBase {
    private int unknown = 24;
    private int size = 2444;
    private int stringOffset = 0;
    private String tagName = "tagName";
    private OpenStartElementNode openStartElementNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.putWord(unknown);
        testBinaryReaderBuilder.putDWord(size);
        testBinaryReaderBuilder.putDWord(stringOffset);

        testBinaryReaderBuilder.put((byte) BxmlNode.CLOSE_EMPTY_ELEMENT_TOKEN);
        when(chunkHeader.getString(stringOffset)).thenReturn(tagName);
        openStartElementNode = new OpenStartElementNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Test
    public void testInit() {
        assertEquals(tagName, openStartElementNode.getTagName());
        List<BxmlNode> children = openStartElementNode.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof CloseEmptyElementNode);
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        openStartElementNode.accept(mock);
        verify(mock).visit(openStartElementNode);
        verifyNoMoreInteractions(mock);
    }

    @Test
    public void testWithFlagAndEmbeddedNameStringNode() throws IOException {
        byte token = (byte)(0x04 << 4 | getToken());
        stringOffset = 5;
        tagName = "teststring";
        testBinaryReaderBuilder = new TestBinaryReaderBuilder();
        testBinaryReaderBuilder.put(token);
        testBinaryReaderBuilder.putWord(unknown);
        testBinaryReaderBuilder.putDWord(size);
        testBinaryReaderBuilder.putDWord(stringOffset);

        testBinaryReaderBuilder.putDWord(0);
        testBinaryReaderBuilder.putWord(0);
        testBinaryReaderBuilder.putWord(tagName.length());
        testBinaryReaderBuilder.putWString(tagName);
        testBinaryReaderBuilder.putWord(0);
        testBinaryReaderBuilder.put(new byte[5]);

        testBinaryReaderBuilder.put((byte) BxmlNode.CLOSE_EMPTY_ELEMENT_TOKEN);

        BinaryReader binaryReader = testBinaryReaderBuilder.build();
        NameStringNode nameStringNode = mock(NameStringNode.class);
        when(nameStringNode.getString()).thenReturn(tagName);
        when(chunkHeader.addNameStringNode(stringOffset, binaryReader)).thenAnswer(invocation -> new NameStringNode(binaryReader, chunkHeader));
        openStartElementNode = new OpenStartElementNode(binaryReader, chunkHeader, parent);

        assertEquals(getToken(), openStartElementNode.getToken() & 0x0F);
        assertTrue((openStartElementNode.getFlags() & 0x04) > 0);
        assertEquals(tagName, openStartElementNode.getTagName());
    }

    @Override
    protected byte getToken() {
        return BxmlNode.OPEN_START_ELEMENT_TOKEN;
    }
}
