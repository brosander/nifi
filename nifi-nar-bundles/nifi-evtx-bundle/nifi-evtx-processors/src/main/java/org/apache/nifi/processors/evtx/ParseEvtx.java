package org.apache.nifi.processors.evtx;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.MediaType;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.FileHeader;
import org.apache.nifi.processors.evtx.parser.FileHeaderFactory;
import org.apache.nifi.processors.evtx.parser.MalformedChunkException;
import org.apache.nifi.processors.evtx.parser.Record;
import org.apache.nifi.processors.evtx.parser.XmlBxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by brosander on 5/24/16.
 */
@SideEffectFree
@EventDriven
@SupportsBatching
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({"logs", "windows", "event", "evtx", "message", "file"})
@CapabilityDescription("Parses the contents of a Windows Event Log file (evtx) and writes the resulting xml to the FlowFile")
public class ParseEvtx extends AbstractProcessor {
    public static final String RECORD = "Record";
    public static final String CHUNK = "Chunk";
    public static final String FILE = "File";
    public static final String EVENTS = "Events";
    public static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    public static final String EVTX_EXTENSION = ".evtx";
    public static final String UNABLE_TO_PROCESS_DUE_TO = "Unable to process {} due to {}";
    public static final String XML_EXTENSION = ".xml";

    @VisibleForTesting
    static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Any FlowFile that was successfully converted from evtx to xml")
            .build();

    @VisibleForTesting
    static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Any FlowFile that encountered an exception during conversion will be transferred to this relationship with as much parsing as possible done")
            .build();

    @VisibleForTesting
    static final Relationship REL_BAD_CHUNK = new Relationship.Builder()
            .name("bad chunk")
            .description("Any bad chunks of records will be transferred to this relationship in their original binary form")
            .build();

    @VisibleForTesting
    static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("The unmodified input FlowFile will be transferred to this relationship")
            .build();

