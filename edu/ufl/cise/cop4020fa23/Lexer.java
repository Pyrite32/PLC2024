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
import java.util.LinkedList;
import java.util.Queue;


public class Lexer implements ILexer {

	String input;
	int currentLexibleIndex = 0;

	private ArrayList<LexibleCluster> lexibles;
	private Queue<Token> lexed;

	int currentLine = 0;
	int currentColumn = 0;

	public Lexer(String input) {
		// input is outside-immutable.
		// it consists of all the code
		// next() enumerates through all the lexed tokens
		// islandize possible Lexer inputs
		this.input = input;

		lexibles = new ArrayList<LexibleCluster>();
		lexed = new LinkedList<Token>();
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
					// ignore the whitespace character.
					String contents = input.substring(anchorLeft, anchorRight);
					SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
					// tellVar("Whitespace Lexible", lexible);

					LexibleCluster lexible = new LexibleCluster(contents, sloc);
					lexibles.add(lexible);
				}
				if (LexicalStructure.isNewLine(thisChar)) {
					currentLine += 1;
					currentColumn = 0;
				}
				anchorLeft = anchorRight;

				// tell("now whitespace");
			}
			// if JUST became comment
			if (LexicalStructure.isCommentChar(previousChar) &&
					LexicalStructure.isCommentChar(thisChar)) {
				// add lexible
				if (anchorRight - anchorLeft - 1 > 0) {
					String contents = input.substring(anchorLeft, anchorRight - 1);
					SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
					// tellVar("Whitespace Lexible", lexible);

					LexibleCluster lexible = new LexibleCluster(contents, sloc);
					lexibles.add(lexible);
				} else {
					// tell("rejected lexible in JUST invalid comments");
				}
				// go into comment mode.
				// tell("is now comment");
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
				// tell("is no longer comment");
				isInComment = false;
			}
			// if just exited whitespace
			else if (LexicalStructure.isWhiteSpace(previousChar) &&
					!LexicalStructure.isWhiteSpace(thisChar)) {
				anchorLeft += 1;
			} else {
				// assume valid;

			}
			// do this every time
			previousChar = thisChar;
			currentColumn += 1;
			anchorRight += 1;
		}
		// final lexible
		if (anchorRight - anchorLeft > 0) {
			String contents = input.substring(anchorLeft, anchorRight);
			// extra precaution
			contents = contents.replace("\n", "");
			contents = contents.replace("\r", "");
			contents = contents.replace(" ", "");
			if (!contents.isEmpty()) {
				SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
				LexibleCluster lexible = new LexibleCluster(contents, sloc);
				lexibles.add(lexible);
				// tellVar("Final Lexible", lexible);
			}
		}

		// tellVar("Lexibles Size",lexibles.size());

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

		// clusters of OtherChar lexibles need to be released
		// and throw errors in the correct order.
		if (!lexed.isEmpty()) {
			Token res = lexed.remove();
			if (res.kind == Kind.ERROR) {
				throw new LexicalException(
					res.sourceLocation(),
					"Some OtherChar Lexical Error with :"
					+ res.source.toString());
			}
			return res;
		}

		if (currentLexibleIndex >= lexibles.size()) {
			return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
		}
		Token result = null;
		LexibleCluster currentCluster = lexibles.get(currentLexibleIndex);
		String current = currentCluster.contents();
		int startLine = currentCluster.location().line();
		int startColumn = currentCluster.location().column() + currentColumn;

		LexerState previousState = LexerState.START;
		
		while (currentColumn < current.length()) {
			char currentChar = current.charAt(currentColumn);
			int col = startColumn+currentColumn;
			// increment here to prevent infinite loop with '0'.
			// col reflects the current situation
			// currentColumn reflects the future situation.
			currentColumn += 1;
			
			// determine change in state;
			LexerState currentState = determineStateSwitch(
					previousState,
					new SourceLocation(currentLine, col),
					currentChar);
			if (currentState == LexerState.START) {

				// react to important state change
				// most common state switch type

			} 
			else if (currentState == LexerState.ZERO) {
				result = new Token(
					Kind.NUM_LIT,
					col,
					1,
					new char['0'],
					new SourceLocation(startLine, col)
					);
				break;
				// instantly convert and recieve and convert '0' token.
			}

			previousState = currentState;

		}
		currentColumn = 0;
		return result;
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

	private Token convertRangeToToken(
			LexerState oldState,
			String string,
			int position,
			SourceLocation location)
			throws LexicalException {
		// clear most possible ranges
		Kind tokenType = LexicalStructure.getKindFromExact(string);
		// match immediate types
		if (tokenType != Kind.ERROR) {
			return new Token(tokenType,
					position,
					string.length(),
					string.toCharArray(),
					location);
		}

		else if (oldState == LexerState.ALPHA_ONLY ||
				oldState == LexerState.ALPHANUMERIC) {
			return new Token(Kind.IDENT,
					position,
					string.length(),
					string.toCharArray(),
					location);
		} 
		// attempt to parse a numeral
		else if (oldState == LexerState.NUMERAL) {
			try {
				Integer.parseInt(string);
			} catch (NumberFormatException e) {
				throw new LexicalException(location, 
						  string + " can't be parsed into an int.");
			}

			return new Token(Kind.NUM_LIT,
					position,
					string.length(),
					string.toCharArray(),
					location);
		}
		return new Token(Kind.ERROR, 0, 0, null, new SourceLocation(1, 1));
	}

	private LexerState determineStateSwitch(
			LexerState start,
			SourceLocation location,
			char current) throws LexicalException {

		// handle immediate potential errors:

		// complete comments are totally ignored.
		// that means incomplete comments must throw errors.
		if (LexicalStructure.isCommentChar(current)) {
			throw new LexicalException(location, "Incomplete comment declaration.");
		} else if (LexicalStructure.isUnprintable(current)) {
			throw new LexicalException(location, "Unprintable character found.");
		}

		if (start == LexerState.START) {
			// alpha, number, other
			if (Character.isAlphabetic(current))
				return LexerState.ALPHA_ONLY;
			else if (Character.isDigit(current))
				if (current == '0')
					return LexerState.ZERO;
				else
					return LexerState.NUMERAL;
			else {
				if (current == LexicalStructure.StringDelimiter)
					return LexerState.STRING;
				else if (current == LexicalStructure.IdentDelimiter)
					return LexerState.IDENTIFIER;
				return LexerState.OTHERCHAR;
			}
		} else if (start == LexerState.ALPHA_ONLY) {
			if (Character.isDigit(current))
				return LexerState.ALPHANUMERIC;
			return LexerState.START;
		} else if (start == LexerState.STRING) {
			if (current == LexicalStructure.StringDelimiter)
				return LexerState.START;
		} else if (start == LexerState.IDENTIFIER) {
			if (LexicalStructure.isOtherChar(current)) {
				return LexerState.START;
			}
		} else if (start == LexerState.OTHERCHAR) {
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
