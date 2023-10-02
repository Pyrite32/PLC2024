/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the fall semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */
package edu.ufl.cise.cop4020fa23;

import static edu.ufl.cise.cop4020fa23.Kind.AND;
import static edu.ufl.cise.cop4020fa23.Kind.BANG;
import static edu.ufl.cise.cop4020fa23.Kind.BITAND;
import static edu.ufl.cise.cop4020fa23.Kind.BITOR;
import static edu.ufl.cise.cop4020fa23.Kind.COLON;
import static edu.ufl.cise.cop4020fa23.Kind.COMMA;
import static edu.ufl.cise.cop4020fa23.Kind.DIV;
import static edu.ufl.cise.cop4020fa23.Kind.EOF;
import static edu.ufl.cise.cop4020fa23.Kind.EQ;
import static edu.ufl.cise.cop4020fa23.Kind.EXP;
import static edu.ufl.cise.cop4020fa23.Kind.GE;
import static edu.ufl.cise.cop4020fa23.Kind.GT;
import static edu.ufl.cise.cop4020fa23.Kind.IDENT;
import static edu.ufl.cise.cop4020fa23.Kind.LE;
import static edu.ufl.cise.cop4020fa23.Kind.LPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.LSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.LT;
import static edu.ufl.cise.cop4020fa23.Kind.MINUS;
import static edu.ufl.cise.cop4020fa23.Kind.MOD;
import static edu.ufl.cise.cop4020fa23.Kind.NUM_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.OR;
import static edu.ufl.cise.cop4020fa23.Kind.PLUS;
import static edu.ufl.cise.cop4020fa23.Kind.QUESTION;
import static edu.ufl.cise.cop4020fa23.Kind.RARROW;
import static edu.ufl.cise.cop4020fa23.Kind.RES_blue;
import static edu.ufl.cise.cop4020fa23.Kind.RES_green;
import static edu.ufl.cise.cop4020fa23.Kind.RES_height;
import static edu.ufl.cise.cop4020fa23.Kind.RES_red;
import static edu.ufl.cise.cop4020fa23.Kind.RES_width;
import static edu.ufl.cise.cop4020fa23.Kind.RPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.RSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.STRING_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.TIMES;
import static edu.ufl.cise.cop4020fa23.Kind.CONST;

import java.util.Arrays;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import java.util.ArrayList;
import java.util.HashSet;

/**
Expr::=  ConditionalExpr | LogicalOrExpr    
ConditionalExpr ::=  ?  Expr  :  Expr  :  Expr 
LogicalOrExpr ::= LogicalAndExpr (    (   |   |   ||   ) LogicalAndExpr)*
LogicalAndExpr ::=  ComparisonExpr ( (   &   |  &&   )  ComparisonExpr)*
ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
PowExpr ::= AdditiveExpr ** PowExpr |   AdditiveExpr
AdditiveExpr ::= MultiplicativeExpr ( ( + | -  ) MultiplicativeExpr )*
MultiplicativeExpr ::= UnaryExpr (( * |  /  |  % ) UnaryExpr)*
UnaryExpr ::=  ( ! | - | length | width) UnaryExpr  |  UnaryExprPostfix
UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
PrimaryExpr ::=STRING_LIT | NUM_LIT |  IDENT | ( Expr ) | Z 
    ExpandedPixel  
ChannelSelector ::= : red | : green | : blue
PixelSelector  ::= [ Expr , Expr ]
ExpandedPixel ::= [ Expr , Expr , Expr ]
Dimension  ::=  [ Expr , Expr ]                         

 */

public class ExpressionParser implements IParser {
	
	final ILexer lexer;
	private AST root;

