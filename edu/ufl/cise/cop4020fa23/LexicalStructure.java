package edu.ufl.cise.cop4020fa23;

import java.util.HashMap;

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

public final class LexicalStructure {

    private static HashMap<String, Kind> alphabeticLiterals;

    // only one length
    private static HashMap<String, Kind> oneCharLiterals;

    // only two length
    private static HashMap<String, Kind> twoCharLiterals;

    public static final void initializeLexicalStructure() {

        alphabeticLiterals = new HashMap<String, Kind>();
        oneCharLiterals = new HashMap<String, Kind>();
        twoCharLiterals = new HashMap<String, Kind>();

        // boolean literal
        for (String s : LexicalStructure.BooleanLit) {
            alphabeticLiterals.put(s, Kind.BOOLEAN_LIT);
        }
        // constant literal
        for (String s : LexicalStructure.Constants) {
            alphabeticLiterals.put(s, Kind.CONST);
        }

        // reserved cases
        alphabeticLiterals.put(LexicalStructure.RES_Image, Kind.RES_image);
        alphabeticLiterals.put(LexicalStructure.RES_Pixel, Kind.RES_pixel);
        alphabeticLiterals.put(LexicalStructure.RES_Int, Kind.RES_int);
        alphabeticLiterals.put(LexicalStructure.RES_String, Kind.RES_string);
        alphabeticLiterals.put(LexicalStructure.RES_Void, Kind.RES_void);
        alphabeticLiterals.put(LexicalStructure.RES_Boolean, Kind.RES_boolean);
        alphabeticLiterals.put(LexicalStructure.RES_Write, Kind.RES_write);
        alphabeticLiterals.put(LexicalStructure.RES_Height, Kind.RES_height);
        alphabeticLiterals.put(LexicalStructure.RES_Width, Kind.RES_width);
        alphabeticLiterals.put(LexicalStructure.RES_If, Kind.RES_if);
        alphabeticLiterals.put(LexicalStructure.RES_Fi, Kind.RES_fi);
        alphabeticLiterals.put(LexicalStructure.RES_Do, Kind.RES_do);
        alphabeticLiterals.put(LexicalStructure.RES_Od, Kind.RES_od);
        alphabeticLiterals.put(LexicalStructure.RES_Red, Kind.RES_red);
        alphabeticLiterals.put(LexicalStructure.RES_Green, Kind.RES_green);
        alphabeticLiterals.put(LexicalStructure.RES_Blue, Kind.RES_blue);

        // one-letter ops
        oneCharLiterals.put(LexicalStructure.Comma, Kind.COMMA);
        oneCharLiterals.put(LexicalStructure.Semi, Kind.SEMI);
        oneCharLiterals.put(LexicalStructure.Question, Kind.QUESTION);
        oneCharLiterals.put(LexicalStructure.Colon, Kind.COLON);
        oneCharLiterals.put(LexicalStructure.LParen, Kind.LPAREN);
        oneCharLiterals.put(LexicalStructure.RParen, Kind.RPAREN);
        oneCharLiterals.put(LexicalStructure.LT, Kind.LT);
        oneCharLiterals.put(LexicalStructure.GT, Kind.GT);
        oneCharLiterals.put(LexicalStructure.LSquare, Kind.LSQUARE);
        oneCharLiterals.put(LexicalStructure.RSquare, Kind.RSQUARE);
        oneCharLiterals.put(LexicalStructure.Assign, Kind.ASSIGN);
        oneCharLiterals.put(LexicalStructure.Bang, Kind.BANG);
        oneCharLiterals.put(LexicalStructure.BitAnd, Kind.BITAND);
        oneCharLiterals.put(LexicalStructure.BitOr, Kind.BITOR);
        oneCharLiterals.put(LexicalStructure.Plus, Kind.PLUS);
        oneCharLiterals.put(LexicalStructure.Minus, Kind.MINUS);
        oneCharLiterals.put(LexicalStructure.Times, Kind.TIMES);
        oneCharLiterals.put(LexicalStructure.Div, Kind.DIV);
        oneCharLiterals.put(LexicalStructure.Mod, Kind.MOD);
        oneCharLiterals.put(LexicalStructure.Return, Kind.RETURN);

        // two-letter ops
        twoCharLiterals.put(LexicalStructure.Eq, Kind.EQ);
        twoCharLiterals.put(LexicalStructure.Le, Kind.LE);
        twoCharLiterals.put(LexicalStructure.Ge, Kind.GE);
        twoCharLiterals.put(LexicalStructure.And, Kind.AND);
        twoCharLiterals.put(LexicalStructure.Or, Kind.OR);
        twoCharLiterals.put(LexicalStructure.Exp, Kind.EXP);
        twoCharLiterals.put(LexicalStructure.BlockOpen, Kind.BLOCK_OPEN);
        twoCharLiterals.put(LexicalStructure.BlockClose, Kind.BLOCK_CLOSE);
        twoCharLiterals.put(LexicalStructure.RArrow, Kind.RARROW);
        twoCharLiterals.put(LexicalStructure.Box, Kind.BOX);
    }

