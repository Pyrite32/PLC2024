package edu.ufl.cise.cop4020fa23;

// to combat the fact that all AST's are immutable by default.
// seriously, that has got to be the dumbest thing ever.
// Let me make changes to the AST's as I am building them! Absolutely ridiculous.

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

public class UnicornAST {
    public UnicornAST parent;
    public UnicornAST left;
    public UnicornAST middle;
    public UnicornAST right;
    public IToken operator;
    public IToken first;
    public int parenthesesLevel = 0;
    public boolean usePixelSelector = false;

    // determines where to hold off on in a PixelSelector, Cond, or ExpandedPixelSelector. comma's increment it. 
    // if a pixel sel has a sequential count of 2

    public int sequentialCount = 0;

    AST dataHead;

    public void incrementSequentialCount() throws SyntaxException
    {
        sequentialCount += 1;
        if (dataHead instanceof PixelSelector && sequentialCount == 2) {

        }
    }

    // only really needed with ExpandedPixelSel
    public void replaceMe(UnicornAST with) {
        if (parent == null) return;
        if (parent.left == this) {
            parent.left = with;
            with.left = left;
            with.middle = middle;
            with.right = right;
            with.sequentialCount = sequentialCount;
            with.first = first;
        }
        else if (parent.middle == this) {
            parent.middle = with;
            with.left = left;
            with.middle = middle;
            with.right = right;
            with.sequentialCount = sequentialCount;
            with.first = first;
        }
        else if (parent.right == this) {
            parent.right = with;
            with.left = left;
            with.middle = middle;
            with.right = right;
            with.sequentialCount = sequentialCount;
            with.first = first;
        }
    }

    public void mutinizeMe(UnicornAST with) {
        if (parent == null) return;
        if (parent.left == this) {

        }
        else if (parent.middle == this) {

        }
        else if (parent.right == this) {

        }
    }

    public int getSequentialCount() {
        return sequentialCount;
    }

    public UnicornAST(AST simple) {
        super();
        dataHead = simple;
        first = simple.firstToken;
    }

    public boolean isSyntaxResolved() {

    }

    public static AST buildAST(UnicornAST root) {
        if (root == null) {
            return null;
        }
        AST myDataHead = root.dataHead;
        if (myDataHead instanceof BooleanLitExpr  ||
            myDataHead instanceof ChannelSelector ||
            myDataHead instanceof ConstExpr       ||
            myDataHead instanceof IdentExpr       ||
            myDataHead instanceof NumLitExpr      ||
            myDataHead instanceof StringLitExpr) 
            {
            return myDataHead;
        }
        
        AST result = null;
        if (myDataHead instanceof BinaryExpr) {
            result = new BinaryExpr(root.first, (Expr)buildAST(root.left), root.operator, (Expr)buildAST(root.right));
        }
        else if (myDataHead instanceof ConditionalExpr) {
            result = new ConditionalExpr(root.first, (Expr)buildAST(root.left),(Expr)buildAST(root.middle),(Expr)buildAST(root.right));
        }
        else if (myDataHead instanceof ExpandedPixelExpr) {
             result = new ExpandedPixelExpr(root.first, (Expr)buildAST(root.left),(Expr)buildAST(root.middle),(Expr)buildAST(root.right));
        }
        else if (myDataHead instanceof PixelSelector) {
            return new PixelSelector(root.first, (Expr)buildAST(root.left), (Expr)buildAST(root.right));
        }
        else if (myDataHead instanceof PostfixExpr) {
            if (root.usePixelSelector)
            return new PostfixExpr(root.first, (Expr)buildAST(root.left), (PixelSelector)buildAST(root.right), null);
            else
            return new PostfixExpr(root.first, (Expr)buildAST(root.left), null, (ChannelSelector)buildAST(root.right));
        }
        else if (myDataHead instanceof UnaryExpr) {
            return new UnaryExpr(root.first, root.operator, (Expr)buildAST(root.left));
        }
        return result;

    }

