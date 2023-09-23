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
import java.util.List;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;

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

import java.util.Stack;

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
	
	final ILexer myLexer;
	private ArrayList<IToken> myTokenList;

	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.myLexer = lexer;
		myTokenList = new ArrayList<>();
		IToken current = lexer.next();
		while (current != null && current.kind() != Kind.EOF) {
			myTokenList.add(current);
			current = lexer.next();
		}
	}

	protected AST parseRecursivelyWithOffset(ArrayList<IToken> tokens, int length) throws SyntaxException {
		for (int i = 0; i < length; i++) {
			tokens.remove(0);
		}
		return parseRecursively(tokens);
	}

	protected AST parseTokenList(ArrayList<IToken> tokens) throws PLCCompilerException {
		AST root;
		IToken current = tokens.get(0);
		if (tokenIsKindOf(current, Kind.BOOLEAN_LIT, STRING_LIT, NUM_LIT, IDENT))
			root = getAtomic(current);
		else if (tokenIsKindOf(current, RES_red, RES_green, RES_blue))
			root = getChannelSelector();
		if (tokenIsKindOf(current, LSQUARE))
		{
			// 
			// begin pixel selection
			Expr pixelX, pixelY;
			IToken name;
			ArrayList<IToken> expressionLeft = new ArrayList<>();
			ArrayList<IToken> expressionRight = new ArrayList<>();
			ArrayList<IToken> list = untilKind(after(tokens,0), RSQUARE); // skip first elem, get all elements before RSQUARE
			var duo = splitBy(list, COMMA); // remove comma from evaluation.
			pixelX = (Expr)parseTokenList(duo.get(0));
			pixelY = (Expr)parseTokenList(duo.get(1));
		}
	}

	protected ArrayList<ArrayList<IToken>> splitBy(ArrayList<IToken> list, Kind delimiter)
	{
		boolean foundDelimiter = false;

		ArrayList<IToken> firstList = new ArrayList<>();
        ArrayList<IToken> secondList = new ArrayList<>();

        for (IToken item : list) {
            if (item.kind().equals(delimiter)) {
                foundDelimiter = true;
                continue; // Skip adding the delimiter itself to the lists
            }
            if (foundDelimiter) {
                secondList.add(item);
            } else {
                firstList.add(item);
            }
        }
		ArrayList<ArrayList<IToken>> result = new ArrayList<ArrayList<IToken>>();
		result.add(firstList);
		result.add(secondList);
		return result;
	}

	// kind-noninclusive.
	protected ArrayList<IToken> untilKind(ArrayList<IToken> list, Kind stop)
	{
		ArrayList<IToken> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++)
		{
			if (list.get(i).kind().equals(stop));
				break;
			result.add(list.get(i));
		}
		return result;
	}
	
	protected ArrayList<IToken> after(ArrayList<IToken> list, int location)
	{
		ArrayList<IToken> result = new ArrayList<IToken>();
		for (int i = location+1; i < list.size(); i++)
		{
			result.add(list.get(i));
		}
		return result;
		// 
	}

	protected ArrayList<IToken> before(ArrayList<IToken> list, int location)
	{
		ArrayList<IToken> result = new ArrayList<IToken>();
		for (int i = 0; i < location && i < list.size(); i++)
		{
			result.add(list.get(i));
		}
		return result;
	}


	protected boolean leftGoesFirst(IToken left, IToken right) throws SyntaxException {
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
		


	@Override
	public AST parse() throws PLCCompilerException {
		Expr e = expr();
		return e;
	}


	private Expr expr() throws PLCCompilerException {
		throw new UnsupportedOperationException("THE PARSER HAS NOT BEEN IMPLEMENTED YET");
	}

    

}
