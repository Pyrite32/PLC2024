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

	LexerState currentState;
	LexerState previousState;

	public Lexer(String input) {
		// input is outside-immutable.
		// it consists of all the code
		// next() enumerates through all the lexed tokens
		// islandize possible Lexer inputs
		this.input = input;

		LexicalStructure.initializeLexicalStructure();
		lexibles = new ArrayList<LexibleCluster>();
		lexed = new LinkedList<Token>();

		currentState = LexerState.START;
		previousState = LexerState.START;

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
		int currentLine = 1;
		int currentColumn = 1;
		char previousChar = input.charAt(0);
		boolean isInComment = false;
		boolean isInString = false;

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
					contents = contents.replace("\r", "").replace("\n", "");
					SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
					// tellVar("Whitespace Lexible", lexible);

					LexibleCluster lexible = new LexibleCluster(contents, sloc);
					lexibles.add(lexible);
				}
				if (LexicalStructure.isNewLine(thisChar)) {
					currentLine += 1;
					currentColumn = 1;
				}
				anchorLeft = anchorRight;

				// tell("now whitespace");
			}
			// if JUST became comment
			if (LexicalStructure.isCommentChar(previousChar) &&
					LexicalStructure.isCommentChar(thisChar) &&
					!isInString) {
				// add lexible
				if (anchorRight - anchorLeft - 1 > 0) {
					String contents = input.substring(anchorLeft, anchorRight - 1);
					contents = contents.replace("\r", "").replace("\n", "");
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
			if (LexicalStructure.isStringChar(thisChar) && !isInComment) {
				isInString = !isInString;
				if (!isInString) {
					if (anchorRight - anchorLeft - 1 > 0) {
						String contents = input.substring(anchorLeft, anchorRight+1);
						contents = contents.replace("\r", "").replace("\n", "");
						SourceLocation sloc = new SourceLocation(currentLine, currentColumn);
						
						LexibleCluster lexible = new LexibleCluster(contents, sloc);
						tellVar("string Lexible", lexible);
						lexibles.add(lexible);
						anchorRight += 1;
						anchorLeft = anchorRight;
					}
				}
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
				tell("lexical error from OtherChar:" + res.source.toString());
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
		tellVar("current cluster", current);

		// start column is
		int islandStartLine = currentCluster.location().line();
		int islandStartColumn = currentCluster.location().column();

		while (currentColumnRight < current.length()) {
			char currentChar = current.charAt(currentColumnRight);
			int col = islandStartColumn + currentColumnRight;
			SourceLocation loc = new SourceLocation(islandStartLine, islandStartColumn + currentColumnLeft);
			// increment here so that when the loop is exited,
			//
			currentColumnRight += 1;

			// determine change in state;
			tellVar("currentChar before state-change", currentChar);
			LexerState currentState = determineStateSwitch(
					previousState,
					loc,
					currentChar);
			tellVar("old state", previousState);
			tellVar("new-applied state", currentState);
			if (currentState == LexerState.START) {
				// current column right is the next-in-line column.
				// can't include the newest character that breaks the flow.
				String subrange = current.substring(currentColumnLeft, currentColumnRight - 1);
				tellVar("subrange", subrange);
				// need to allow the state-changing character to be processed on its own.
				currentColumnRight -= 1;
				currentColumnLeft = currentColumnRight;
				

				// requires a special intervention:
				if (previousState == LexerState.OTHERCHAR) {
					processOtherCharClusters(subrange, loc);
					// prepare for next()
					// get me the first lexed OtherCharCluster NOW.
					result = lexed.remove();
				} else if (previousState == LexerState.STRING) {
					// the terminating string character cannot be considered.
					currentColumnRight += 1;
					currentColumnLeft += 1;

					// subrange does not include the " character.
					subrange += LexicalStructure.StringDelimiter;
					return convertRangeToToken(
							previousState,
							subrange,
							islandStartColumn + current.length(),
							loc);
				} else {
					result = convertRangeToToken(previousState, subrange, col, loc);
				}
				currentState = determineStateSwitch(
					previousState,
					loc,
					currentChar);
				previousState = LexerState.START;
				return result;
				// anchor the state.

			} else if (currentState == LexerState.ZERO) {
				currentColumnLeft = currentColumnRight;
				// instantly terminate this token!
				tell("lexing a 0 token!");
				return new Token(
						Kind.NUM_LIT,
						col,
						1,
						"0",
						loc);
				// instantly convert and recieve and convert '0' token.
			}

			previousState = currentState;

		}
		tell("THE LOOP HAS BEEN EXITED!!!!!");

		////////////////////////////////////
		// handle dangling lexable range: //
		////////////////////////////////////

		String subrange = current.substring(currentColumnLeft, current.length());

		currentColumnRight = 0;
		currentColumnLeft = 0;
		currentLexibleIndex++;

		if (subrange == "") {
			// dangerous??????
			tell("calling next() recursively");
			previousState = currentState;
			return next();
		} else {
			tellVar("dangling subrange", subrange);
			currentColumnLeft = currentColumnRight;

			if (previousState == LexerState.OTHERCHAR) {
				processOtherCharClusters(
						subrange,
						new SourceLocation(islandStartLine, islandStartColumn + current.length()-1));
				result = lexed.remove();
			} else if (previousState == LexerState.STRING) {
				subrange += LexicalStructure.StringDelimiter;
				result = convertRangeToToken(
						previousState,
						subrange,
						islandStartColumn + current.length(),
						new SourceLocation(islandStartLine, islandStartColumn + current.length()-1));
			}
			// all other tokens are predictable in nature.
			else {
				result = convertRangeToToken(
						previousState,
						subrange,
						islandStartColumn + current.length(),
						new SourceLocation(islandStartLine, islandStartColumn + current.length()-1));
			}
			previousState = currentState;
			return result;
		}
	}

	private void processOtherCharClusters(String source, SourceLocation location) {
		tell("call processOtherCharClusters() with :" + source);
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
				Kind kindMatch = LexicalStructure.getKindFromExact(currentTwoChars);
				// enqueue
				if (kindMatch != Kind.ERROR && kindMatch != null) {
					lexed.add(
							new Token(kindMatch,
									location.column() + i,
									2,
									currentTwoChars,
									new SourceLocation(location.line(), location.column()+i)));
					i += 2;
					tellVar("two-char match", currentTwoChars);
					doubleMatchFailed = false;
				} else
					doubleMatchFailed = true;
			}
			if (doubleMatchFailed) {
				String currentChar = "" + source.charAt(i);
				Kind kindMatch = LexicalStructure.getKindFromExact(currentChar);
				if (kindMatch != Kind.ERROR && kindMatch != null) {
					lexed.add(
							new Token(kindMatch,
									location.column() + i,
									1,
									currentChar,
									new SourceLocation(location.line(), location.column()+i)));
					tellVar("one-char-match found!", currentChar);
				} else {
					// erroneous token detected!
					lexed.add(
							new Token(Kind.ERROR,
									location.column() + i,
									1,
									currentChar,
									new SourceLocation(location.line(), location.column()+i)));
					tellVar("bad other-char", currentChar);
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
		tell("call convertRangeToToken() with :" + string);
		tellVar("this state", oldState.toString());
		// try to match reserved keywords and constants first.
		Kind tokenType = LexicalStructure.getKindFromExact(string);
		// match immediate types
		if (tokenType != null && tokenType != Kind.ERROR) {
			tell("return matched type:" + tokenType.toString());
			return new Token(tokenType,
					position,
					string.length(),
					string,
					location);
		}
		// types with more rules: Identifiers
		else if (oldState == LexerState.ALPHA_ONLY ||
				oldState == LexerState.ALPHANUMERIC) {
			tell("returning identifier :" + string);
			return new Token(Kind.IDENT,
					position,
					string.length(),
					string,
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
			tell("returning new numeral :" + string);
			return new Token(Kind.NUM_LIT,
					position,
					string.length(),
					string,
					location);
		} else if (oldState == LexerState.STRING) {
			tell("returning new string literal :" + string);

			return new Token(Kind.STRING_LIT,
					position,
					string.length(),
					string,
					location);
		}
		tell("returning new erroneous token");
		return new Token(Kind.ERROR, 0, 0, null, new SourceLocation(1, 1));
	}

	private LexerState determineStateSwitch(
			LexerState start,
			SourceLocation location,
			char current) throws LexicalException {

		// handle immediate potential errors:

		// complete comments are totally ignored.
		// that means incomplete comments must throw errors.
		if (LexicalStructure.isIllegal(current) && start != LexerState.STRING) {
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
				else if (LexicalStructure.isIdentifierPrefix(current))
					return LexerState.IDENTIFIER;
				return LexerState.OTHERCHAR;
			}
		} else if (start == LexerState.ALPHA_ONLY) {
			if (Character.isAlphabetic(current))
				return LexerState.ALPHA_ONLY;
			if (Character.isDigit(current))
				return LexerState.ALPHANUMERIC;
		} else if (start == LexerState.ALPHANUMERIC) {
			if (Character.isAlphabetic(current) ||
					Character.isDigit(current)) {
				return LexerState.ALPHANUMERIC;
			}
		} else if (start == LexerState.STRING) {
			tell("string state");
			if (current != LexicalStructure.StringDelimiter)
				return LexerState.STRING;
			;
		} else if (start == LexerState.IDENTIFIER) {
			if (Character.isAlphabetic(current) || Character.isDigit(current))
				return LexerState.IDENTIFIER;
		} else if (start == LexerState.OTHERCHAR) {
			if (LexicalStructure.isOtherChar(current))
				return LexerState.OTHERCHAR;
		} else if (start == LexerState.NUMERAL) {
			if (Character.isDigit(current))
				return LexerState.NUMERAL;
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
