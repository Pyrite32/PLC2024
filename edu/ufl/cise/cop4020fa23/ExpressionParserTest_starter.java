package edu.ufl.cise.cop4020fa23;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
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

class ExpressionParserTest_starter {

	/**
	 * Constructs a scanner and parser for the given input string, scans and parses
	 * the input and returns and AST.
	 *
	 * @param input String representing program to be tested
	 * @return AST representing the program
	 * @throws PLCCompilerException
	 */
	AST getAST(String input) throws PLCCompilerException {
		return ComponentFactory.makeExpressionParser(input).parse();
	}

	/**
	 * Checks that the given AST e has type NumLitExpr with the indicated value.
	 * Returns the given AST cast to NumLitExpr.
	 *
	 * @param e
	 * @param value
	 * @return
	 */
	NumLitExpr checkNumLitExpr(AST e, int value) {
		assertThat("", e, instanceOf(NumLitExpr.class));
		NumLitExpr ne = (NumLitExpr) e;
		assertEquals(Integer.toString(value), ne.getText());
		return ne;
	}

	/**
	 * Checks that the given AST e has type StringLitExpr with the given String
	 * value. For convenience, we do not require the value to include the enclosing
	 * quotes. So we can write checkStringLitExpr(ast, "hello") rather than
	 * checkStringLitExpr(ast,""hello"");
	 *
	 * Returns the given AST cast to StringLitExpr.
	 *
	 * @param e
	 * @param nameToken
	 * @return
	 */
	StringLitExpr checkStringLitExpr(AST e, String value) {
		assertThat("", e, instanceOf(StringLitExpr.class));
		StringLitExpr se = (StringLitExpr) e;
		String s = se.getText();
		assertEquals('"', s.charAt(0)); // check that first char is "
		assertEquals('"', s.charAt(s.length() - 1));
		assertEquals(value, s.substring(1, s.length() - 1));
		return se;
	}

	/**
	 * Checks that the given AST has type BooleanLitExpr and represents the given boolean value
	 * 
	 * @param e
	 * @param value
	 * @return
	 */
	BooleanLitExpr checkBooleanLitExpr(AST e, boolean value) {
		assertThat("", e, instanceOf(BooleanLitExpr.class));
		BooleanLitExpr be = (BooleanLitExpr) e;
		assertEquals(Boolean.toString(value), be.getText());
		return be;
	}

	/**
	 * Checks that the given AST e has type UnaryExpr with the given operator.
	 * Returns the given AST cast to UnaryExpr.
	 *
	 * @param e
	 * @param op Kind of expected operator
	 * @return
	 */
	private UnaryExpr checkUnaryExpr(AST e, Kind op) {
		assertThat("", e, instanceOf(UnaryExpr.class));
		assertEquals(op, ((UnaryExpr) e).getOp());
		return (UnaryExpr) e;
	}

	/**
	 * Checks that the given AST e has type ConditionalExpr. Returns the given AST
	 * cast to ConditionalExpr.
	 *
	 * @param e
	 * @return
	 */
	private ConditionalExpr checkConditionalExpr(AST e) {
		assertThat("", e, instanceOf(ConditionalExpr.class));
		return (ConditionalExpr) e;
	}

	/**
	 * Checks that the given AST e has type BinaryExpr with the given operator.
	 * Returns the given AST cast to BinaryExpr.
	 *
	 * @param e
	 * @param op Kind of expected operator
	 * @return
	 */
	BinaryExpr checkBinaryExpr(AST e, Kind expectedOp) {
		assertThat("", e, instanceOf(BinaryExpr.class));
		BinaryExpr be = (BinaryExpr) e;
		assertEquals(expectedOp, be.getOp().kind());
		return be;
	}

	/**
	 * Checks that the given AST e has type IdentExpr with the given name. Returns
	 * the given AST cast to IdentExpr.
	 *
	 * @param e
	 * @param name
	 * @return
	 */
	IdentExpr checkIdentExpr(AST e, String name) {
		assertThat("", e, instanceOf(IdentExpr.class));
		IdentExpr ident = (IdentExpr) e;
		assertEquals(name, ident.getName());
		return ident;
	}

