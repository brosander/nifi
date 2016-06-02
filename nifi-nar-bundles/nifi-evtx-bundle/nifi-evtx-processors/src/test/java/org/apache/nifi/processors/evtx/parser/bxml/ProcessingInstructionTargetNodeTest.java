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
public class ProcessingInstructionTargetNodeTest extends BxmlNodeWithTokenAndStringTestBase {
    private String instruction = "testInstruction";
    private ProcessingInstructionTargetNode processingInstructionTargetNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        processingInstructionTargetNode = new ProcessingInstructionTargetNode(testBinaryReaderBuilder.build(), chunkHeader, parent);
    }

    @Override
    protected String getString() {
        return instruction;
    }

    @Test
    public void testInit() {
        assertEquals(getToken(), processingInstructionTargetNode.getToken());
        assertEquals("<?" + instruction, processingInstructionTargetNode.getValue());
    }

    @Test
    public void testVisitor() throws IOException {
        BxmlNodeVisitor mock = mock(BxmlNodeVisitor.class);
        processingInstructionTargetNode.accept(mock);
        verify(mock).visit(processingInstructionTargetNode);
        verifyNoMoreInteractions(mock);
    }

    @Override
    protected byte getToken() {
        return BxmlNode.PROCESSING_INSTRUCTION_TARGET_TOKEN;
    }
}
