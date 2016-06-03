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
public class CDataSectionNodeTest extends BxmlNodeWithTokenTestBase {
    private String content;
    private CDataSectionNode cDataSectionNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        content = "cdata content";
        testBinaryReaderBuilder.putWord(content.length() + 2);
        testBinaryReaderBuilder.putWString(content);
        cDataSectionNode = new CDataSectionNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.C_DATA_SECTION_TOKEN;
    }

    @Test
    public void testInit() {
        assertEquals(content, cDataSectionNode.getCdata());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        cDataSectionNode.accept(mock);
        verify(mock).visit(cDataSectionNode);
        verifyNoMoreInteractions(mock);
    }
}
