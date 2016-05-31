package org.apache.nifi.processors.evtx;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processors.evtx.parser.FileHeader;
import org.apache.nifi.processors.evtx.parser.XmlBxmlNodeVisitor;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
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
    private static final Relationship REL_SUCCESS = new Relationship.Builder().name("success").build();
    private static final Relationship REL_FAILURE = new Relationship.Builder().name("failure").build();
    private static final Relationship REL_ORIGINAL = new Relationship.Builder().name("original").build();
    private static final PropertyDescriptor GRANULARITY = new PropertyDescriptor.Builder().name("Granularity").description("Output flow file for each Record, Chunk, or File encountered in the event log").allowableValues(RECORD, CHUNK, FILE).build();

    private XMLStreamWriter createWriter(OutputStream outputStream) throws XMLStreamException {
        XMLStreamWriter xmlStreamWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(outputStream, "UTF-8");
        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement(EVENTS);
        return xmlStreamWriter;
    }

    private void close(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.close();
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        String granularity = context.getProperty(GRANULARITY).getValue();
        session.read(flowFile, in -> {
            FileHeader fileHeader = new FileHeader(in);
            if (RECORD.equals(granularity)) {
                fileHeader.forEachRemaining(chunkHeader -> chunkHeader.forEachRemaining(record -> {
                    FlowFile updated = session.clone(flowFile);
                    AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                    session.write(updated, out -> {
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
                            e.printStackTrace();
                        }
                    });
                    Exception exception = exceptionReference.get();
                    if (exception != null) {
                        session.transfer(flowFile, REL_SUCCESS);
                    } else {
                        session.transfer(flowFile, REL_FAILURE);
                    }
                }));
            } else if (CHUNK.equals(granularity)) {
                fileHeader.forEachRemaining(chunkHeader -> {
                    FlowFile updated = session.clone(flowFile);
                    AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                    session.write(updated, out -> {
                        try {
                            XMLStreamWriter writer = createWriter(out);
                            try {
                                chunkHeader.forEachRemaining(record -> {
                                    try {
                                        new XmlBxmlNodeVisitor(writer, record.getRootNode());
                                    } catch (IOException e) {
                                        exceptionReference.set(e);
                                    }
                                });
                            } finally {
                                close(writer);
                            }
                        } catch (XMLStreamException e) {
                            exceptionReference.set(e);
                        }
                    });
                    Exception exception = exceptionReference.get();
                    if (exception != null) {
                        session.transfer(flowFile, REL_SUCCESS);
                    } else {
                        session.transfer(flowFile, REL_FAILURE);
                    }
                });
            } else {
                AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
                session.write(flowFile, out -> {
                    try {
                        XMLStreamWriter writer = createWriter(out);
                        try {
                            fileHeader.forEachRemaining(chunkHeader -> chunkHeader.forEachRemaining(record -> {
                                        try {
                                            new XmlBxmlNodeVisitor(writer, record.getRootNode());
                                        } catch (IOException e) {
                                            exceptionReference.set(e);
                                        }
                                    })
                            );
                        } finally {
                            close(writer);
                        }
                    } catch (XMLStreamException e) {
                        exceptionReference.set(e);
                    }
                });
                Exception exception = exceptionReference.get();
                if (exception != null) {
                    session.transfer(flowFile, REL_SUCCESS);
                } else {
                    session.transfer(flowFile, REL_FAILURE);
                }
            }
        });
        session.transfer(flowFile);
    }
}