    public static final String[] BooleanLit = {
            "TRUE",
            "FALSE"
    };
    public static final String[] Constants = {
            "Z",
            "BLACK",
            "BLUE",
            "CYAN",
            "DARK_GRAY",
            "GRAY",
            "GREEN",
            "LIGHT_GRAY",
            "MAGENTA",
            "ORANGE",
            "PINK",
            "RED",
            "WHITE",
            "YELLOW"
    };
    // any length

    public static final String RES_Image = "image";
    public static final String RES_Pixel = "pixel";
    public static final String RES_Int = "int";
    public static final String RES_String = "string";
    public static final String RES_Void = "void";
    public static final String RES_Boolean = "boolean";
    public static final String RES_Write = "write";
    public static final String RES_Height = "height";
    public static final String RES_Width = "width";
    public static final String RES_If = "if";
    public static final String RES_Fi = "fi";
    public static final String RES_Do = "do";
    public static final String RES_Od = "od";
    public static final String RES_Red = "red";
    public static final String RES_Green = "green";
    public static final String RES_Blue = "blue";

    public static final String Comma = ",";
    public static final String Semi = ";";
    public static final String Question = "?";
    public static final String Colon = ":";
    public static final String LParen = "(";
    public static final String RParen = ")";
    public static final String LT = "<";
    public static final String GT = ">";
    public static final String LSquare = "[";
    public static final String RSquare = "]";
    public static final String Assign = "=";
    public static final String Bang = "!";
    public static final String BitAnd = "&";
    public static final String BitOr = "|";
    public static final String Plus = "+";
    public static final String Minus = "-";
    public static final String Times = "*";
    public static final String Div = "/";
    public static final String Mod = "%";
    public static final String Return = "^";

    public static final String Eq = "==";
    public static final String Le = "<=";
    public static final String Ge = ">=";
    public static final String And = "&&";
    public static final String Or = "||";
    public static final String Exp = "**";
    public static final String BlockOpen = "<:";
    public static final String BlockClose = ":>";
    public static final String RArrow = "->";
    public static final String Box = "[]";
    public static final char IncompleteComment = '#';
    public static final char StringDelimiter = '\"';
    public static final char IdentDelimiter = '_';
    public static final char Illegal = '@';

    public static final boolean isWhiteSpace(char character) {
        return (character == ' ' || character == '\n' || character == '\r');
    }

    public static final Kind getKindFromExact(String string) {
        Kind result = alphabeticLiterals.get(string);
        if (result != null)
            return result;
        result = oneCharLiterals.get(string);
        if (result != null)
            return result;
        result = twoCharLiterals.get(string);
        return result;
    }

