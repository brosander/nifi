package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.apache.nifi.stream.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class WStringArrayTypeNode extends VariantTypeNode {
    public static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private final String value;

    public WStringArrayTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        String raw;
        if (length >= 0) {
            raw = binaryReader.readWString(length / 2);
        } else {
            int binaryLength = binaryReader.readWord();
            raw = binaryReader.readWString(binaryLength / 2);
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
