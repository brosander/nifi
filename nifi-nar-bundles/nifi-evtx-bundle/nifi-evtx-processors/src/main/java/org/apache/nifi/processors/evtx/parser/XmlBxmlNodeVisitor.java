package org.apache.nifi.processors.evtx.parser;

import org.apache.nifi.processors.evtx.parser.bxml.*;
import org.apache.nifi.processors.evtx.parser.bxml.value.BXmlTypeNode;
import org.apache.nifi.processors.evtx.parser.bxml.value.VariantTypeNode;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brosander on 5/31/16.
 */
public class XmlBxmlNodeVisitor implements BxmlNodeVisitor {
    private final XMLStreamWriter xmlStreamWriter;
    private List<VariantTypeNode> substitutions;

    public XmlBxmlNodeVisitor(XMLStreamWriter xmlStreamWriter, RootNode rootNode) throws IOException {
        this.xmlStreamWriter = xmlStreamWriter;
        substitutions = rootNode.getSubstitutions();
        for (BxmlNode bxmlNode : rootNode.getChildren()) {
            bxmlNode.accept(this);
        }
    }

    @Override
    public void visit(OpenStartElementNode openStartElementNode) throws IOException {
        try {
            xmlStreamWriter.writeStartElement(openStartElementNode.getTagName());
            List<BxmlNode> nonAttributeChildren = new ArrayList<>();
            for (BxmlNode bxmlNode : openStartElementNode.getChildren()) {
                if (bxmlNode instanceof AttributeNode) {
                    bxmlNode.accept(this);
                } else {
                    nonAttributeChildren.add(bxmlNode);
                }
            }
            for (BxmlNode nonAttributeChild : nonAttributeChildren) {
                nonAttributeChild.accept(this);
            }
            xmlStreamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void visit(AttributeNode attributeNode) throws IOException {
        try {
            AttributeNodeVisitor attributeNodeVisitor = new AttributeNodeVisitor();
            attributeNodeVisitor.visit(attributeNode);
            xmlStreamWriter.writeAttribute(attributeNode.getAttributeName(), attributeNodeVisitor.getValue());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void visit(TemplateInstanceNode templateInstanceNode) throws IOException {
        templateInstanceNode.getTemplateNode().accept(this);
    }

    @Override
    public void visit(TemplateNode templateNode) throws IOException {
        for (BxmlNode bxmlNode : templateNode.getChildren()) {
            bxmlNode.accept(this);
        }
    }

    @Override
    public void visit(RootNode rootNode) throws IOException {
        new XmlBxmlNodeVisitor(xmlStreamWriter, rootNode);
    }

    @Override
    public void visit(CDataSectionNode cDataSectionNode) throws IOException {
        try {
            xmlStreamWriter.writeCData(cDataSectionNode.getCdata());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void visit(EntityReferenceNode entityReferenceNode) throws IOException {
        try {
            xmlStreamWriter.writeCharacters(entityReferenceNode.getValue());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void visit(ValueNode valueNode) throws IOException {
        for (BxmlNode bxmlNode : valueNode.getChildren()) {
            bxmlNode.accept(this);
        }
    }

    @Override
    public void visit(ConditionalSubstitutionNode conditionalSubstitutionNode) throws IOException {
        substitutions.get(conditionalSubstitutionNode.getIndex()).accept(this);
    }

    @Override
    public void visit(NormalSubstitutionNode normalSubstitutionNode) throws IOException {
        substitutions.get(normalSubstitutionNode.getIndex()).accept(this);
    }

    @Override
    public void visit(VariantTypeNode variantTypeNode) throws IOException {
        try {
            if (variantTypeNode instanceof BXmlTypeNode) {
                ((BXmlTypeNode) variantTypeNode).getRootNode().accept(this);
            } else {
                xmlStreamWriter.writeCharacters(variantTypeNode.getValue());
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private class AttributeNodeVisitor implements BxmlNodeVisitor {
        private String value;

        public String getValue() {
            return value;
        }

        @Override
        public void visit(AttributeNode attributeNode) throws IOException {
            attributeNode.getValue().accept(this);
        }

        @Override
        public void visit(ValueNode valueNode) throws IOException {
            for (BxmlNode bxmlNode : valueNode.getChildren()) {
                bxmlNode.accept(this);
            }
        }

        @Override
        public void visit(VariantTypeNode variantTypeNode) throws IOException {
            value = variantTypeNode.getValue();
        }

        @Override
        public void visit(NormalSubstitutionNode normalSubstitutionNode) throws IOException {
            value = substitutions.get(normalSubstitutionNode.getIndex()).getValue();
        }

        @Override
        public void visit(ConditionalSubstitutionNode conditionalSubstitutionNode) throws IOException {
            value = substitutions.get(conditionalSubstitutionNode.getIndex()).getValue();
        }
    }
}
