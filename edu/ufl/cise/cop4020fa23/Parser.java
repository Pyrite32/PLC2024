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

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Block.BlockElem;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;


import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.util.LinkedList;
import java.util.List;

public class Parser implements IParser {
	
	final ILexer lexer;
	private IToken t;
	private IToken n;

	public Parser(ILexer lexer) throws LexicalException {
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
		AST e = program();
		return e;
	}

	private AST program() throws PLCCompilerException {
		var first = t;
		var type = type();
		var name = require(IDENT);
		require(LPAREN);
		var params = paramList();
		var block = block();
		require(EOF);
		return new Program(first, type, name, params, block);
	}

	private List<NameDef> paramList() throws PLCCompilerException {
		List<NameDef> res = new LinkedList<NameDef>();
		while (!on(RPAREN)) {
			res.add(nameDef());		
			if (!on(COMMA)) break;
			eat();
		}
		eat();
		return res;
	} 

	// NameDef ::= Type IDENT | Type Dimension IDENT
	private NameDef nameDef() throws PLCCompilerException {
		var first = t;
		var typum = type();
		Dimension dimension = null;
		if (on(LSQUARE)) {
			dimension = dim();
		} 
		var ident = require(IDENT);
		return new NameDef(first, typum, dimension, ident);
	}

	// Dimension ::= [ Expr , Expr ]
	private Dimension dim() throws PLCCompilerException {
		eat();
		var pixsel = pix2();
		return new Dimension(pixsel.firstToken(), pixsel.xExpr(), pixsel.yExpr());

	}

	// Block ::= <: (Declaration ; | Statement ;)* :>
	private Block block() throws PLCCompilerException {
		var first = t;
		require(BLOCK_OPEN);
		LinkedList<BlockElem> blocks = new LinkedList<BlockElem>();
		while (!on(BLOCK_CLOSE)) {
			if (on(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) { // declaration
				var declaration = decl();
				require(SEMI);
				blocks.add(declaration);
			}
			else { // statement -- a lot harder!
				var statement = stmt();
				require(SEMI);
				blocks.add(statement);
			}
		}
		require(BLOCK_CLOSE);
		return new Block(first, blocks);

	}

	// Declaration::= NameDef | NameDef = Expr
	private Declaration decl() throws PLCCompilerException {
		var first = t;
		var nameDef = nameDef();
		Expr expression = null;
		if (on(ASSIGN)) {
			eat();
			expression = expr();
		}
		return new Declaration(first, nameDef, expression);
	}

	/*  Statement::=
		LValue = Expr |
		write Expr |
		do GuardedBlock [] GuardedBlock* od |
		if GuardedBlock [] GuardedBlock* if |
		^ Expr |
		BlockStatement
	*/
	private Statement stmt() throws PLCCompilerException {
		var first = t;
		if (on(IDENT)) { // LValue
			var left = lval();
			require(ASSIGN);
			var right = expr();
			return new AssignmentStatement(first, left, right);
		}
		else if (on(RES_write)) {
			eat();
			var right = expr();
			return new WriteStatement(first, right);
		}
		else if (on(RES_do)) {
			eat();
			LinkedList<GuardedBlock> doMembers = new LinkedList<GuardedBlock>();
			var guardBlockFirst = guardedBlock();
			doMembers.add(guardBlockFirst);
			if (on(BOX)) {
				eat();
				reject(RES_od);
				while (!on(RES_od)) {
					doMembers.add(guardedBlock());
					if (!on(BOX)) break;
					eat();
					reject(RES_od);
				}
			}
			require(RES_od);
			return new DoStatement(first, doMembers);
		}
		else if (on(RES_if)) {
			eat();
			LinkedList<GuardedBlock> ifMembers = new LinkedList<GuardedBlock>();
			var guardBlockFirst = guardedBlock();
			ifMembers.add(guardBlockFirst);
			if (on(BOX)) {
				eat();
				reject(RES_fi);
				while (!on(RES_fi)) {
					ifMembers.add(guardedBlock());
					if (!on(BOX)) break;
					eat();
					reject(RES_fi);
				}
			}
			require(RES_fi);
			return new IfStatement(first, ifMembers);
		}
		else if (on(RETURN)) {
			eat();
			var expression = expr();
			return new ReturnStatement(first, expression);
		}
		else if (on(BLOCK_OPEN)) {
			return new StatementBlock(first, block());
		}
		throw new SyntaxException("Could not find matching grammar rule with token :" + first.toString());
	}

	private GuardedBlock guardedBlock() throws PLCCompilerException {
		var first = t;
		var guard = expr();
		require(RARROW);
		var block = block();
		return new GuardedBlock(first, guard, block);
	}

	private LValue lval() throws PLCCompilerException {
		var first = t;
		var name = require(IDENT);
		ChannelSelector cc = null;
		PixelSelector px = null;
		if (on(LSQUARE)) {
			eat();
			px = pix2();
		}
		if (on(COLON)) {
			eat();
			cc = colorSel();
		}
		return new LValue(first, name, px, cc);
	}

	private IToken type() throws PLCCompilerException {
		return require(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean);
	}

	/// inherited from ExpressionParser:

	// * ConditionalExpr ::= ? Expr : Expr : Expr
	protected Expr cond() throws PLCCompilerException // base-level chain. End exactly here
	{
		var first = t;
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

	// * AtomizedOrParenthesized ::= ATOM | any Parenthesized Exps
	protected Expr atomOrParenned() throws PLCCompilerException {
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
