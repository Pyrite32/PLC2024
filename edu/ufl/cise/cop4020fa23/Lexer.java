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

import javax.xml.transform.Source;

public class Lexer implements ILexer {

	String input;
	int currentLexibleIndex = 0;

	private ArrayList<LexibleCluster> lexibles;
	private Queue<Token> lexed;

	int currentLine = 0;
	int currentColumnRight = 0;
	int currentColumnLeft = 0;

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

		// start column is
		int islandStartLine = currentCluster.location().line();
		int islandStartColumn = currentCluster.location().column() + currentColumnRight;

		LexerState previousState = LexerState.START;

		while (currentColumnRight < current.length()) {
			char currentChar = current.charAt(currentColumnRight);
			int col = islandStartColumn + currentColumnRight;
			SourceLocation loc = new SourceLocation(islandStartLine, col);
			// increment here to prevent infinite loop with '0'.
			// col reflects the current situation
			// currentColumn reflects the future situation.
			currentColumnRight += 1;

			// determine change in state;
			LexerState currentState = determineStateSwitch(
					previousState,
					new SourceLocation(currentLine, col),
					currentChar);
			if (currentState == LexerState.START) {
				// current column right is not the current column.
				String subrange = current.substring(currentColumnLeft, currentColumnRight);
				currentColumnLeft = currentColumnRight;

				// requires a special intervention:
				if (previousState == LexerState.OTHERCHAR) {
					// most advanced procedure:
					processOtherCharClusters(
							subrange,
							new SourceLocation(islandStartLine, col));
					// get me the first lexed OtherCharCluster NOW.
					return lexed.remove();
				}
				// all other tokens are predictable in nature.
				else {
					return convertRangeToToken(previousState, subrange, col, loc);
				}
				// anchor the state.
			} else if (currentState == LexerState.ZERO) {
				// instantly terminate this token!
				result = new Token(
						Kind.NUM_LIT,
						col,
						1,
						new char['0'],
						new SourceLocation(islandStartLine, col));
				break;
				// instantly convert and recieve and convert '0' token.
			}

			previousState = currentState;

		}
		currentColumnRight = 0;
		currentLexibleIndex++;

		////////////////////////////////////
		// handle dangling lexable range: //
		////////////////////////////////////
		
		String subrange = current.substring(currentColumnLeft, current.length());
		currentColumnLeft = currentColumnRight;

		if (previousState == LexerState.OTHERCHAR) {
			processOtherCharClusters(
					subrange,
					new SourceLocation(islandStartLine, islandStartColumn + current.length()));
			result = lexed.remove();
		}
		// all other tokens are predictable in nature.
		else {
			result = convertRangeToToken(
				previousState,
				subrange,
				islandStartColumn + current.length(),
				new SourceLocation(islandStartLine, islandStartColumn + current.length()));
		}

		///////////////////////////////////
		// handle dangling lexable range //
		///////////////////////////////////

		return result;
	}

	private void processOtherCharClusters(String source, SourceLocation location) {
		// OtherChar clusters are only 1-2 chars long.
		// perform search for 2 char match first, then 1 char match.
		// add match to queue.
		// repeat until string is gone.
		// parse 'junk' into error token.
		int i = 0;
		while (i < source.length()) {
			// double-matching available
			boolean doubleMatchFailed = true;
			if (i + 1 < source.length()) {
				// double match
				String currentTwoChars = source.charAt(i) + "" + source.charAt(i + 1);
				Kind kindMatch = LexicalStructure.getKindFromExact(
						currentTwoChars);
				// enqueue
				if (kindMatch != Kind.ERROR) {
					lexed.add(
							new Token(kindMatch,
									location.column() + i,
									2,
									currentTwoChars.toCharArray(),
									location));
					i += 2;
					doubleMatchFailed = false;
				} else
					doubleMatchFailed = true;
			}
			if (doubleMatchFailed) {
				String currentChar = "" + source.charAt(i);
				Kind kindMatch = LexicalStructure.getKindFromExact(currentChar);
				if (kindMatch != Kind.ERROR) {
					lexed.add(
							new Token(kindMatch,
									location.column() + i,
									1,
									currentChar.toCharArray(),
									location));
				} else {
					// erroneous token detected!
					lexed.add(
							new Token(Kind.ERROR,
									location.column() + i,
									1,
									currentChar.toCharArray(),
									location));
				}
				i += 1;
			}
		}

	}

	private Token convertRangeToToken(
			LexerState oldState,
			String string,
			int position,
			SourceLocation location)
			throws LexicalException {

		// try to match reserved keywords and constants first.
		Kind tokenType = LexicalStructure.getKindFromExact(string);
		// match immediate types
		if (tokenType != Kind.ERROR) {
			return new Token(tokenType,
					position,
					string.length(),
					string.toCharArray(),
					location);
		}
		// types with more rules: Identifiers
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
		} else if (oldState == LexerState.STRING) {
			return new Token(Kind.STRING_LIT,
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
