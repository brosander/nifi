package org.apache.nifi.processors.evtx;

import com.google.common.base.Charsets;
import com.google.common.net.MediaType;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.FileHeader;
import org.apache.nifi.processors.evtx.parser.FileHeaderFactory;
import org.apache.nifi.processors.evtx.parser.MalformedChunkException;
import org.apache.nifi.processors.evtx.parser.Record;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by brosander on 6/2/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class ParseEvtxTest {
    @Mock
    FileHeaderFactory fileHeaderFactory;

    @Mock
    ParseEvtx.MalformedChunkHandler malformedChunkHandler;

    @Mock
    ParseEvtx.RootNodeHandler rootNodeHandler;

    @Mock
    ParseEvtx.XMLStreamWriterFactory xmlStreamWriterFactory;

    @Mock
    ParseEvtx.ResultProcessor resultProcessor;

    @Mock
    InputStream in;

    @Mock
    OutputStream out;

    @Mock
    FileHeader fileHeader;

    @Mock
    XMLStreamWriter xmlStreamWriter;

    ParseEvtx parseEvtx;

    @Before
    public void setup() throws XMLStreamException, IOException {
        parseEvtx = new ParseEvtx(fileHeaderFactory, malformedChunkHandler, rootNodeHandler, xmlStreamWriterFactory, resultProcessor);
        when(xmlStreamWriterFactory.create(out)).thenReturn(xmlStreamWriter);
        when(fileHeaderFactory.create(in)).thenReturn(fileHeader);
    }

    @Test
    public void testCreateCloseWriter() throws XMLStreamException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ParseEvtx.close(ParseEvtx.createWriter(outputStream));
        String toString = Charsets.UTF_8.decode(ByteBuffer.wrap(outputStream.toByteArray())).toString();
        assertTrue(toString.endsWith("<Events></Events>"));
    }

    @Test
    public void testGetNameFile() {
        String basename = "basename";
        assertEquals(basename + ".xml", ParseEvtx.getName(basename, null, null, ParseEvtx.XML_EXTENSION));
    }

    @Test
    public void testGetNameFileChunk() {
        String basename = "basename";
        assertEquals(basename + "-chunk1.xml", ParseEvtx.getName(basename, 1, null, ParseEvtx.XML_EXTENSION));
    }

    @Test
    public void testGetNameFileChunkRecord() {
        String basename = "basename";
        assertEquals(basename + "-chunk1-record2.xml", ParseEvtx.getName(basename, 1, 2, ParseEvtx.XML_EXTENSION));
    }

    @Test
    public void testGetBasenameEvtxExtension() {
        String basename = "basename";
        FlowFile flowFile = mock(FlowFile.class);
        ComponentLog componentLog = mock(ComponentLog.class);

        when(flowFile.getAttribute(CoreAttributes.FILENAME.key())).thenReturn(basename + ".evtx");

        assertEquals(basename, ParseEvtx.getBasename(flowFile, componentLog));
        verifyNoMoreInteractions(componentLog);
    }

    @Test
    public void testGetBasenameExtension() {
        String basename = "basename.wrongextension";
        FlowFile flowFile = mock(FlowFile.class);
        ComponentLog componentLog = mock(ComponentLog.class);

        when(flowFile.getAttribute(CoreAttributes.FILENAME.key())).thenReturn(basename);

        assertEquals(basename, ParseEvtx.getBasename(flowFile, componentLog));
        verify(componentLog).warn(anyString(), isA(Object[].class));
    }

    @Test
    public void testProcessResultFile() {
        ProcessSession processSession = mock(ProcessSession.class);
        ComponentLog componentLog = mock(ComponentLog.class);
        FlowFile flowFile = mock(FlowFile.class);
        Exception exception = null;
        String basename = "basename";
        ChunkHeader chunkHeader = null;
        Record record = null;

        when(processSession.putAttribute(eq(flowFile), anyString(), anyString())).thenReturn(flowFile);

        ParseEvtx.processResult(processSession, componentLog, flowFile, exception, basename, chunkHeader, record);
        verify(processSession).putAttribute(flowFile, CoreAttributes.FILENAME.key(), ParseEvtx.getName(basename, chunkHeader, record, ParseEvtx.XML_EXTENSION));
        verify(processSession).putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        verify(processSession).transfer(flowFile, ParseEvtx.REL_SUCCESS);
        verifyNoMoreInteractions(componentLog);
    }

    @Test
    public void testProcessResultFileChunkRecord() {
        ProcessSession processSession = mock(ProcessSession.class);
        ComponentLog componentLog = mock(ComponentLog.class);
        FlowFile flowFile = mock(FlowFile.class);
        Exception exception = new Exception();
        String basename = "basename";
        ChunkHeader chunkHeader = mock(ChunkHeader.class);
        Record record = mock(Record.class);

        when(processSession.putAttribute(eq(flowFile), anyString(), anyString())).thenReturn(flowFile);
        when(chunkHeader.getChunkNumber()).thenReturn(2);
        when(record.getRecordNum()).thenReturn(UnsignedLong.valueOf(22));

        ParseEvtx.processResult(processSession, componentLog, flowFile, exception, basename, chunkHeader, record);
        verify(processSession).putAttribute(flowFile, CoreAttributes.FILENAME.key(), ParseEvtx.getName(basename, chunkHeader.getChunkNumber(), record.getRecordNum(), ParseEvtx.XML_EXTENSION));
        verify(processSession).putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        verify(processSession).transfer(flowFile, ParseEvtx.REL_FAILURE);
        verify(componentLog).error(eq(ParseEvtx.UNABLE_TO_PROCESS_DUE_TO), any(Object[].class), eq(exception));
    }

    @Test
    public void testGetRelationships() {
        assertEquals(ParseEvtx.RELATIONSHIPS, parseEvtx.getRelationships());
    }

    @Test
    public void testGetSupportedPropertyDescriptors() {
        assertEquals(ParseEvtx.PROPERTY_DESCRIPTORS, parseEvtx.getSupportedPropertyDescriptors());
    }

    @Test
    public void testHandleMalformedChunkException() {
        String basename = "basename";
        int chunkNum = 5;
        int offset = 10001;
        byte[] badChunk = {8};
        MalformedChunkException malformedChunkException = new MalformedChunkException("Test", null, offset, chunkNum, badChunk);
        FlowFile original = mock(FlowFile.class);
        FlowFile updated1 = mock(FlowFile.class);
        FlowFile updated2 = mock(FlowFile.class);
        FlowFile updated3 = mock(FlowFile.class);
        FlowFile updated4 = mock(FlowFile.class);
        ProcessSession session = mock(ProcessSession.class);

        when(session.create(original)).thenReturn(updated1);
        when(session.putAttribute(updated1, CoreAttributes.FILENAME.key(), parseEvtx.getName(basename, 5, null, ParseEvtx.EVTX_EXTENSION))).thenReturn(updated2);
        when(session.putAttribute(updated2, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_BINARY.toString())).thenReturn(updated3);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(session.write(eq(updated3), any(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(out);
            return updated4;
        });

        ParseEvtx.handleMalformedChunkException(original, session, basename, malformedChunkException);

        verify(session).transfer(updated4, ParseEvtx.REL_BAD_CHUNK);
        assertArrayEquals(badChunk, out.toByteArray());
    }

    @Test
    public void testProcessFileGranularity() throws IOException, MalformedChunkException, XMLStreamException {
        String basename = "basename";
        int chunkNum = 5;
        int offset = 10001;
        byte[] badChunk = {8};

        ChunkHeader chunkHeader1 = mock(ChunkHeader.class);
        ChunkHeader chunkHeader2 = mock(ChunkHeader.class);
        Record record1 = mock(Record.class);
        Record record2 = mock(Record.class);
        Record record3 = mock(Record.class);
        RootNode rootNode1 = mock(RootNode.class);
        RootNode rootNode2 = mock(RootNode.class);
        RootNode rootNode3 = mock(RootNode.class);
        ProcessSession session = mock(ProcessSession.class);
        FlowFile flowFile = mock(FlowFile.class);
        AtomicReference<Exception> reference = new AtomicReference<>();
        MalformedChunkException malformedChunkException = new MalformedChunkException("Test", null, offset, chunkNum, badChunk);

        when(record1.getRootNode()).thenReturn(rootNode1);
        when(record2.getRootNode()).thenReturn(rootNode2);
        when(record3.getRootNode()).thenReturn(rootNode3);

        when(fileHeader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(fileHeader.next()).thenThrow(malformedChunkException).thenReturn(chunkHeader1).thenReturn(chunkHeader2).thenReturn(null);

        when(chunkHeader1.hasNext()).thenReturn(true).thenReturn(false);
        when(chunkHeader1.next()).thenReturn(record1).thenReturn(null);

        when(chunkHeader2.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(chunkHeader2.next()).thenReturn(record2).thenReturn(record3).thenReturn(null);

        parseEvtx.processFileGranularity(session, flowFile, basename, reference, in, out);

        verify(malformedChunkHandler).handle(flowFile, session, basename, malformedChunkException);
        verify(rootNodeHandler).handle(xmlStreamWriter, rootNode1);
        verify(rootNodeHandler).handle(xmlStreamWriter, rootNode2);
        verify(rootNodeHandler).handle(xmlStreamWriter, rootNode3);
        verify(xmlStreamWriter).close();
    }

    @Test
    public void testProcessChunkGranularity() throws IOException, MalformedChunkException, XMLStreamException {
        String basename = "basename";
        int chunkNum = 5;
        int offset = 10001;
        byte[] badChunk = {8};

        ComponentLog componentLog = mock(ComponentLog.class);
        ChunkHeader chunkHeader1 = mock(ChunkHeader.class);
        ChunkHeader chunkHeader2 = mock(ChunkHeader.class);
        Record record1 = mock(Record.class);
        Record record2 = mock(Record.class);
        Record record3 = mock(Record.class);
        RootNode rootNode1 = mock(RootNode.class);
        RootNode rootNode2 = mock(RootNode.class);
        RootNode rootNode3 = mock(RootNode.class);
        ProcessSession session = mock(ProcessSession.class);
        FlowFile flowFile = mock(FlowFile.class);
        FlowFile created1 = mock(FlowFile.class);
        FlowFile updated1 = mock(FlowFile.class);
        FlowFile created2 = mock(FlowFile.class);
        FlowFile updated2 = mock(FlowFile.class);
        MalformedChunkException malformedChunkException = new MalformedChunkException("Test", null, offset, chunkNum, badChunk);
        OutputStream out2 = mock(OutputStream.class);
        XMLStreamWriter xmlStreamWriter2 = mock(XMLStreamWriter.class);

        when(session.create(flowFile)).thenReturn(created1).thenReturn(created2).thenReturn(null);

        when(session.write(eq(created1), any(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(out);
            return updated1;
        });

        when(session.write(eq(created2), any(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(out2);
            return updated2;
        });

        when(record1.getRootNode()).thenReturn(rootNode1);
        when(record2.getRootNode()).thenReturn(rootNode2);
        when(record3.getRootNode()).thenReturn(rootNode3);

        when(xmlStreamWriterFactory.create(out2)).thenReturn(xmlStreamWriter2);

        when(fileHeader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(fileHeader.next()).thenThrow(malformedChunkException).thenReturn(chunkHeader1).thenReturn(chunkHeader2).thenReturn(null);

        when(chunkHeader1.hasNext()).thenReturn(true).thenReturn(false);
        when(chunkHeader1.next()).thenReturn(record1).thenReturn(null);

        when(chunkHeader2.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(chunkHeader2.next()).thenReturn(record2).thenReturn(record3).thenReturn(null);

        parseEvtx.processChunkGranularity(session, componentLog, flowFile, basename, in);

        verify(malformedChunkHandler).handle(flowFile, session, basename, malformedChunkException);
        verify(rootNodeHandler).handle(xmlStreamWriter, rootNode1);
        verify(rootNodeHandler).handle(xmlStreamWriter2, rootNode2);
        verify(rootNodeHandler).handle(xmlStreamWriter2, rootNode3);
        verify(xmlStreamWriter).close();
    }

    @Test
    public void testProcess1RecordGranularity() throws IOException, MalformedChunkException, XMLStreamException {
        String basename = "basename";
        int chunkNum = 5;
        int offset = 10001;
        byte[] badChunk = {8};

        ComponentLog componentLog = mock(ComponentLog.class);
        ChunkHeader chunkHeader1 = mock(ChunkHeader.class);
        ChunkHeader chunkHeader2 = mock(ChunkHeader.class);
        Record record1 = mock(Record.class);
        Record record2 = mock(Record.class);
        Record record3 = mock(Record.class);
        RootNode rootNode1 = mock(RootNode.class);
        RootNode rootNode2 = mock(RootNode.class);
        RootNode rootNode3 = mock(RootNode.class);
        ProcessSession session = mock(ProcessSession.class);
        FlowFile flowFile = mock(FlowFile.class);
        FlowFile created1 = mock(FlowFile.class);
        FlowFile updated1 = mock(FlowFile.class);
        FlowFile created2 = mock(FlowFile.class);
        FlowFile updated2 = mock(FlowFile.class);
        FlowFile created3 = mock(FlowFile.class);
        FlowFile updated3 = mock(FlowFile.class);
        MalformedChunkException malformedChunkException = new MalformedChunkException("Test", null, offset, chunkNum, badChunk);
        OutputStream out2 = mock(OutputStream.class);
        OutputStream out3 = mock(OutputStream.class);
        XMLStreamWriter xmlStreamWriter2 = mock(XMLStreamWriter.class);
        XMLStreamWriter xmlStreamWriter3 = mock(XMLStreamWriter.class);

        when(session.create(flowFile)).thenReturn(created1).thenReturn(created2).thenReturn(created3).thenReturn(null);

        when(session.write(eq(created1), any(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(out);
            return updated1;
        });

        when(session.write(eq(created2), any(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(out2);
            return updated2;
        });

        when(session.write(eq(created3), any(OutputStreamCallback.class))).thenAnswer(invocation -> {
            ((OutputStreamCallback) invocation.getArguments()[1]).process(out3);
            return updated3;
        });

        when(record1.getRootNode()).thenReturn(rootNode1);
        when(record2.getRootNode()).thenReturn(rootNode2);
        when(record3.getRootNode()).thenReturn(rootNode3);

        when(xmlStreamWriterFactory.create(out2)).thenReturn(xmlStreamWriter2);
        when(xmlStreamWriterFactory.create(out3)).thenReturn(xmlStreamWriter3);

        when(fileHeader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(fileHeader.next()).thenThrow(malformedChunkException).thenReturn(chunkHeader1).thenReturn(chunkHeader2).thenReturn(null);

        when(chunkHeader1.hasNext()).thenReturn(true).thenReturn(false);
        when(chunkHeader1.next()).thenReturn(record1).thenReturn(null);

        when(chunkHeader2.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(chunkHeader2.next()).thenReturn(record2).thenReturn(record3).thenReturn(null);

        parseEvtx.processRecordGranularity(session, componentLog, flowFile, basename, in);

        verify(malformedChunkHandler).handle(flowFile, session, basename, malformedChunkException);
        verify(rootNodeHandler).handle(xmlStreamWriter, rootNode1);
        verify(rootNodeHandler).handle(xmlStreamWriter2, rootNode2);
        verify(rootNodeHandler).handle(xmlStreamWriter3, rootNode3);
        verify(xmlStreamWriter).close();
    }
}
