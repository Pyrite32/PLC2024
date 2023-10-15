package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.Lexer.ProcessState;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import static edu.ufl.cise.cop4020fa23.Kind.EOF;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Lexer implements ILexer {

    private LinkedList<LexibleCluster> lexableIslands = new LinkedList<LexibleCluster>();

    boolean errf = false;
    String source;

    private Queue<Token> awaitTokens = new LinkedList<Token>();

    public Lexer(String input) {
        LexicalStructure.initializeLexicalStructure();
        removeWhitespace(input);
        source = input;
        for (var lexable : lexableIslands) {
            System.out.println("island:" + lexable.toString());
        }
    }

    private enum LexerState {
        START,
        ALPHA_ONLY,
        ALPHANUMERIC,
        IDENTIFIER,
        ZERO,
        NUMERAL,
        // removed STRING because strings are always isolated.
        OTHERCHAR
    }

    @Override
    public IToken next() throws LexicalException {

        if (errf)
            throw new LexicalException(null, "Unterminated string!");
        if (awaitTokens.size() != 0) {
            return awaitTokens.remove();
        }
        if (lexableIslands.size() == 0) {
            return new Token(EOF, 0, 0, "", new SourceLocation(1, 1));
        }
        
        
        var currentIsland = lexableIslands.pop();

        // return string literal
        if (currentIsland.contents().charAt(0) == '\"') {
            return new Token(Kind.STRING_LIT, 0, currentIsland.contents().length(), currentIsland.contents(),
                    currentIsland.location());
        }

        

        LexerState initialState = LexerState.START;
        LexerState newState = LexerState.START;
        String currentStateChars = "";
        var islandString = currentIsland.contents();

        for (int i = 0; i < islandString.length(); i++) {
            char currentChar = islandString.charAt(i);
            newState = getStateChange(initialState, currentChar);
            if (newState != initialState && newState == LexerState.START) {
                i--; // because when the state changes, the new state-changing token isn't properly accomodated.
                switch (initialState) {
                    case ALPHANUMERIC, ALPHA_ONLY, IDENTIFIER:
                        processAlphaNumToken(currentStateChars, i, initialState, currentIsland.location());
                        currentStateChars = "";
                        break;
                    case NUMERAL, ZERO:
                        awaitTokens.add(new Token(
                                Kind.NUM_LIT,
                                i,
                                currentStateChars.length(),
                                currentStateChars,
                                new SourceLocation(currentIsland.location().line(),
                                        currentIsland.location().column() + i)));
                        currentStateChars = "";
                        break;
                    case OTHERCHAR:
                        processOtherCharTokens(currentStateChars, currentIsland.location());
                        currentStateChars = "";
                        break;
                    default:
                        throw new LexicalException(null, "Initial state is ZERO!");
                }
            } else if (newState == LexerState.ZERO) {
                awaitTokens.add(new Token(
                        Kind.NUM_LIT,
                        i,
                        1,
                        "0",
                        new SourceLocation(currentIsland.location().line(),
                                currentIsland.location().column() + i)));
                newState = LexerState.START;
                currentStateChars = "";
            } else {
                currentStateChars += currentChar;
            }

            initialState = newState;
        }

        /////////////////
        /// LAST TOKENS!!
        /////////////////
        if (currentStateChars != "") {
            switch (initialState) {
            case ALPHANUMERIC, ALPHA_ONLY, IDENTIFIER, NUMERAL, ZERO:
                SourceLocation loc = new SourceLocation(currentIsland.location().line(), islandString.length() - currentStateChars.length() + currentIsland.location().column());
                processAlphaNumToken(currentStateChars, islandString.length() - currentStateChars.length(), newState, loc);
                currentStateChars = "";
                break;
            case OTHERCHAR:
                processOtherCharTokens(currentStateChars, currentIsland.location());
                currentStateChars = "";
                break;
            default:
                throw new LexicalException(null, "Initial state is ZERO!");
            }
        }
        
        if (awaitTokens.size() != 0) {
            return awaitTokens.remove();
        }

        return new Token(EOF, 0, 0, "", new SourceLocation(1, 1));
    }

    private void processAlphaNumToken(String source, int position, LexerState oldState, SourceLocation location) throws LexicalException {

        Kind tokenType = LexicalStructure.getKindFromExact(source);
        if (source == "_") {
            throw new LexicalException(location, source);
        }
        // match immediate types
        if (tokenType != null && tokenType != Kind.ERROR) {
            awaitTokens.add(new Token(tokenType,
                    position,
                    source.length(),
                    source,
                    location));
            return;
        }
        // types with more rules: Identifiers
        else if (oldState == LexerState.ALPHA_ONLY ||
                oldState == LexerState.ALPHANUMERIC ||
                oldState == LexerState.IDENTIFIER) {
             awaitTokens.add(new Token(Kind.IDENT,
                    position,
                    source.length(),
                    source,
                    location));
            return;
        }
        // attempt to parse a numeral
        else if (oldState == LexerState.NUMERAL) {
            try {
                Integer.parseInt(source);
            } catch (NumberFormatException e) {
                throw new LexicalException(location,
                        source + " can't be parsed into an int.");
            }
            awaitTokens.add(new Token(Kind.NUM_LIT,
                    position,
                    source.length(),
                    source,
                    location));
            return;
        }
        throw new LexicalException("The state is not Alpha only, alphanumeric, numeric, or an exact match: it is : " + oldState.toString());
    }

    private void processOtherCharTokens(String source, SourceLocation location) {
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
                    awaitTokens.add(
                            new Token(kindMatch,
                                    location.column() + i,
                                    2,
                                    currentTwoChars,
                                    new SourceLocation(location.line(), location.column() + i)));
                    i += 2;
                    doubleMatchFailed = false;
                } else
                    doubleMatchFailed = true;
            }
            if (doubleMatchFailed) {
                String currentChar = "" + source.charAt(i);
                Kind kindMatch = LexicalStructure.getKindFromExact(currentChar);
                if (kindMatch != Kind.ERROR && kindMatch != null) {
                    awaitTokens.add(
                            new Token(kindMatch,
                                    location.column() + i,
                                    1,
                                    currentChar,
                                    new SourceLocation(location.line(), location.column() + i)));
                } else {
                    // erroneous token detected!
                    awaitTokens.add(
                            new Token(Kind.ERROR,
                                    location.column() + i,
                                    1,
                                    currentChar,
                                    new SourceLocation(location.line(), location.column() + i)));
                }
                i += 1;
            }
        }

    }

    private LexerState getStateChange(LexerState currentState, char currentChar) throws LexicalException {
        if (LexicalStructure.isIllegal(currentChar) || LexicalStructure.isUnprintable(currentChar)) {
            throw new LexicalException(null, "This character is not a part of the grammar");
        }

        switch (currentState) {
            case START:
                if (Character.isAlphabetic(currentChar))
                    return LexerState.ALPHA_ONLY;
                else if (Character.isDigit(currentChar)) {
                    if (currentChar == '0')
                        return LexerState.ZERO;
                    else
                        return LexerState.NUMERAL;
                } else {
                    if (LexicalStructure.isIdentifierPrefix(currentChar))
                        return LexerState.IDENTIFIER;
                    else
                        return LexerState.OTHERCHAR;
                }
            case ALPHA_ONLY:
                if (Character.isAlphabetic(currentChar) || LexicalStructure.isIdentifierPrefix(currentChar))
                    return LexerState.ALPHA_ONLY;
                if (Character.isDigit(currentChar))
                    return LexerState.ALPHANUMERIC;
                break;
            case ALPHANUMERIC:
                if (Character.isAlphabetic(currentChar) ||
                        Character.isDigit(currentChar))
                    return LexerState.ALPHANUMERIC;
                break;
            case IDENTIFIER:
                if (Character.isAlphabetic(currentChar) || Character.isDigit(currentChar))
                    return LexerState.IDENTIFIER;
                break;
            case OTHERCHAR:
                if (LexicalStructure.isOtherChar(currentChar))
                    return LexerState.OTHERCHAR;
                break;
            case NUMERAL:
                if (Character.isDigit(currentChar))
                    return LexerState.NUMERAL;
                break;
            default:
                return LexerState.START;
        }
        return LexerState.START;
    }

    enum ProcessState {
        NORMAL,
        STRING,
        COMMENT
    }

    private void removeWhitespace(String input) {
        // hello there "hello string" ## this is a comment " hello !"
        // hello, there, "hello string"

        // first, separate

        var currentState = ProcessState.NORMAL;

        String cluster = "";
        int sourceColumn = 1;
        int sourceRow = 1;

        for (int i = 0; i < input.length(); i++) {
            var currentChar = input.charAt(i);

            switch (currentState) {
                case NORMAL:
                    if (currentChar == '"') {
                        currentState = ProcessState.STRING;
                        if (cluster != "")
                            lexableIslands
                                    .add(new LexibleCluster(cluster, new SourceLocation(sourceRow, sourceColumn)));
                        cluster = "";
                        break;
                    }
                    if (currentChar == '#' && i < input.length() - 1 && input.charAt(i + 1) == '#') {
                        currentState = ProcessState.COMMENT;
                        if (cluster != "")
                            lexableIslands
                                    .add(new LexibleCluster(cluster, new SourceLocation(sourceRow, sourceColumn)));
                        cluster = "";
                        break;
                    }
                    if (currentChar == ' ' || currentChar == '\n' || currentChar == '\r') {
                        if (cluster != "")
                            lexableIslands.add(new LexibleCluster(cluster,
                                    new SourceLocation(sourceRow, sourceColumn - cluster.length())));
                        cluster = "";
                        break;
                    }
                    cluster += currentChar;
                    break;
                case STRING:
                    if (currentChar == '"') {
                        currentState = ProcessState.NORMAL;
                        lexableIslands.add(new LexibleCluster('"' + cluster + '"',
                                new SourceLocation(sourceRow, sourceColumn - cluster.length() - 1)));
                        cluster = "";
                        break;
                    }
                    cluster += currentChar;
                    break;
                case COMMENT:
                    if (currentChar == '\n') {
                        currentState = ProcessState.NORMAL;
                    }
                    break;
            }
            if (currentChar == '\n') {
                sourceColumn = 1;
                sourceRow += 1;
            } else
                sourceColumn += 1;
        }
        if (currentState == ProcessState.STRING) {
            errf = true;
        }
        if (cluster != "")
            lexableIslands
                    .add(new LexibleCluster(cluster, new SourceLocation(sourceRow, sourceColumn - cluster.length())));

        // removes comments
        // removes whitespace
        // removes space

    }

}