    @VisibleForTesting
    static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(REL_SUCCESS, REL_FAILURE, REL_ORIGINAL, REL_BAD_CHUNK)));

    @VisibleForTesting
    static final PropertyDescriptor GRANULARITY = new PropertyDescriptor.Builder().required(true)
            .name("Granularity")
            .description("Output flow file for each Record, Chunk, or File encountered in the event log")
            .allowableValues(RECORD, CHUNK, FILE)
            .build();

    @VisibleForTesting
    static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(GRANULARITY));

    private final FileHeaderFactory fileHeaderFactory;
    private final MalformedChunkHandler malformedChunkHandler;
    private final RootNodeHandler rootNodeHandler;
    private final XMLStreamWriterFactory xmlStreamWriterFactory;
    private final ResultProcessor resultProcessor;

    public ParseEvtx() {
        this(FileHeader::new, ParseEvtx::handleMalformedChunkException, XmlBxmlNodeVisitor::new,
                ParseEvtx::createWriter, ParseEvtx::processResult);
    }

    @VisibleForTesting
    ParseEvtx(FileHeaderFactory fileHeaderFactory, MalformedChunkHandler malformedChunkHandler, RootNodeHandler rootNodeHandler,
              XMLStreamWriterFactory xmlStreamWriterFactory, ResultProcessor resultProcessor) {
        this.fileHeaderFactory = fileHeaderFactory;
        this.malformedChunkHandler = malformedChunkHandler;
        this.rootNodeHandler = rootNodeHandler;
        this.xmlStreamWriterFactory = xmlStreamWriterFactory;
        this.resultProcessor = resultProcessor;
    }

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
        if (basename.endsWith(EVTX_EXTENSION)) {
            return basename.substring(0, basename.length() - EVTX_EXTENSION.length());
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
    static void handleMalformedChunkException(FlowFile original, ProcessSession processSession, String basename, MalformedChunkException malformedChunkException) {
        FlowFile flowFile = processSession.create(original);
        flowFile = processSession.putAttribute(flowFile, CoreAttributes.FILENAME.key(), getName(basename, malformedChunkException.getChunkNum(), null, EVTX_EXTENSION));
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
                processFileGranularity(session, original, basename, exceptionReference, in, out);
            });
            session.transfer(original, REL_ORIGINAL);
            resultProcessor.process(session, logger, updated, exceptionReference.get(), basename, null, null);
        } else {
            session.read(flowFile, in -> {
                if (RECORD.equals(granularity)) {
                    processRecordGranularity(session, logger, flowFile, basename, in);
                } else if (CHUNK.equals(granularity)) {
                    processChunkGranularity(session, logger, flowFile, basename, in);
                }
            });
            session.transfer(flowFile, REL_ORIGINAL);
        }
    }

    @VisibleForTesting
    void processFileGranularity(ProcessSession session, FlowFile original, String basename, AtomicReference<Exception> exceptionReference, InputStream in, OutputStream out) throws IOException {
        FileHeader fileHeader = fileHeaderFactory.create(in);
        try {
            XMLStreamWriter writer = xmlStreamWriterFactory.create(out);
            try {
                while (fileHeader.hasNext()) {
                    try {
                        ChunkHeader chunkHeader = fileHeader.next();
                        while (chunkHeader.hasNext() && exceptionReference.get() == null) {
                            rootNodeHandler.handle(writer, chunkHeader.next().getRootNode());
                        }
                    } catch (MalformedChunkException e) {
                        malformedChunkHandler.handle(original, session, basename, e);
                    }
                }
            } finally {
                close(writer);
            }
        } catch (IOException e) {
            exceptionReference.set(e);
        } catch (XMLStreamException e) {
            exceptionReference.set(e);
        }
    }

    @VisibleForTesting
    void processChunkGranularity(ProcessSession session, ComponentLog logger, FlowFile flowFile, String basename, InputStream in) throws IOException {
        FileHeader fileHeader = fileHeaderFactory.create(in);
        while (fileHeader.hasNext()) {
            try {
                ChunkHeader chunkHeader = fileHeader.next();
                FlowFile updated = session.create(flowFile);
                AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                updated = session.write(updated, out -> {
                    try {
                        XMLStreamWriter writer = xmlStreamWriterFactory.create(out);
                        try {
                            while (chunkHeader.hasNext()) {
                                try {
                                    rootNodeHandler.handle(writer, chunkHeader.next().getRootNode());
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
                resultProcessor.process(session, logger, updated, exceptionReference.get(), basename, chunkHeader, null);
            } catch (MalformedChunkException e) {
                malformedChunkHandler.handle(flowFile, session, basename, e);
            }
        }
    }

    @VisibleForTesting
    void processRecordGranularity(ProcessSession session, ComponentLog logger, FlowFile flowFile, String basename, InputStream in) throws IOException {
        FileHeader fileHeader = fileHeaderFactory.create(in);
        while (fileHeader.hasNext()) {
            try {
                ChunkHeader chunkHeader = fileHeader.next();
                while (chunkHeader.hasNext()) {
                    FlowFile updated = session.create(flowFile);
                    AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                    Record record = chunkHeader.next();
                    updated = session.write(updated, out -> {
                        try {
                            XMLStreamWriter writer = xmlStreamWriterFactory.create(out);
                            try {
                                rootNodeHandler.handle(writer, record.getRootNode());
                            } catch (IOException e) {
                                exceptionReference.set(e);
                            } finally {
                                close(writer);
                            }
                        } catch (XMLStreamException e) {
                            exceptionReference.set(e);
                        }
                    });
                    resultProcessor.process(session, logger, updated, exceptionReference.get(), basename, chunkHeader, record);
                }
            } catch (MalformedChunkException e) {
                malformedChunkHandler.handle(flowFile, session, basename, e);
            }
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

    @VisibleForTesting
    interface MalformedChunkHandler {
        void handle(FlowFile original, ProcessSession processSession, String basename, MalformedChunkException malformedChunkException);
    }

    @VisibleForTesting
    interface RootNodeHandler {
        void handle(XMLStreamWriter writer, RootNode rootNode) throws IOException;
    }

    @VisibleForTesting
    interface XMLStreamWriterFactory {
        XMLStreamWriter create(OutputStream outputStream) throws XMLStreamException;
    }

    @VisibleForTesting
    interface ResultProcessor {
        void process(ProcessSession session, ComponentLog logger, FlowFile updated, Exception exception, String basename, ChunkHeader chunkHeader, Record record);
    }
}
