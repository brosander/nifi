package org.apache.nifi.processors.evtx;

import com.google.common.base.Charsets;
import com.google.common.net.MediaType;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.Record;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by brosander on 6/2/16.
 */
public class EvtxProcessorTest {
    @Test
    public void testCreateCloseWriter() throws XMLStreamException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EvtxProcessor.close(EvtxProcessor.createWriter(outputStream));
        String toString = Charsets.UTF_8.decode(ByteBuffer.wrap(outputStream.toByteArray())).toString();
        assertTrue(toString.endsWith("<Events></Events>"));
    }

    @Test
    public void testGetNameFile() {
        String basename = "basename";
        assertEquals(basename + ".xml", EvtxProcessor.getName(basename, null, null, EvtxProcessor.XML_EXTENSION));
    }

    @Test
    public void testGetNameFileChunk() {
        String basename = "basename";
        assertEquals(basename + "-chunk1.xml", EvtxProcessor.getName(basename, 1, null, EvtxProcessor.XML_EXTENSION));
    }

    @Test
    public void testGetNameFileChunkRecord() {
        String basename = "basename";
        assertEquals(basename + "-chunk1-record2.xml", EvtxProcessor.getName(basename, 1, 2, EvtxProcessor.XML_EXTENSION));
    }

    @Test
    public void testGetBasenameEvtxExtension() {
        String basename = "basename";
        FlowFile flowFile = mock(FlowFile.class);
        ComponentLog componentLog = mock(ComponentLog.class);

        when(flowFile.getAttribute(CoreAttributes.FILENAME.key())).thenReturn(basename + ".evtx");

        assertEquals(basename, EvtxProcessor.getBasename(flowFile, componentLog));
        verifyNoMoreInteractions(componentLog);
    }

    @Test
    public void testGetBasenameExtension() {
        String basename = "basename.wrongextension";
        FlowFile flowFile = mock(FlowFile.class);
        ComponentLog componentLog = mock(ComponentLog.class);

        when(flowFile.getAttribute(CoreAttributes.FILENAME.key())).thenReturn(basename);

        assertEquals(basename, EvtxProcessor.getBasename(flowFile, componentLog));
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

        EvtxProcessor.processResult(processSession, componentLog, flowFile, exception, basename, chunkHeader, record);
        verify(processSession).putAttribute(flowFile, CoreAttributes.FILENAME.key(), EvtxProcessor.getName(basename, chunkHeader, record, EvtxProcessor.XML_EXTENSION));
        verify(processSession).putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        verify(processSession).transfer(flowFile, EvtxProcessor.REL_SUCCESS);
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

        EvtxProcessor.processResult(processSession, componentLog, flowFile, exception, basename, chunkHeader, record);
        verify(processSession).putAttribute(flowFile, CoreAttributes.FILENAME.key(), EvtxProcessor.getName(basename, chunkHeader.getChunkNumber(), record.getRecordNum(), EvtxProcessor.XML_EXTENSION));
        verify(processSession).putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        verify(processSession).transfer(flowFile, EvtxProcessor.REL_FAILURE);
        verify(componentLog).error(eq(EvtxProcessor.UNABLE_TO_PROCESS_DUE_TO), any(Object[].class), eq(exception));
    }

    @Test
    public void testGetRelationships() {
        assertEquals(EvtxProcessor.RELATIONSHIPS, new EvtxProcessor().getRelationships());
    }

    @Test
    public void testGetSupportedPropertyDescriptors() {
        assertEquals(EvtxProcessor.PROPERTY_DESCRIPTORS, new EvtxProcessor().getSupportedPropertyDescriptors());
    }
}
