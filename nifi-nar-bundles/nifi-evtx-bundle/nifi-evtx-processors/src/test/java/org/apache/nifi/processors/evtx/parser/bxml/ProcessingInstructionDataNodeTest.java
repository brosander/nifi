package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by brosander on 6/2/16.
 */
public class ProcessingInstructionDataNodeTest extends BxmlNodeWithTokenTestBase {
    private String data = "testData";
    private ProcessingInstructionDataNode processingInstructionDataNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.putWord(data.length());
        testBinaryReaderBuilder.putWString(data);
        processingInstructionDataNode = new ProcessingInstructionDataNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Test
    public void testInit() {
        assertEquals(getToken(), processingInstructionDataNode.getToken());
        assertEquals(data + "?>", processingInstructionDataNode.getValue());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        processingInstructionDataNode.accept(mock);
        verify(mock).visit(processingInstructionDataNode);
        verifyNoMoreInteractions(mock);
    }

    @Test
    public void testNoData() throws IOException {
        super.setup();
        testBinaryReaderBuilder.putDWord(0);
        processingInstructionDataNode = new ProcessingInstructionDataNode(testBinaryReaderBuilder.build(), chunkHeader, parent);

        assertEquals(getToken(), processingInstructionDataNode.getToken());
        assertEquals("?>", processingInstructionDataNode.getValue());
    }

    @Override
    protected byte getToken() {
        return BxmlNode.PROCESSING_INSTRUCTION_DATA_TOKEN;
    }
}
