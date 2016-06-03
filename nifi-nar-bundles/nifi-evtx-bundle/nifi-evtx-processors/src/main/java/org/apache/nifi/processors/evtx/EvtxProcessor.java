package org.apache.nifi.processors.evtx;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.MediaType;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processors.evtx.parser.*;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by brosander on 5/24/16.
 */
public class EvtxProcessor extends AbstractProcessor {
    public static final String RECORD = "Record";
    public static final String CHUNK = "Chunk";
    public static final String FILE = "File";
    public static final String EVENTS = "Events";
    public static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    public static final String EVTX = ".evtx";
    public static final String UNABLE_TO_PROCESS_DUE_TO = "Unable to process {} due to {}";
    public static final String XML_EXTENSION = ".xml";
    static final Relationship REL_SUCCESS = new Relationship.Builder().name("success").build();
    static final Relationship REL_FAILURE = new Relationship.Builder().name("failure").build();
    static final Relationship REL_BAD_CHUNK = new Relationship.Builder().name("bad chunk").build();
    static final Relationship REL_ORIGINAL = new Relationship.Builder().name("original").build();
    public static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(REL_SUCCESS, REL_FAILURE, REL_ORIGINAL, REL_BAD_CHUNK)));
    private static final PropertyDescriptor GRANULARITY = new PropertyDescriptor.Builder().name("Granularity").description("Output flow file for each Record, Chunk, or File encountered in the event log").allowableValues(RECORD, CHUNK, FILE).build();
    public static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(GRANULARITY));

    @VisibleForTesting
    static XMLStreamWriter createWriter(OutputStream outputStream) throws XMLStreamException {
        XMLStreamWriter xmlStreamWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(outputStream, "UTF-8");
        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement(EVENTS);
        return xmlStreamWriter;
    }

    @VisibleForTesting
    static void close(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.close();
    }

    @VisibleForTesting
    static String getName(String basename, Object chunkNumber, Object recordNumber, String extension) {
        StringBuilder stringBuilder = new StringBuilder(basename);
        if (chunkNumber != null) {
            stringBuilder.append("-chunk");
            stringBuilder.append(chunkNumber);
        }
        if (recordNumber != null) {
            stringBuilder.append("-record");
            stringBuilder.append(recordNumber);
        }
        stringBuilder.append(extension);
        return stringBuilder.toString();
    }

    @VisibleForTesting
    static String getBasename(FlowFile flowFile, ComponentLog logger) {
        String basename = flowFile.getAttribute(CoreAttributes.FILENAME.key());
        if (basename.endsWith(EVTX)) {
            return basename.substring(0, basename.length() - EVTX.length());
        } else {
            logger.warn("Trying to parse file without .evtx extension {} from flowfile {}", new Object[]{basename, flowFile});
            return basename;
        }
    }

    @VisibleForTesting
    static void processResult(ProcessSession session, ComponentLog logger, FlowFile updated, Exception exception, String basename, ChunkHeader chunkHeader, Record record) {
        Integer chunkNum = chunkHeader == null ? null : chunkHeader.getChunkNumber();
        UnsignedLong recordNum = record == null ? null : record.getRecordNum();
        String name = getName(basename, chunkNum, recordNum, XML_EXTENSION);
        updated = session.putAttribute(updated, CoreAttributes.FILENAME.key(), name);
        updated = session.putAttribute(updated, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_XML_UTF_8.toString());
        if (exception == null) {
            session.transfer(updated, REL_SUCCESS);
        } else {
            logger.error(UNABLE_TO_PROCESS_DUE_TO, new Object[]{name, exception}, exception);
            session.transfer(updated, REL_FAILURE);
        }
    }

    @VisibleForTesting
    public void handleMalformedChunkException(FlowFile original, ProcessSession processSession, String basename, MalformedChunkException malformedChunkException) {
        FlowFile flowFile = processSession.create(original);
        flowFile = processSession.putAttribute(flowFile, CoreAttributes.FILENAME.key(), getName(basename, malformedChunkException.getChunkNum(), null, ".evtx"));
        flowFile = processSession.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), MediaType.APPLICATION_BINARY.toString());
        flowFile = processSession.write(flowFile, out -> out.write(malformedChunkException.getBadChunk()));
        processSession.transfer(flowFile, REL_BAD_CHUNK);
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        ComponentLog logger = getLogger();
        final FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        String basename = getBasename(flowFile, logger);
        String granularity = context.getProperty(GRANULARITY).getValue();
        if (FILE.equals(granularity)) {
            FlowFile original = session.clone(flowFile);
            AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
            FlowFile updated = session.write(flowFile, (in, out) -> {
                FileHeader fileHeader = new FileHeader(in);
                try {
                    XMLStreamWriter writer = createWriter(out);
                    try {
                        while (fileHeader.hasNext()) {
                            try {
                                ChunkHeader chunkHeader = fileHeader.next();
                                while (chunkHeader.hasNext() && exceptionReference.get() == null) {
                                    new XmlBxmlNodeVisitor(writer, chunkHeader.next().getRootNode());
                                }
                            } catch (MalformedChunkException e) {
                                handleMalformedChunkException(original, session, basename, e);
                            }
                        }
                    } finally {
                        close(writer);
                    }
                } catch (XMLStreamException e) {
                    exceptionReference.set(e);
                }
            });
            session.transfer(original, REL_ORIGINAL);
            processResult(session, logger, updated, exceptionReference.get(), basename, null, null);
        } else {
            session.read(flowFile, in -> {
                FileHeader fileHeader = new FileHeader(in);
                if (RECORD.equals(granularity)) {
                    while (fileHeader.hasNext()) {
                        try {
                            ChunkHeader chunkHeader = fileHeader.next();
                            while (chunkHeader.hasNext()) {
                                FlowFile updated = session.create(flowFile);
                                AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                                Record record = chunkHeader.next();
                                updated = session.write(updated, out -> {
                                    try {
                                        XMLStreamWriter writer = createWriter(out);
                                        try {
                                            new XmlBxmlNodeVisitor(writer, record.getRootNode());
                                        } catch (IOException e) {
                                            exceptionReference.set(e);
                                        } finally {
                                            close(writer);
                                        }
                                    } catch (XMLStreamException e) {
                                        exceptionReference.set(e);
                                    }
                                });
                                processResult(session, logger, updated, exceptionReference.get(), basename, chunkHeader, record);
                            }
                        } catch (MalformedChunkException e) {
                            handleMalformedChunkException(flowFile, session, basename, e);
                        }
                    }
                } else if (CHUNK.equals(granularity)) {
                    while (fileHeader.hasNext()) {
                        try {
                            ChunkHeader chunkHeader = fileHeader.next();
                            FlowFile updated = session.create(flowFile);
                            AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                            updated = session.write(updated, out -> {
                                try {
                                    XMLStreamWriter writer = createWriter(out);
                                    try {
                                        while (chunkHeader.hasNext()) {
                                            try {
                                                new XmlBxmlNodeVisitor(writer, chunkHeader.next().getRootNode());
                                            } catch (IOException e) {
                                                exceptionReference.set(e);
                                                break;
                                            }
                                        }
                                    } finally {
                                        close(writer);
                                    }
                                } catch (XMLStreamException e) {
                                    exceptionReference.set(e);
                                }
                            });
                            processResult(session, logger, updated, exceptionReference.get(), basename, chunkHeader, null);
                        } catch (MalformedChunkException e) {
                            handleMalformedChunkException(flowFile, session, basename, e);
                        }
                    }
                }
            });
            session.transfer(flowFile, REL_ORIGINAL);
        }
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }
}
