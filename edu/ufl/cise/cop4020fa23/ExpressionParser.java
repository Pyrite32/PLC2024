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

import javax.sound.midi.MidiChannel;

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

public class ExpressionParser implements IParser {

	final ILexer lexer;
	boolean sniffed = false;
	protected IToken t;
	protected IToken n;

	/**
	 * @param lexer
	 * @throws LexicalException
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}

	@Override
	public AST parse() throws PLCCompilerException {
		if (t.kind() == EOF)
			throw new SyntaxException("all is nothing");
		n = lexer.next();
		if (n == null) {
			throw new SyntaxException("invalid!");
		}
		return expr();
	}

	// * ConditionalExpr ::= ? Expr : Expr : Expr
	protected Expr cond() throws PLCCompilerException // base-level chain. End exactly here
	{
		var first = t;
		Expr cond = null;
		eat();
		Expr condition = expr();
		require(RARROW);
		Expr whenTrue = expr();
		require(COMMA);
		Expr whenFalse = expr();
		return new ConditionalExpr(first, condition, whenTrue, whenFalse);
	}

	// * LogicalOrExpr ::= LogicalAndExpr ( ( | | || ) LogicalAndExpr)*
	protected Expr logor() throws PLCCompilerException {
		// musthave logand
		var first = t;
		Expr left = logand();
		Expr right = null;
		while (on(BITOR, OR)) {
			var operand = eat();
			right = logand();
			left = new BinaryExpr(first, left, operand, right);
		}
		return left;
	}

	// * LogicalAndExpr ::= ComparisonExpr ( ( & | && ) ComparisonExpr)*
	protected Expr logand() throws PLCCompilerException {
		var first = t;
		Expr left = compar();
		Expr right = null;
		while (on(BITAND, AND)) {
			var operand = eat();
			right = compar();
			left = new BinaryExpr(first, left, operand, right);
		}
		return left;
	}

	// * ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
	protected Expr compar() throws PLCCompilerException {
		var first = t;
		Expr left = pow();
		Expr right = null;
		while (on(LT, GT, EQ, LE, GE)) {
			var operand = eat();
			right = pow();
			left = new BinaryExpr(first, left, operand, right);
		}
		return left;
	}

	// * PowExpr ::= AdditiveExpr ** PowExpr | AdditiveExpr
	protected Expr pow() throws PLCCompilerException {
		var first = t;
		Expr left = add();
		while (on(EXP)) {
			var power = eat();
			Expr right = pow();
			left = new BinaryExpr(first, left, power, right);
		}
		return left;
	}

	// * AdditiveExpr ::= MultiplicativeExpr ( ( + | - ) MultiplicativeExpr )*
	protected Expr add() throws PLCCompilerException {
		var first = t;
		Expr left = mul();
		while (on(MINUS, PLUS)) {
			var operand = eat();
			Expr right = mul();
			left = new BinaryExpr(first, left, operand, right);
		}
		return left;
	}

	// * MultiplicativeExpr ::= UnaryExpr (( * | / | % ) UnaryExpr)*
	protected Expr mul() throws PLCCompilerException {
		var first = t;
		Expr left = unare();
		while (on(TIMES, DIV, MOD)) {
			var operand = eat();
			Expr right = unare();
			left = new BinaryExpr(first, left, operand, right);
		}
		return left;
	}

	// * UnaryExpr ::= ( ! | - | length | width) UnaryExpr | UnaryExprPostfix
	protected Expr unare() throws PLCCompilerException {
		var first = t;
		if (on(BANG, MINUS, RES_width, RES_height)) {
			var operand = eat();
			Expr upon = unare();
			return new UnaryExpr(first, operand, upon);
		} else {
			return unareAfter();
		}
	}

	// * UnaryExprPostfix::= AtomizedOrParenthesized (PixelSelector | ε )
	// (ChannelSelector | ε )
	protected Expr unareAfter() throws PLCCompilerException {
		var first = t;
		Expr left = atomOrParenned();
		PixelSelector pixsel = null;
		ChannelSelector chansel = null;
		if (on(LSQUARE)) {
			eat();
			pixsel = pix2();
		}
		if (on(COLON)) { // not muex because square can activate the colon
			eat();
			chansel = colorSel();
		}
		if (pixsel == null && chansel == null)
			return left;
		else
			return new PostfixExpr(first, left, pixsel, chansel);
	}

	// * ChannelSelector ::= : red | : green | : blue
	protected ChannelSelector colorSel() throws PLCCompilerException {
		var color = t;
		if (!on(RES_red, RES_green, RES_blue)) throw new SyntaxException("Invalid channel selection color.");
		eat();
		return new ChannelSelector(color, color);
	}

	// * PixelSelector ::= [ Expr , Expr ]
	protected PixelSelector pix2() throws PLCCompilerException {
		var first = t;
		Expr xExpr = expr();
		require(COMMA);
		Expr yExpr = expr();
		require(RSQUARE);
		return new PixelSelector(first, xExpr, yExpr);
	}

	// * ExpandedPixel ::= [ Expr , Expr , Expr ]
	protected Expr pix3() throws PLCCompilerException {
		var first = t;
		eat();
		Expr red = expr();
		require(COMMA);
		Expr green = expr();
		require(COMMA);
		Expr blue = expr();
		return new ExpandedPixelExpr(first, red, green, blue);

	}

	// * AtomizedOrParenthesized ::= ATOM | any Parenthesized Exp
	protected Expr atomOrParenned() throws PLCCompilerException {
		var first = t;
		Expr fact = null;
		if (on(NUM_LIT, STRING_LIT, CONST, Kind.BOOLEAN_LIT, IDENT)) // int_lit
		{
			fact = (Expr) atomize(t);
			eat();
		} 
		else if (on(LPAREN)) // OR (expr)
		{
			eat();
			fact = expr();
			require(RPAREN);
		}
		else if (on(LSQUARE)) {
			fact = pix3();
			eat();
		}
		else {
			throw new SyntaxException("improper expectation for grammene 'Factor'. on: " + t.text());
		}
		return fact;
	}

	// * Expr::= ConditionalExpr | LogicalOrExpr
	protected Expr expr() throws PLCCompilerException, LexicalException {
		var first = t;
		Expr main = null;
		if (on(QUESTION)) {
			if (on(QUESTION)) { // Expr is Conditional | LogicalOr
				main = cond();
			}
		}
		else {
			main = logor();
		}

		return main;
	}

	protected AST atomize(IToken token) throws SyntaxException {
		return switch (token.kind()) {
			case IDENT -> new IdentExpr(token);
			case STRING_LIT -> new StringLitExpr(token);
			case NUM_LIT -> new NumLitExpr(token);
			case CONST -> new ConstExpr(token);
			case BOOLEAN_LIT -> new BooleanLitExpr(token);
			case RES_blue, RES_green, RES_red -> new ChannelSelector(token, token);
			default -> throw new SyntaxException("You can't atomize the token of type : " + token.text());
		};
	}

	protected boolean on(Kind... kinds) {
		boolean match = false;
		for (var k : kinds) {
			if (t.kind() == k) {
				match = true;
				break;
			}
		}
		return match;
	}

	protected boolean ahead(Kind... kinds) {
		boolean match = false;
		for (var kind : kinds) {
			if (n.kind() == kind) {
				match = true;
				break;
			}
		}
		return match;
	}

	protected IToken eat() throws LexicalException {
		IToken tempm = t;
		t = n;
		n = lexer.next();
		return tempm;
	}

	protected IToken require(Kind... requirements) throws SyntaxException, LexicalException {
		return require(true, requirements);
	}

	protected void reject(Kind... requirements) throws SyntaxException, LexicalException {
		boolean throwMe = false;
		try
		{
			// don't skip --- this behaves more like a scan rather than a requirement
			require(false, requirements);
			throwMe = true;
		}
		catch (SyntaxException e) {
			throwMe = false;
		}
		if (throwMe) throw new SyntaxException("Rejected token as it was not expected there.");
	}

	protected IToken require(boolean skip, Kind... requirements) throws SyntaxException, LexicalException {
		boolean met = false;
		for (var requirement : requirements) {

			if (t.kind() == requirement) {
				met = true;
				break;
			}
		}
		if (!met)
			throw new SyntaxException("The required syntax punctuation has not been met.");
		if (skip)
			return eat();
		else
			return t;
	}
}
