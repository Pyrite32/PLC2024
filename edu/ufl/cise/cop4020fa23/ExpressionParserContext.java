package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.AST;


public record ExpressionParserContext(AST parent, int child, boolean isFinished) {};