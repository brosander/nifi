package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.apache.nifi.stream.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class WStringArrayTypeNode extends VariantTypeNode {
    public static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private final String value;

    public WStringArrayTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        String raw;
        if (length >= 0) {
            raw = readWString(length / 2);
        } else {
            UnsignedInteger binaryLength = readWord();
            raw = readWString(binaryLength.intValue() / 2);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            XMLStreamWriter xmlStreamWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(stream, "UTF-8");
            for (String s : raw.split("\u0000")) {
                xmlStreamWriter.writeStartElement("string");
                xmlStreamWriter.writeCharacters(s);
                xmlStreamWriter.writeEndElement();
            }
            xmlStreamWriter.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        value = stream.toString("UTF-8");
    }

    @Override
    public String getValue() {
        return value;
    }
}