	/**
	 * Checks that the given AST e has type ChannelSelector with the indicated color
	 *
	 * @param e
	 * @param expectedColor
	 * @return
	 */
	ChannelSelector checkChannelSelector(AST e, Kind expectedColor) {
		assertThat("", e, instanceOf(ChannelSelector.class));
		ChannelSelector chan = (ChannelSelector) e;
		assertEquals(expectedColor, chan.color());
		return chan;
	}

	/**
	 * Checks that given AST e has type PostfixExpr, and checks whether or not
	 * ChannelSelectors and PixelSelectors exist
	 *
	 * @param e
	 * @param hasChannelSelector
	 * @param hasPixelSelector
	 * @return
	 */
	PostfixExpr checkPostfixExpr(AST e, boolean hasPixelSelector, boolean hasChannelSelector) {
		assertThat("", e, instanceOf(PostfixExpr.class));
		PostfixExpr pfe = (PostfixExpr) e;
		AST channel = pfe.channel();
		assertEquals(hasChannelSelector, channel != null);
		AST pixel = pfe.pixel();
		assertEquals(hasPixelSelector, pixel != null);
		return pfe;
	}

	@Test
	void test0() throws PLCCompilerException {
		String input = """
				""

				""";
		AST ast = getAST(input);
		checkStringLitExpr(ast, ""); // stringLiteral AST
	}
	// enter
	// get ""
	// load "" as root
	// end
	// ret

	// str
	//	-> 

	@Test
	void test1() throws PLCCompilerException {
		String input = """
				9

				""";
		AST ast = getAST(input);
		checkNumLitExpr(ast, 9); // numLiteral AST
	}
	// enter
	// get 3
	// load 3 as root
	// end
	// ret

	@Test
	void test2() throws PLCCompilerException {
		String input = """
				b

				""";
		AST ast = getAST(input);
		checkIdentExpr(ast, "b"); // identity AST
	}
	// enter
	// get b
	// load b as root
	// end
	// ret

	@Test
	void test3() throws PLCCompilerException {
		String input = """
				"hello"

				""";
		AST ast = getAST(input);
		checkStringLitExpr(ast, "hello"); // string literal AST again
	}
	// enter
	// get hello
	// load hello as root
	// end
	// ret