    public static final boolean isCRLF(char character) {
        return (character == '\r' || character == '\n');
    }

    public static final boolean isNewLine(char character) {
        return (character == '\n');
    }

    public static final boolean isUnprintable(char character) {
        return (int) character < 32;
    }

    public static final boolean isCommentChar(char character) {
        return character == LexicalStructure.IncompleteComment;
    }

    public static final boolean isStringChar(char character) {
        return character == LexicalStructure.StringDelimiter;
    }

    public static final boolean isIllegal(char character) {
        return character == LexicalStructure.Illegal || character == LexicalStructure.IncompleteComment;
    }

    public static final boolean isIdentifierPrefix(char character) {
        return character == LexicalStructure.IdentDelimiter;
    }

    public static final boolean isOtherChar(char character) {
        return !Character.isAlphabetic(character) && !Character.isDigit(character);
    }

    public static String kind2Char(Kind kind) throws LexicalException {
        switch (kind) {
            case LE:
                return LexicalStructure.Le;
            case BANG:
                return LexicalStructure.Bang;
            case LPAREN:
                return LexicalStructure.LParen;
            case LSQUARE:
                return LexicalStructure.LSquare;
            case LT:
                return LexicalStructure.LT;
            case GT:
                return LexicalStructure.GT;
            case GE:
                return LexicalStructure.GT;
            case MINUS:
                return LexicalStructure.Minus;
            case MOD:
                return LexicalStructure.Mod;
            case OR:
                return LexicalStructure.Or;
            case PLUS:
                return LexicalStructure.Plus;
            case QUESTION:
                return LexicalStructure.Question;
            case RARROW:
                return LexicalStructure.RArrow;
            case RES_blue:
                return LexicalStructure.RES_Blue;
            case RES_boolean:
                return LexicalStructure.RES_Boolean;
            case RES_do:
                return LexicalStructure.RES_Do;
            case RES_fi:
                return LexicalStructure.RES_Fi;
            case RES_green:
                return LexicalStructure.RES_Green;
            case RES_height:
                return LexicalStructure.RES_Height;
            case RES_if:
                return LexicalStructure.RES_If;
            case RES_image:
                return LexicalStructure.RES_Image;
            case RES_int:
                return LexicalStructure.RES_Int;
            case RES_nil:
                return "null";
            case RES_od:
                return LexicalStructure.RES_Od;
            case RES_pixel:
                return LexicalStructure.RES_Pixel;
            case RES_red:
                return LexicalStructure.RES_Red;
            case RES_string:
                return LexicalStructure.RES_String;
            case RES_void:
                return LexicalStructure.RES_Void;
            case RES_width:
                return LexicalStructure.RES_Width;
            case RES_write:
                return LexicalStructure.RES_Height;
            case RETURN:
                return LexicalStructure.Return;
            case RPAREN:
                return LexicalStructure.RParen;
            case RSQUARE:
                return LexicalStructure.RSquare;
            case SEMI:
                return LexicalStructure.Semi;
            case TIMES:
                return LexicalStructure.Times;
            case AND:
                return LexicalStructure.And;
            case ASSIGN:
                return LexicalStructure.Assign;
            case BITAND:
                return LexicalStructure.BitAnd;
            case BITOR:
                return LexicalStructure.BitOr;
            case BLOCK_CLOSE:
                return LexicalStructure.BlockClose;
            case BLOCK_OPEN:
                return LexicalStructure.BlockOpen;
            case BOX:
                return LexicalStructure.Box;
            case COLON:
                return LexicalStructure.Colon;
            case COMMA:
                return LexicalStructure.Comma;
            case DIV:
                return LexicalStructure.Div;
            case EQ:
                return LexicalStructure.Eq;
            case EXP:
                return LexicalStructure.Exp;
            default:
                throw new LexicalException("invalid type provided for string conversion-- do it yourself!");
        }
    }
}
