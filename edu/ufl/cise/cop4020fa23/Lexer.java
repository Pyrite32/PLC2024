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

import static edu.ufl.cise.cop4020fa23.Kind.EOF;
import edu.ufl.cise.cop4020fa23.LexicalStructure;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import java.util.HashMap;
import java.util.ArrayList;

import javax.lang.model.element.VariableElement;

public class Lexer implements ILexer {

	String input;
	int currentLexibleIndex = 0;

	// any length
	private HashMap<String, Kind> literalToKindAny;

	// only one length
	private HashMap<String, Kind> literalToKindOne;

	// only two length
	private HashMap<String, Kind> literalToKindTwo;

	private ArrayList<String> lexibles;

	SourceLocation location;

	public Lexer(String input) {
		// input is outside-immutable.
		// it consists of all the code
		// next() enumerates through all the lexed tokens
		// islandize possible Lexer inputs
		this.input = input;
		literalToKindAny = new HashMap<String, Kind>();
		literalToKindOne = new HashMap<String, Kind>();
		literalToKindTwo = new HashMap<String, Kind>();
		lexibles = new ArrayList<String>();
		createLexicalStructure();
		loadLexibles();
	}

	private void loadLexibles() {
		// discard anything that starts with a comment
		// discard whitespace;

		if (input.isEmpty()) {
			return;
		}
		
		int anchorLeft = 0;
		int anchorRight = 0;
		char previousChar = input.charAt(0);
		boolean isInComment = false;

		while (anchorRight < input.length()) {
			char thisChar = input.charAt(anchorRight);
			// if JUST became whitespace
			if (isInComment) {
				anchorLeft = anchorRight;
			}
			if ((!LexicalStructure.isWhiteSpace(previousChar) &&
				LexicalStructure.isWhiteSpace(thisChar))) {
				// add lexible
				if (anchorRight - anchorLeft > 0) {
					// 											ignore the whitespace character.
					String lexible = input.substring(anchorLeft, anchorRight);
					//tellVar("Whitespace Lexible", lexible);
					lexibles.add(lexible);
				}
				else {
					//tell("rejected lexible in JUST invalid whitespace");
				}
				anchorLeft = anchorRight;

				//tell("now whitespace");
			}
			// if JUST became comment
			if (LexicalStructure.isCommentChar(previousChar) &&
				LexicalStructure.isCommentChar(thisChar)) {
				// add lexible
				if (anchorRight - anchorLeft - 1 > 0) {
					String lexible = input.substring(anchorLeft, anchorRight - 1);
					//tellVar("Prev Comment Lexible", lexible);
					lexibles.add(lexible);
				}
				else {
					//tell("rejected lexible in JUST invalid comments");
				}
				// go into comment mode.
				//tell("is now comment");
				isInComment = true;
			}
			// if current in whitespace
			else if (LexicalStructure.isWhiteSpace(previousChar) &&
			 		 LexicalStructure.isWhiteSpace(thisChar)) {
				anchorLeft = anchorRight;
			}
			// invalidate comment mode
			else if (isInComment && !LexicalStructure.isCRLF(previousChar) && 
					 LexicalStructure.isCRLF(thisChar)) {
				//tell("is no longer comment");
				isInComment = false;
			}
			// if just exited whitespace
			else if (LexicalStructure.isWhiteSpace(previousChar) &&
					 !LexicalStructure.isWhiteSpace(thisChar)) {
				anchorLeft += 1;
			}
			else {
				// assume valid;

			}
			// do this every time
			previousChar = thisChar;
			anchorRight += 1;
		}
		// final lexible
		if (anchorRight - anchorLeft> 0) {
			String lexible = input.substring(anchorLeft, anchorRight);
			// extra precaution
			lexible = lexible.replace("\n", "");
			lexible = lexible.replace("\r", "");
			lexible = lexible.replace(" ", "");
			if (!lexible.isEmpty()) {
				lexibles.add(lexible);
				//tellVar("Final Lexible", lexible);
			}
		}

		//tellVar("Lexibles Size",lexibles.size());
		
	}

