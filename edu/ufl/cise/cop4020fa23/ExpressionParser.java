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
	private UnicornAST root;
	private AST currentContextRoot;
	private boolean usePixelSelector = false;

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
		AST result = UnicornAST.buildAST(root);
		guardAgainst(result == null, "Nothing to compile.");
		//assertTrue(isASTComplete(root), "Tree is incomplete, dangling Expr");
		return result;
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

		if (current.kind() == LPAREN) {
			parenDepth++;
			return;
		}
		else if (current.kind() == RPAREN) {
			parenDepth--;
			return;
		}

		if (root == null) {
			root = UnicornAST.fromAtomStandalone(current);
			if (root == null) {
				if (current.kind() == QUESTION)
				root = UnicornAST.fromNewConditionalExpr(current);
				else if (current.kind() == BANG ||
						 current.kind() == MINUS ||
						 current.kind() == RES_width ||
						 current.kind() == RES_height)
				{
					root = UnicornAST.fromNewUnaryExpr(current);
				}
				// else if (current.kind() == LPAREN)
				// {
				// 	parenDepth++;
				// }
				else
				{
					throw new SyntaxException("Basis of AST cannot begin with : " + current.kind().toString());
				}
			}
			return;
		}

		ExpressionParserContext astContext = UnicornAST.getFirstUnfinishedAST(root);
		UnicornAST parentToModify = astContext.parent();
		if (astContext.parent() == null) parentToModify = root;

		// if the tree is currently complete, or if a sequential syntax is left unresolved.
		if (astContext.isComplete()) {
			UnicornAST newAST = UnicornAST.fromAtomStandalone(current);

			// if this is an atomic...
			if (newAST != null) {
				throw new SyntaxException("Unexpected atom: " + current.text() + " --- current statement should be complete!");
			}
			// handle statement shift characters.
			else if (current.kind() == Kind.PLUS  || 
				current.kind() == Kind.MINUS || 
				current.kind() == Kind.TIMES || 
				current.kind() == Kind.DIV   ||
				current.kind() == Kind.MOD)
				{
					AST mutiner = new BinaryExpr(current, null, current, null);
					UnicornAST uni = new UnicornAST(mutiner, current);
					parentToModify = parentToModify.mutinizeMe(uni);
					if (parentToModify.parent == null)
					root = parentToModify; 

					System.out.println("ClassName r : " + root.dataHead.getClass().getSimpleName());
					System.out.println("ClassName p : " + parentToModify.dataHead.getClass().getSimpleName());

				}
			else if (current.kind() == Kind.RPAREN) {
				AST mutiner = new PostfixExpr(current, null, null, null);
				AST mutinerL = new PixelSelector(current, null, null);
				UnicornAST uni = new UnicornAST(mutiner);
				UnicornAST uniL = new UnicornAST(mutinerL);
				uni.usePixelSelector = true;
				uni.left = uniL;
				if (parentToModify == root)
					parentToModify = parentToModify.mutinizeMe(uni);
				else
					parentToModify.mutinizeMe(uni);
				
			}
			else if (current.kind() == Kind.BANG ||
				current.kind() == Kind.MINUS ||
				current.kind() == Kind.RES_width ||
				current.kind() == Kind.RES_height) {
					System.out.println("unary expr");
					AST mutiner = new UnaryExpr(current, current, null);
					UnicornAST uni = new UnicornAST(mutiner);
					if (parentToModify == root)
						parentToModify = parentToModify.mutinizeMe(uni);
					else
						parentToModify.mutinizeMe(uni);
				
			}
			else if (current.kind() == Kind.BITAND ||
				current.kind() == Kind.AND ||
				current.kind() == Kind.OR ||
				current.kind() == Kind.BITOR)
			{

			}
			

		}
		else {
			// // an unfinished AST needs to be filled with atoms or other statements.
			UnicornAST newAst = UnicornAST.fromAtomStandalone(current);

			// AST shifting token?
			if (newAst == null) {
				if (parentToModify.dataHead instanceof PixelSelector || parentToModify.dataHead instanceof ExpandedPixelExpr)
				{
					if (current.kind() == Kind.COMMA)
						parentToModify.incrementSequentialCount();
					else if (current.kind() == Kind.RSQUARE) // RSQUARE signifies the end of the syntax
						parentToModify.forceComplete = true;
					else
						throw new SyntaxException("Incorrect grammar encountered when examining PixelSelector : " + current.text());
				}
				else if (parentToModify.dataHead instanceof ConditionalExpr)
				{
					if (current.kind() == Kind.RARROW) {
						if (parentToModify.getSequentialCount() == 0) {
							parentToModify.incrementSequentialCount();
						}
						else {
							throw new SyntaxException("Incorrect grammar encountered in conditional!");
						}
					}
					else if (current.kind() == Kind.COMMA) {
						if (parentToModify.getSequentialCount() == 1) {
							parentToModify.incrementSequentialCount();
							parentToModify.forceComplete = true; // there are no more commas afterwards.
						}
					}
				}
				else {
					throw new SyntaxException("Expected atoms to fill incomplete grammar, but got a logic shifter instead: " + current.text());
				}
				return;
			}
			else
			{
				parentToModify.setChildByIndex(astContext.childIndex(), newAst);
			}


		}
		// modify the current context and child expr.
	}

	protected boolean isPunctuation(IToken token) {
		return token.kind() == LPAREN || 
			   token.kind() == Kind.RPAREN ||
			   token.kind() == COMMA;
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

}
