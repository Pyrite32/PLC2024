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

import javax.lang.model.element.VariableElement;

public class Lexer implements ILexer {

	String input;

	HashMap<String, Kind> literalToKind;

	public Lexer(String input) {
		this.input = input;
		literalToKind = new HashMap<String, Kind>();
	}

	private void createLexicalStructure() {
		// boolean literal
		for (String s : LexicalStructure.BooleanLit) {
			literalToKind.put(s, Kind.BOOLEAN_LIT);
		}
		// constant literal
		for (String s : LexicalStructure.Constants) {
			literalToKind.put(s, Kind.CONST);
		}

	}

	@Override
	public IToken next() throws LexicalException {
		System.out.println("My contents are: ")
		return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
		printVar("input", input);
	}

	private void tellVar(VariableElement var) {

	}

	private static String getLoc() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            return stackTrace[2].getLineNumber();
        } else {
            return "?";
        }
    }


}
