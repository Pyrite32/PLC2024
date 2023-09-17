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
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import java.util.HashMap;
import java.util.ArrayList;


public class Lexer implements ILexer {

	String input;
	int currentLexibleIndex = 0;

	// any length
	private HashMap<String, Kind> literalToKindAny;

	// only one length
	private HashMap<String, Kind> literalToKindOne;

	// only two length
	private HashMap<String, Kind> literalToKindTwo;

	private ArrayList<LexibleCluster> lexibles;

	int currentLine = 0;
	int currentColumn = 0;

	public Lexer(String input) {
		// input is outside-immutable.
		// it consists of all the code
		// next() enumerates through all the lexed tokens
		// islandize possible Lexer inputs
		this.input = input;
		literalToKindAny = new HashMap<String, Kind>();
		literalToKindOne = new HashMap<String, Kind>();
		literalToKindTwo = new HashMap<String, Kind>();
		lexibles = new ArrayList<LexibleCluster>();
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
		int currentLine = 0;
		int currentColumn = 0;
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
					String contents = input.substring(anchorLeft, anchorRight);
					SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
					//tellVar("Whitespace Lexible", lexible);

					LexibleCluster lexible = new LexibleCluster(contents, sloc);
					lexibles.add(lexible);
				}
				if (LexicalStructure.isNewLine(thisChar)) {
					currentLine += 1;
					currentColumn = 0;
				}
				anchorLeft = anchorRight;

				//tell("now whitespace");
			}
			// if JUST became comment
			if (LexicalStructure.isCommentChar(previousChar) &&
				LexicalStructure.isCommentChar(thisChar)) {
				// add lexible
				if (anchorRight - anchorLeft - 1 > 0) {
					String contents = input.substring(anchorLeft, anchorRight - 1);
					SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
					//tellVar("Whitespace Lexible", lexible);

					LexibleCluster lexible = new LexibleCluster(contents, sloc);
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
			currentColumn += 1;
			anchorRight += 1;
		}
		// final lexible
		if (anchorRight - anchorLeft> 0) {
			String contents = input.substring(anchorLeft, anchorRight);
			// extra precaution
			contents = contents.replace("\n", "");
			contents = contents.replace("\r", "");
			contents = contents.replace(" ", "");
			if (!contents.isEmpty()) {
				SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
				LexibleCluster lexible = new LexibleCluster(contents, sloc);
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

	private enum LexerState {
		START,
		ALPHA_ONLY,
		ALPHANUMERIC,
		IDENTIFIER,
		ZERO,
		NUMERAL,
		STRING,
		OTHERCHAR
	}

	@Override
	public IToken next() throws LexicalException {
		if (currentLexibleIndex >= lexibles.size()) {
			return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
		}
		Token result = null;
		LexibleCluster currentCluster = lexibles.get(currentLexibleIndex);
		String current = currentCluster.contents();
		int currentLine = currentCluster.location().line();
		int currentColumn = currentCluster.location().column();

		LexerState previousState = LexerState.START;

		for (int i = 0; i < current.length(); i++ ) {
			char currentChar = current.charAt(i);
			// determine change in state;
			LexerState currentState = determineStateSwitch(
										previousState,
										new SourceLocation(currentLine, currentColumn),
										currentChar);
			if (currentState == LexerState.START) {
				// react to important state change


			}
			else if (currentState == LexerState.ZERO) {

			}



			previousState = currentState;

		}
		// retrieve state change
		// react when state has changed from anything other than start.
		// typically tokens are submitted when this happens.
		// if number goes to alphanum, then submit numeral.

		// split up cases:
		// numbers first -
			// if 0 first ret 0
			// else enum till non-digit
		// 



		return result;

	}

	private Token convertRangeToToken(String range) {

	}

	private Token filterAlphaOnlyByReserved() {
		// intercepts latest alpha token and replaces it with possible matches.
		return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
	}

	private Token filterOtherCharByReserved() {
		// intercepts latest other-char token and replaces it with possible matches.
		return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
	}

	private LexerState determineStateSwitch(
		LexerState start,
		SourceLocation location,
		char current
	) throws LexicalException {

		// handle immediate potential errors:

		// complete comments are totally ignored.
		// that means incomplete comments must throw errors.
		if (LexicalStructure.isCommentChar(current)) {
			throw new LexicalException(location, "Incomplete comment declaration.");
		}
		else if (LexicalStructure.isUnprintable(current)) {
			throw new LexicalException(location, "Unprintable character found.");
		}

		if (start == LexerState.START) {
			// alpha, number, other
			if (Character.isAlphabetic(current))
				return LexerState.ALPHA_ONLY;
			else if (Character.isDigit(current))
				if (current == '0')
					return LexerState.ZERO;
				else return LexerState.NUMERAL;
			else {
				if (current == LexicalStructure.StringDelimiter)
					return LexerState.STRING;
				else if (current == LexicalStructure.IdentDelimiter)
					return LexerState.IDENTIFIER;
				return LexerState.OTHERCHAR;
			}
		}
		else if (start == LexerState.ALPHA_ONLY) {
			if (Character.isDigit(current))
				return LexerState.ALPHANUMERIC;
			return LexerState.START;
		}
		else if (start == LexerState.STRING) {
			if (current == LexicalStructure.StringDelimiter)
				return LexerState.START;
		}
		else if (start == LexerState.IDENTIFIER) {
			if (LexicalStructure.isOtherChar(current)) {
				return LexerState.START;
			}
		}
		else if (start == LexerState.OTHERCHAR) {
			if (!LexicalStructure.isOtherChar(current))
				return LexerState.START;
		}
		return LexerState.START; 

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
