package org.jtaccuino.jshell;

import jdk.jshell.SourceCodeAnalysis;

import java.util.HashSet;
import java.util.Set;

public record SyntaxHighlight(int start, int end, Set<SyntaxHighlightAttribute> attributes) {
    public enum SyntaxHighlightAttribute {
        DECLARATION,
        DEPRECATED,
        KEYWORD,
        CLASS_DECLARATION,
        METHOD_DECLARATION,
        VARIABLE_REFERENCE,
        METHOD_INVOCATION,
        STRING,
        COMMENT;
    }

    public static SyntaxHighlight fromJShellHighlight(SourceCodeAnalysis.Highlight highlight){
        Set<SyntaxHighlightAttribute> attributes = new HashSet<>();
        for (SourceCodeAnalysis.Attribute attribute : highlight.attributes()) {
            switch (attribute) {
                case SourceCodeAnalysis.Attribute.DECLARATION -> attributes.add(SyntaxHighlightAttribute.DECLARATION);
                case SourceCodeAnalysis.Attribute.DEPRECATED -> attributes.add(SyntaxHighlightAttribute.DEPRECATED);
                case SourceCodeAnalysis.Attribute.KEYWORD -> attributes.add(SyntaxHighlightAttribute.KEYWORD);
            }

        }
        return new SyntaxHighlight(highlight.start(),highlight.end(), attributes);
    }
}
