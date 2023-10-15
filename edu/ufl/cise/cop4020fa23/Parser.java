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

import edu.ufl.cise.cop4020fa23.ExpressionParser;

import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Parser extends ExpressionParser {
	

	public Parser(ILexer lexer) throws LexicalException {
		super(lexer);
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
			require(BOX);
			while (!on(RES_od)) {
				doMembers.add(guardedBlock());
			}
			eat();
			return new DoStatement(first, doMembers);
		}
		else if (on(RES_if)) {
			eat();
			LinkedList<GuardedBlock> ifMembers = new LinkedList<GuardedBlock>();
			var guardBlockFirst = guardedBlock();
			ifMembers.add(guardBlockFirst);
			require(BOX);
			while (!on(RES_fi)) {
				ifMembers.add(guardedBlock());
			}
			eat();
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

}
