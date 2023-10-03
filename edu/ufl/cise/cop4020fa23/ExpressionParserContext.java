package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.UnicornAST;


public record ExpressionParserContext(UnicornAST parent, int childIndex, boolean isComplete) {};