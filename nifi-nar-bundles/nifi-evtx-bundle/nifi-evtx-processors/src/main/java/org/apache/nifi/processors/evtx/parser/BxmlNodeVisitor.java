package org.apache.nifi.processors.evtx.parser;

import org.apache.nifi.processors.evtx.parser.bxml.AttributeNode;
import org.apache.nifi.processors.evtx.parser.bxml.CDataSectionNode;
import org.apache.nifi.processors.evtx.parser.bxml.CloseElementNode;
import org.apache.nifi.processors.evtx.parser.bxml.CloseEmptyElementNode;
import org.apache.nifi.processors.evtx.parser.bxml.CloseStartElementNode;
import org.apache.nifi.processors.evtx.parser.bxml.ConditionalSubstitutionNode;
import org.apache.nifi.processors.evtx.parser.bxml.EndOfStreamNode;
import org.apache.nifi.processors.evtx.parser.bxml.EntityReferenceNode;
import org.apache.nifi.processors.evtx.parser.bxml.NameStringNode;
import org.apache.nifi.processors.evtx.parser.bxml.NormalSubstitutionNode;
import org.apache.nifi.processors.evtx.parser.bxml.OpenStartElementNode;
import org.apache.nifi.processors.evtx.parser.bxml.ProcessingInstructionDataNode;
import org.apache.nifi.processors.evtx.parser.bxml.ProcessingInstructionTargetNode;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;
import org.apache.nifi.processors.evtx.parser.bxml.StreamStartNode;
import org.apache.nifi.processors.evtx.parser.bxml.TemplateInstanceNode;
import org.apache.nifi.processors.evtx.parser.bxml.TemplateNode;
import org.apache.nifi.processors.evtx.parser.bxml.ValueNode;
import org.apache.nifi.processors.evtx.parser.bxml.value.VariantTypeNode;

import java.io.IOException;

/**
 * Created by brosander on 5/31/16.
 */
public interface BxmlNodeVisitor {
    default void visit(RootNode rootNode) throws IOException {

    }

    default void visit(TemplateInstanceNode templateInstanceNode) throws IOException {

    }

    default void visit(TemplateNode templateNode) throws IOException {

    }

    default void visit(ValueNode valueNode) throws IOException {

    }

    default void visit(StreamStartNode streamStartNode) throws IOException {

    }

    default void visit(ProcessingInstructionTargetNode processingInstructionTargetNode) throws IOException {

    }

    default void visit(ProcessingInstructionDataNode processingInstructionDataNode) throws IOException {

    }

    default void visit(OpenStartElementNode openStartElementNode) throws IOException {

    }

    default void visit(NormalSubstitutionNode normalSubstitutionNode) throws IOException {

    }

    default void visit(NameStringNode nameStringNode) throws IOException {

    }

    default void visit(EntityReferenceNode entityReferenceNode) throws IOException {

    }

    default void visit(EndOfStreamNode endOfStreamNode) throws IOException {

    }

    default void visit(ConditionalSubstitutionNode conditionalSubstitutionNode) throws IOException {

    }

    default void visit(CloseStartElementNode closeStartElementNode) throws IOException {

    }

    default void visit(CloseEmptyElementNode closeEmptyElementNode) throws IOException {

    }

    default void visit(CloseElementNode closeElementNode) throws IOException {

    }

    default void visit(CDataSectionNode cDataSectionNode) throws IOException {

    }

    default void visit(AttributeNode attributeNode) throws IOException {

    }

    default void visit(VariantTypeNode variantTypeNode) throws IOException {

    }
}