    public boolean isBinaryExpr() {
        return (dataHead instanceof BinaryExpr);
    }
    public boolean isBooleanLitExpr() {
        return (dataHead instanceof BooleanLitExpr);
    }
    public boolean isChannelSelector() {
        return (dataHead instanceof ChannelSelector);
    }
    public boolean isConditionalExpr() {
        return (dataHead instanceof ConditionalExpr);
    }
    public boolean isConstExpr() {
        return (dataHead instanceof ConstExpr);
    }
    public boolean isExpandedPixelExpr() {
        return (dataHead instanceof ExpandedPixelExpr);
    }
    public boolean isIdentExpr() {
        return (dataHead instanceof IdentExpr);
    }
    public boolean isNumLitExpr() {
        return (dataHead instanceof NumLitExpr);
    }
    public boolean isPixelSelector() {
        return (dataHead instanceof PixelSelector);
    }
    public boolean isPostfixSelector() {
        return (dataHead instanceof PostfixExpr);
    }
    public boolean isStringLitExpr() {
        return (dataHead instanceof StringLitExpr);
    }
    public boolean isUnaryExpr() {
        return (dataHead instanceof UnaryExpr);
    }

    public static UnicornAST fromAtomStandalone(IToken token) {
        return switch (token.kind()) {
			case STRING_LIT -> new UnicornAST(new StringLitExpr(token));
			case NUM_LIT -> new UnicornAST(new NumLitExpr(token));
			case BOOLEAN_LIT -> new UnicornAST(new BooleanLitExpr(token));
			case IDENT -> new UnicornAST(new IdentExpr(token));
			case CONST -> new UnicornAST(new ConstExpr(token));
			default -> null;
		};
    }

    public static UnicornAST fromNewConditionalExpr(IToken first) {
        return new UnicornAST(new ConditionalExpr(first, null, null, null));
    }
    public static UnicornAST fromNewUnaryExpr(IToken first) {
        return new UnicornAST(new UnaryExpr(first, first, null));
    }

    public static ExpressionParserContext getFirstUnfinishedAST(UnicornAST root) throws SyntaxException {
        return _getFirstUnfinishedAST(root, null, 0, null);
    }

    private static ExpressionParserContext _getFirstUnfinishedAST(UnicornAST root, UnicornAST parent, int num, ExpressionParserContext res) throws SyntaxException {
        if (res != null) return res;
        if (root == null) {
            if (parent != null) {
                return new ExpressionParserContext(parent, num, false);
            }
            else { 
                throw new SyntaxException("root cannot be null when calling getFirstUnfinishedAST()");
            }
        }
        if (root.dataHead instanceof BooleanLitExpr ||
        root.dataHead instanceof ConstExpr	  ||
        root.dataHead instanceof IdentExpr	  ||
        root.dataHead instanceof StringLitExpr  ||
        root.dataHead instanceof NumLitExpr 	  ||
        root.dataHead instanceof ChannelSelector)
        {
            return new ExpressionParserContext(root.parent, num, root.parent.syntaxResolved);
        }

        if (root.isConditionalExpr() || root.isExpandedPixelExpr()) {
           res = _getFirstUnfinishedAST(root.left, root, num, res);
           if (!res.isComplete()) return res;
           res = _getFirstUnfinishedAST(root.middle, root, num, res);
           if (!res.isComplete()) return res;
           res = _getFirstUnfinishedAST(root.right, root, num, res);
           if (!res.isComplete()) return res;
        }
        if (root.isBinaryExpr() || root.isPixelSelector() || root.isPostfixSelector()) {
           res = _getFirstUnfinishedAST(root.left, root, num, res);
           if (!res.isComplete()) return res;
           res = _getFirstUnfinishedAST(root.right, root, num, res);
           if (!res.isComplete()) return res;
        }
        if (root.isUnaryExpr()) { // typically not possible.
           res = _getFirstUnfinishedAST(root.left, root, num, res);
           if (!res.isComplete()) return res;
        }

        return new ExpressionParserContext(parent, num, true);

    }

}
