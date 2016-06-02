package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.apache.nifi.processors.evtx.parser.bxml.EndOfStreamNode;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by brosander on 6/2/16.
 */
public class BXmlTypeNodeTest extends BxmlNodeTestBase {
    private BXmlTypeNode bXmlTypeNode;

    @Override
    public void setup() throws IOException {
        super.setup();
        testBinaryReaderBuilder.put((byte) BxmlNode.END_OF_STREAM_TOKEN);
        testBinaryReaderBuilder.putDWord(0);
        bXmlTypeNode = new BXmlTypeNode(testBinaryReaderBuilder.build(), chunkHeader, parent, -1);
    }

    @Test
    public void testInit() {
        RootNode rootNode = bXmlTypeNode.getRootNode();
        List<BxmlNode> children = rootNode.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof EndOfStreamNode);
        assertEquals(0, rootNode.getSubstitutions().size());
        assertEquals(rootNode.toString(), bXmlTypeNode.getValue());
    }
}