	private void createLexicalStructure() {
		// boolean literal
		for (String s : LexicalStructure.BooleanLit) {
			literalToKindAny.put(s, Kind.BOOLEAN_LIT);
		}
		// constant literal
		for (String s : LexicalStructure.Constants) {
			literalToKindAny.put(s, Kind.CONST);
		}

		// reserved cases
		literalToKindAny.put(LexicalStructure.RES_Image, Kind.RES_image);
		literalToKindAny.put(LexicalStructure.RES_Pixel, Kind.RES_pixel);
		literalToKindAny.put(LexicalStructure.RES_Int, Kind.RES_int);
		literalToKindAny.put(LexicalStructure.RES_String, Kind.RES_string);
		literalToKindAny.put(LexicalStructure.RES_Void, Kind.RES_void);
		literalToKindAny.put(LexicalStructure.RES_Boolean, Kind.RES_boolean);
		literalToKindAny.put(LexicalStructure.RES_Write, Kind.RES_write);
		literalToKindAny.put(LexicalStructure.RES_Height, Kind.RES_height);
		literalToKindAny.put(LexicalStructure.RES_Width, Kind.RES_width);
		literalToKindAny.put(LexicalStructure.RES_If, Kind.RES_if);
		literalToKindAny.put(LexicalStructure.RES_Fi, Kind.RES_fi);
		literalToKindAny.put(LexicalStructure.RES_Do, Kind.RES_do);
		literalToKindAny.put(LexicalStructure.RES_Od, Kind.RES_od);
		literalToKindAny.put(LexicalStructure.RES_Red, Kind.RES_red);
		literalToKindAny.put(LexicalStructure.RES_Green, Kind.RES_green);
		literalToKindAny.put(LexicalStructure.RES_Blue, Kind.RES_blue);

		// one-letter ops
		literalToKindOne.put(LexicalStructure.Comma, Kind.COMMA);
		literalToKindOne.put(LexicalStructure.Semi, Kind.SEMI);
		literalToKindOne.put(LexicalStructure.Question, Kind.QUESTION);
		literalToKindOne.put(LexicalStructure.Colon, Kind.COLON);
		literalToKindOne.put(LexicalStructure.LParen, Kind.LPAREN);
		literalToKindOne.put(LexicalStructure.RParen, Kind.RPAREN);
		literalToKindOne.put(LexicalStructure.LT, Kind.LT);
		literalToKindOne.put(LexicalStructure.GT, Kind.GT);
		literalToKindOne.put(LexicalStructure.LSquare, Kind.LSQUARE);
		literalToKindOne.put(LexicalStructure.RSquare, Kind.RSQUARE);
		literalToKindOne.put(LexicalStructure.Assign, Kind.ASSIGN);
		literalToKindOne.put(LexicalStructure.Bang, Kind.BANG);
		literalToKindOne.put(LexicalStructure.BitAnd, Kind.BITAND);
		literalToKindOne.put(LexicalStructure.BitOr, Kind.BITOR);
		literalToKindOne.put(LexicalStructure.Plus, Kind.PLUS);
		literalToKindOne.put(LexicalStructure.Minus, Kind.MINUS);
		literalToKindOne.put(LexicalStructure.Times, Kind.TIMES);
		literalToKindOne.put(LexicalStructure.Div, Kind.DIV);
		literalToKindOne.put(LexicalStructure.Mod, Kind.MOD);
		literalToKindOne.put(LexicalStructure.Return, Kind.RETURN);

		// two-letter ops
		literalToKindTwo.put(LexicalStructure.Eq, Kind.EQ);
		literalToKindTwo.put(LexicalStructure.Le, Kind.LE);
		literalToKindTwo.put(LexicalStructure.Ge, Kind.GE);
		literalToKindTwo.put(LexicalStructure.And, Kind.AND);
		literalToKindTwo.put(LexicalStructure.Or, Kind.OR);
		literalToKindTwo.put(LexicalStructure.Exp, Kind.EXP);
		literalToKindTwo.put(LexicalStructure.BlockOpen, Kind.BLOCK_OPEN);
		literalToKindTwo.put(LexicalStructure.BlockClose, Kind.BLOCK_CLOSE);
		literalToKindTwo.put(LexicalStructure.RArrow, Kind.RARROW);
		literalToKindTwo.put(LexicalStructure.Box, Kind.BOX);
	}

	@Override
	public IToken next() throws LexicalException {
		String currentLexible = lexibles.get(currentLexibleIndex);

		// process the longest possible lexibles first...
		// alphabeticals
		// numerals


		return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
	}




	////////////////
	// DEBUG VARS //
	////////////////


	private void tellVar(String name, Object var) {
		System.out.println(getLoc() + "value of " + name + " is :" + var.toString());
	}

	private void tell(String contents) {
		System.out.println(getLoc() + contents);
	}

	private static String getLoc() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		if (stackTrace.length >= 4) {
			String lineStr = Integer.toString(stackTrace[2].getLineNumber());
			String testStr = stackTrace[4].getMethodName();
			return " [" + testStr + "][Line " + lineStr + "] ";
		} else {
			return "[?]";
		}
	}

}
