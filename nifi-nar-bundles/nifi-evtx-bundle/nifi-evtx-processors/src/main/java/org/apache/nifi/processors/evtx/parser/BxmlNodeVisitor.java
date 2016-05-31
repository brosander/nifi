package org.apache.nifi.processors.evtx.parser;

import org.apache.nifi.processors.evtx.parser.bxml.*;
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