	@Test
	void test4() throws PLCCompilerException {
		String input = """
				a:red

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, false, true); // ast is the selector type postfixExpr
		Expr v0 = ((PostfixExpr) ast).primary(); 
		checkIdentExpr(v0, "a");						// ast has a primary component and it is an ident
		ChannelSelector v1 = ((PostfixExpr) ast).channel();	
		checkChannelSelector(v1, Kind.RES_red); 			// ast has a secondary component and it is the reserved term RED. 
	}
	// enter
	// get a (atomic lexeme)
	// load a as root
	// get :
	// logic : means postfix			U		  	  / K
	// dismount 'a' to left 						U
	// get red (atomic lexeme)
	// mount 'red' to right
	// end
	// ret

	@Test
	void test5() throws PLCCompilerException {
		String input = """
				(a+b):green

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, false, true); // ast is the type postfixExpr
		Expr v0 = ((PostfixExpr) ast).primary();								// ast has a primary component and it is a binaryExpr.
		checkBinaryExpr(v0, Kind.PLUS);											// ensure the operation is plus
		Expr v1 = ((BinaryExpr) v0).getLeftExpr();								// ensure left is an ident
		checkIdentExpr(v1, "a");
		Expr v2 = ((BinaryExpr) v0).getRightExpr();								// ensure right is an ident
		checkIdentExpr(v2, "b");
		ChannelSelector v3 = ((PostfixExpr) ast).channel();						// ast has a secondary component and it is a reserved
		checkChannelSelector(v3, Kind.RES_green);

	}
	// get paren
	// push MAX pemdaz value to stack
	// get a (atomic)
	// make 'a' root
	// get +
	// logic + means binaere
	// dismount 'a' left
	// get b
	// add to root right
	// get paren
	// pop MAX pemdaz
	// get :
	// logic : means postfix
	// dismount binare to left
	// get green
	// push green to right
	// end
	// ret

	@Test
	void test6() throws PLCCompilerException {
		String input = """
				x[x,y]

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, true, false); // ast is a postfix expression
		Expr v0 = ((PostfixExpr) ast).primary();								// the primary is an ident
		checkIdentExpr(v0, "x");
		PixelSelector v1 = ((PostfixExpr) ast).pixel();							// the secondary is a pixel selector
		Expr v2 = v1.xExpr();
		checkIdentExpr(v2, "x");											// pixel selected x is 'x'
		Expr v3 = v1.yExpr();
		checkIdentExpr(v3, "y");											// pixel selected y is 'y'
	}
	// get x (atomic)
	// set x as root
	// get [
	// dismount x to left with new unmodified PixelSel
	// get x
	// add x to PixelSel left.
	// get ,
	// switch to PixelSel right
	// get y
	// add y to PixelSel right
	// get ]
	// escape PixelSel
	// end, ret

	@Test
	void test7() throws PLCCompilerException {
		String input = """
				 (3)
				""";
		AST ast = getAST(input);
		checkNumLitExpr(ast, 3);											// parentheses affect pemdaz. but ast is numlit expr 
	}
	/* difference between:			3 + 5 * 2				(3 + 5) * 2
									binareAdd				binareMul
									3	binareMul		binareAdd	2	
											5	2		3	5
	If a mount is attempted with a symbol like ( * / + -), then the root is considered
	if the root type corresponds to the symbol, then pemdaz is considered before mounting
	make the decision.
 	*/
	@Test
	void test8() throws PLCCompilerException {
		String input = """
				 (!3)
				""";
		AST ast = getAST(input);
		checkUnaryExpr(ast, Kind.BANG);											// ast begins with bang that impacts 3.
		Expr v0 = ((UnaryExpr) ast).getExpr();							
		checkNumLitExpr(v0, 3);											
	}

	@Test
	void test9() throws PLCCompilerException {
		String input = """
				 -3
				""";
		AST ast = getAST(input);
		checkUnaryExpr(ast, Kind.MINUS);									// ast begins with minus that impacts 3.
		Expr v0 = ((UnaryExpr) ast).getExpr();
		checkNumLitExpr(v0, 3);
	}

	@Test
	void test10() throws PLCCompilerException {
		String input = """
				 (-3)
				""";
		AST ast = getAST(input);
		checkUnaryExpr(ast, Kind.MINUS);
		Expr v0 = ((UnaryExpr) ast).getExpr();								// pemdaz affected but neutral
		checkNumLitExpr(v0, 3);
	}


	// load a
	// root : ident
	// load [
	// dismount a left
	// root : new pixelSelector
	// load x
	// pixelSelector left = ident x
	// load +
	// make new binare ast, dismount x left
	// load 1
	// add 1 to binare ast left.
	// load ,
	// break discovery for x, move on to y.
	// ...
	// load ]
	// break discovery for PixelSelector
	// load :
	// dismount pixelSelector left
	// load blue atom
	// accessor.right = blue
	@Test
	void test11() throws PLCCompilerException {
		String input = """
				a[x+1,y-2]:blue

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, true, true);
		Expr v0 = ((PostfixExpr) ast).primary();					// ast begins with a, actually.
		checkIdentExpr(v0, "a");
		PixelSelector v1 = ((PostfixExpr) ast).pixel();				// aLeft is a pixel selector
		Expr v2 = v1.xExpr();
		checkBinaryExpr(v2, Kind.PLUS);								// pixel selector's X is binare
		Expr v3 = ((BinaryExpr) v2).getLeftExpr();
		checkIdentExpr(v3, "x");
		Expr v4 = ((BinaryExpr) v2).getRightExpr();
		checkNumLitExpr(v4, 1);
		Expr v5 = v1.yExpr();
		checkBinaryExpr(v5, Kind.MINUS);
		Expr v6 = ((BinaryExpr) v5).getLeftExpr();					// pixel selector's YY is binare.
		checkIdentExpr(v6, "y");
		Expr v7 = ((BinaryExpr) v5).getRightExpr();
		checkNumLitExpr(v7, 2);
		ChannelSelector v8 = ((PostfixExpr) ast).channel();			// aRight is the channel selector
		checkChannelSelector(v8, Kind.RES_blue);					//	ensure it is reserve blue
	}


	@Test
	void test13() throws PLCCompilerException {
		String input = """
				1-2+3*4/5%6
				""";
		AST ast = getAST(input);
		checkBinaryExpr(ast, Kind.PLUS);					// the first expression is NOT divisive; it is additive due to natural Pemdaz.
		Expr v0 = ((BinaryExpr) ast).getLeftExpr(); 		// additive.left is minus expr
		checkBinaryExpr(v0, Kind.MINUS);
		Expr v1 = ((BinaryExpr) v0).getLeftExpr();			// 
		checkNumLitExpr(v1, 1);
		Expr v2 = ((BinaryExpr) v0).getRightExpr();
		checkNumLitExpr(v2, 2);						
		Expr v3 = ((BinaryExpr) ast).getRightExpr();		// additive.right is times expr
		checkBinaryExpr(v3, Kind.MOD);
		Expr v4 = ((BinaryExpr) v3).getLeftExpr();
		checkBinaryExpr(v4, Kind.DIV);
		Expr v5 = ((BinaryExpr) v4).getLeftExpr();
		checkBinaryExpr(v5, Kind.TIMES);
		Expr v6 = ((BinaryExpr) v5).getLeftExpr();
		checkNumLitExpr(v6, 3);
		Expr v7 = ((BinaryExpr) v5).getRightExpr();
		checkNumLitExpr(v7, 4);
		Expr v8 = ((BinaryExpr) v4).getRightExpr();
		checkNumLitExpr(v8, 5);
		Expr v9 = ((BinaryExpr) v3).getRightExpr();
		checkNumLitExpr(v9, 6);
	}

	@Test
	void test14() throws PLCCompilerException {
		String input = """
				MAGENTA
				""";
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(ConstExpr.class));	// CONSTANT
	}

	@Test
	void test15() throws PLCCompilerException {
		String input = """
				 Z
				""";
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(ConstExpr.class));	// Constant
	}

	@Test
	void test18() throws PLCCompilerException {
		String input = """
				f+g
				""";
		AST ast = getAST(input);
		checkBinaryExpr(ast, Kind.PLUS);
		Expr v0 = ((BinaryExpr) ast).getLeftExpr();
		Expr v1 = ((BinaryExpr) ast).getRightExpr();
		System.out.println(v0.toString());
		System.out.println(v1.toString());
		checkIdentExpr(v0, "f");
		checkIdentExpr(v1, "g");
	}

	@Test
	void test19() throws PLCCompilerException {
		String input = """
				 ? d -> e , f
				""";
		AST ast = getAST(input);
		checkConditionalExpr(ast);							// ast is a conditional due to ?.
		Expr v0 = ((ConditionalExpr) ast).getGuardExpr();
		checkIdentExpr(v0, "d");						// guard expression is numlit d.
		Expr v1 = ((ConditionalExpr) ast).getTrueExpr();
		checkIdentExpr(v1, "e");						// conditional yield true is e.
		Expr v2 = ((ConditionalExpr) ast).getFalseExpr();
		checkIdentExpr(v2, "f");						// conditional yield false is f.
	}

	@Test
	void test29() throws PLCCompilerException {
		String input = """
				b+2
				""";
		AST ast = getAST(input);
		checkBinaryExpr(ast, Kind.PLUS);
		Expr v0 = ((BinaryExpr) ast).getLeftExpr();
		checkIdentExpr(v0, "b");
		Expr v1 = ((BinaryExpr) ast).getRightExpr();
		checkNumLitExpr(v1, 2);
	}

	@Test
	void test30() throws PLCCompilerException {
		String input = """

				""";
		assertThrows(SyntaxException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});		// throws a general compiler exception due to having no input.
	}

	@Test
	void test31() throws PLCCompilerException {
		String input = """
				b + + 2
				""";
		assertThrows(SyntaxException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});	// throws an exception because no righthand ident or num is provided
	}

	@Test
	void test32() throws PLCCompilerException {
		String input = """
				3 @ 4
				""";
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});	// throws an exception from lexer because @ is disallowed.
	}

}