	int parenDepth = 0;

	

	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
	}

	@Override
	public AST parse() throws PLCCompilerException {
		// runs only once.
		IToken t = lexer.next();

		while (t.kind() != Kind.EOF) {
			useNext(t);
			t = lexer.next();
		}
		guardAgainst(root == null, "Nothing to compile.");
		assertTrue(isASTComplete(root), "Tree is incomplete, dangling Expr");
		return root;
	}

	protected void useNext(IToken current) throws PLCCompilerException {
		// determine if the root is empty.
			// if so, determine the statement type from FIRST:
			// FILTER_ATOM
			// ? Cond
			// [ pixSelector or Postfix
			// : channelSel or Postfix
			// ! - width height unary
			// throw!
		if (root == null) {
			root = astFromAtomStandalone(current);
			if (root == null) {
				if (current.kind() == QUESTION)
				root = new ConditionalExpr(current, null, null, null);
				else if (current.kind() == BANG ||
						 current.kind() == MINUS ||
						 current.kind() == RES_width ||
						 current.kind() == RES_height)
				{
					root = new UnaryExpr(current, current, null);
				}
				else if (current.kind() == LPAREN)
				{
					parenDepth++;
				}
				else
				{
					throw new SyntaxException("Basis of AST cannot begin with : " + current.kind().toString());
				}
			}
			return;
		}
		ExpressionParserContext context = getContext();
		// only atoms are finished.
		if (context.isFinished()) {
			return;
		}
		// we are interested in finishing an unfinished context one at a time.
		

		// modify the current context and child expr.
	}

	// scan the tree and determine if there are any null parts to the tree.
	// the null parts are considered incomplete and need to be replaced.
	protected ExpressionParserContext getContext() throws PLCCompilerException {
		// set currentRoot
		// set currentRootChildNum
		return _getContextRec(root, null, 0);
	}
	private ExpressionParserContext _getContextRec(AST currentNode, AST parent, int num) throws PLCCompilerException {
		
		// base successful case
		if (currentNode == null) {
			if (parent != null) {
				return new ExpressionParserContext(parent, num, false);
			}
			throw new SyntaxException("cannot getContext() on an empty node");
		}
		if (currentNode instanceof BooleanLitExpr ||
			currentNode instanceof ConstExpr	  ||
			currentNode instanceof IdentExpr	  ||
			currentNode instanceof StringLitExpr  ||
			currentNode instanceof NumLitExpr 	  ||
			currentNode instanceof ChannelSelector)
			return new ExpressionParserContext(parent, num, true);
		// immediately return the first ExpressionParserContext with a isFinished: false.
		else if (currentNode instanceof ConditionalExpr) {
			var c = (ConditionalExpr)currentNode;
			ExpressionParserContext e;
			e = _getContextRec(c.getGuardExpr(), currentNode, 0);
			if (!e.isFinished()) return e;
			e = _getContextRec(c.getTrueExpr(), currentNode, 1);
			if (!e.isFinished()) return e;
			e = _getContextRec(c.getFalseExpr(), currentNode, 2);
			if (!e.isFinished()) return e;
		}
		else if (currentNode instanceof ExpandedPixelExpr) {
			var c = (ExpandedPixelExpr)currentNode;
			ExpressionParserContext e;
			e = _getContextRec(c.getRed(), currentNode, 0);
			if (!e.isFinished()) return e;
			e = _getContextRec(c.getGreen(), currentNode, 1);
			if (!e.isFinished()) return e;
			e = _getContextRec(c.getBlue(), currentNode, 2);
			if (!e.isFinished()) return e;
		}
		else if (currentNode instanceof PixelSelector) {
			var c = (PixelSelector)currentNode;
			ExpressionParserContext e;
			e = _getContextRec(c.xExpr(), currentNode, 0);
			if (!e.isFinished()) return e;
			e = _getContextRec(c.yExpr(), currentNode, 1);
			if (!e.isFinished()) return e;
		}
		else if (currentNode instanceof PostfixExpr) {
			var c = (PostfixExpr)currentNode;
			ExpressionParserContext e;
			ExpressionParserContext e2;
			e = _getContextRec(c.primary(), currentNode, 0);
			if (!e.isFinished()) return e;
			e = _getContextRec(c.pixel(), currentNode, 1);
			e2 = _getContextRec(c.channel(), currentNode, 2);
			if (!e.isFinished() && !e2.isFinished()) return e;

		}
		else if (currentNode instanceof UnaryExpr) {
			var c = (UnaryExpr)currentNode;
			ExpressionParserContext e;
			e = _getContextRec(c.getExpr(), currentNode, 0);
			if (!e.isFinished()) return e;
		}
		throw new SyntaxException("Could not resolve unknown Expr");
	}

	protected boolean isASTComplete(AST currentNode) throws SyntaxException {
		// base false case
		if (currentNode == null)
			return false;

		// base true case
		// atoms are already created at full completion.
		if (currentNode instanceof BooleanLitExpr ||
			currentNode instanceof ConstExpr	  ||
			currentNode instanceof IdentExpr	  ||
			currentNode instanceof StringLitExpr  ||
			currentNode instanceof NumLitExpr 	  ||
			currentNode instanceof ChannelSelector)
		return true;
		else if (currentNode instanceof BinaryExpr ) {
			var c = (BinaryExpr)currentNode;
			return isASTComplete(c.getLeftExpr()) &&
				   isASTComplete(c.getRightExpr());
		}
		else if (currentNode instanceof ConditionalExpr) {
			var c = (ConditionalExpr)currentNode;
			return isASTComplete(c.getGuardExpr()) &&
				   isASTComplete(c.getTrueExpr())  &&
				   isASTComplete(c.getFalseExpr());
		}
		else if (currentNode instanceof ExpandedPixelExpr) {
			var c = (ExpandedPixelExpr)currentNode;
			return isASTComplete(c.getRed()) && 
				   isASTComplete(c.getGreen()) &&
				   isASTComplete(c.getBlue());
		}
		else if (currentNode instanceof PixelSelector) {
			var c = (PixelSelector)currentNode;
			return isASTComplete(c.xExpr()) &&
				   isASTComplete(c.yExpr());
		}
		else if (currentNode instanceof PostfixExpr) {
			var c = (PostfixExpr)currentNode;
			return isASTComplete(c.primary()) &&
				   (isASTComplete(c.pixel())  ||
					isASTComplete(c.channel()));

		}
		else if (currentNode instanceof UnaryExpr) {
			var c = (UnaryExpr)currentNode;
			return isASTComplete(c.getExpr());
		}
		throw new SyntaxException("Unhandled expression type found!");
	}

	protected AST astFromAtomStandalone(IToken token) {
		return switch (token.kind()) {
			case STRING_LIT -> new StringLitExpr(token);
			case NUM_LIT -> new NumLitExpr(token);
			case BOOLEAN_LIT -> new BooleanLitExpr(token);
			case IDENT -> new IdentExpr(token);
			case CONST -> new ConstExpr(token);
			default -> null;
		};
	}


	protected boolean naturalPemdazIsLeft(IToken left, IToken right) throws SyntaxException {
		// true -> left
		// false -> right
		final Kind[] logical = { AND, OR }; 
		final Kind[] conditional = { GE, EQ, GT, LE }; 
		final Kind[] addSubtract = { PLUS, MINUS }; 
		final Kind[] multiplyDivide = { TIMES, DIV, MOD };
		
		assertTrue(
			tokenIsKindOf(left, logical) && tokenIsKindOf(left, conditional) &&
			tokenIsKindOf(left, addSubtract) && tokenIsKindOf(left, multiplyDivide),
			"non-PEMDAZ token found in left : " + left.kind().toString());

		assertTrue(
			tokenIsKindOf(right, logical) && tokenIsKindOf(right, conditional) &&
			tokenIsKindOf(right, addSubtract) && tokenIsKindOf(right, multiplyDivide),
			"non-PEMDAZ token found in right : " + right.kind().toString());

		if (left.kind() == Kind.ASSIGN) return true;
		if (tokenIsKindOf(left, logical)) {
			return !tokenIsKindOf(right, logical); // logical are done left to right.
		}
		if (tokenIsKindOf(left, conditional)) {
			return !tokenIsKindOf(right, logical) &&
				   !tokenIsKindOf(right, conditional);
		}
		if (tokenIsKindOf(left, addSubtract)) {
			return !tokenIsKindOf(right, logical) &&
				   !tokenIsKindOf(right, conditional) &&
				   !tokenIsKindOf(right, addSubtract);
		}
		// the most strict.
		if (tokenIsKindOf(left, multiplyDivide)) {
			return tokenIsKindOf(right, multiplyDivide);
		}

		return false;
		
	}

	protected void guardAgainst(boolean faulty, String message) throws SyntaxException
	{
		if (faulty) throw new SyntaxException(message);
	}

	protected void assertTrue(boolean fact, String message) throws SyntaxException
	{
		if (fact == false) throw new SyntaxException(message);
	}

	private boolean tokenIsKindOf(IToken t, Kind... kinds) {
		for (Kind k : kinds) {
			if (t.kind() == k) return true;
		}
		return false;
	}

	private AST getAtomic(IToken token) throws PLCCompilerException
	{
		return switch (token.kind()) {
			case BOOLEAN_LIT -> new BooleanLitExpr(token);
			case STRING_LIT -> new StringLitExpr(token);
			case IDENT -> new IdentExpr(token);
			case NUM_LIT -> new NumLitExpr(token);
			default -> throw new PLCCompilerException("getAtomic() not properly filtered.");
		};
	}
		

}
