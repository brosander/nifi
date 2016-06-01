package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.TestBinaryReaderBuilder;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by brosander on 6/1/16.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class VariantTypeNodeTestBase {
    TestBinaryReaderBuilder testBinaryReaderBuilder;

    @Mock
    ChunkHeader chunkHeader;

    @Mock
    BxmlNode parent;

    @Before
    public void setup() {
        testBinaryReaderBuilder = new TestBinaryReaderBuilder();
    }
}
