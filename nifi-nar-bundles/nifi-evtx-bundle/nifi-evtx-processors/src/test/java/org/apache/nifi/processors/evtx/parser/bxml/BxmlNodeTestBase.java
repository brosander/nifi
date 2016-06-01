package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.TestBinaryReaderBuilder;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

/**
 * Created by brosander on 6/1/16.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BxmlNodeTestBase {
    public TestBinaryReaderBuilder testBinaryReaderBuilder;

    @Mock
    public ChunkHeader chunkHeader;

    @Mock
    public BxmlNode parent;

    @Before
    public void setup() throws IOException {
        testBinaryReaderBuilder = new TestBinaryReaderBuilder();
    }
}
