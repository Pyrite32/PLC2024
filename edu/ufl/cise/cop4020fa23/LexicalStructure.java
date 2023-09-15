package edu.ufl.cise.cop4020fa23;

public final class LexicalStructure {
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

    public static final boolean isWhiteSpace(char character) {
        return (character == ' ' || character == '\n' || character == '\r');
    }

    public static final boolean isCRLF(char character) {
        return (character == '\r' || character == '\n');
    }

     public static final boolean isCommentChar(char character) {
        return character == '#';
    }
    public static final String[] CommentDelimiter = {
        "##"
    };
}
