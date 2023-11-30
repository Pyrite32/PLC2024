package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.ASTVisitor;
import edu.ufl.cise.cop4020fa23.ast.AssignmentStatement;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.Block;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.Declaration;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.ast.DoStatement;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.GuardedBlock;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.IfStatement;
import edu.ufl.cise.cop4020fa23.ast.LValue;
import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.Program;
import edu.ufl.cise.cop4020fa23.ast.ReturnStatement;
import edu.ufl.cise.cop4020fa23.ast.StatementBlock;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.Type;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.VarTableEntry;
import edu.ufl.cise.cop4020fa23.ast.WriteStatement;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import static edu.ufl.cise.cop4020fa23.Kind.RES_height;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import javax.swing.text.Position;

public class CodeGenVisitor implements ASTVisitor {

    private final String LBRACE = "{";
    private final String RBRACE = "}";

    SymbolTable variables = new SymbolTable();
    StringBuilder codebase = new StringBuilder();
    String packageName;

    Queue<String> deferredEmit = new LinkedList<String>();

    long id = 0;
    long scopeLevel = 0;

    HashMap<VarTableEntry, String> uniqueIdentifierMapper = new HashMap<VarTableEntry, String>();

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        String text = constExpr.getName();
        if (text.equals("Z")) {
            emits("255");
        } else {
            String result = "0x" + Integer.toHexString(stringToColor(text).getRGB());
            emits(result);
        }

        return null;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        emits(booleanLitExpr.getText().toLowerCase());
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        emits(stringLitExpr.firstToken().text());
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        emits(numLitExpr.getText());
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        var identName = identExpr.getName();
        var tryGetName = getIdentifierName(identName);

        emits(tryGetName);

        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg)
            throws PLCCompilerException {

        if (assignmentStatement.getlValue().getType() == Type.PIXEL
                && assignmentStatement.getE().getType() == Type.INT) {
            if (assignmentStatement.getlValue().getChannelSelector() != null) {
                assignmentStatement.getlValue().visit(this, arg);
                emits(LexicalStructure.Assign);
                Kind color = assignmentStatement.getlValue().getChannelSelector().color();
                switch (color) {
                    case RES_red:
                        emits("PixelOps.setRed", LexicalStructure.LParen);
                        break;
                    case RES_green:
                        emits("PixelOps.setGreen", LexicalStructure.LParen);
                        break;
                    case RES_blue:
                        emits("PixelOps.setBlue", LexicalStructure.LParen);
                        break;
                    default:
                        break;
                }
                assignmentStatement.getlValue().visit(this, arg);
                emits(LexicalStructure.Comma);
                assignmentStatement.getE().visit(this, arg);
                emits(LexicalStructure.RParen);
            } else {
                assignmentStatement.getlValue().visit(this, arg);
                emits(LexicalStructure.Assign);
                emits("CodeGenUtilities.asPixel", LexicalStructure.LParen);
                assignmentStatement.getE().visit(this, arg);
                emits(LexicalStructure.RParen);
            }
        } else if (assignmentStatement.getlValue().getType() == Type.IMAGE) {
            if (assignmentStatement.getlValue().getChannelSelector() != null
                    && assignmentStatement.getlValue().getPixelSelector() != null) {
                throw new CodeGenException(assignmentStatement.firstToken.sourceLocation(),
                        "Using Pixel and Channel selectors simultaneously is not supported!");
            }
            if (assignmentStatement.getE().getType() == Type.STRING) {

                String imageName = getIdentifierName(assignmentStatement.getlValue().getName());
                assignmentStatement.getlValue().visit(this, arg);
                emits(LexicalStructure.Assign);
                emits("ImageOps.copyAndResize ( FileURLIO.readImage", LexicalStructure.LParen);
                assignmentStatement.getE().visit(this, arg);
                emits(LexicalStructure.RParen);
                emits(LexicalStructure.Comma);
                emits(imageName, ".getWidth()");
                emits(LexicalStructure.Comma);
                emits(imageName, ".getHeight()");
                emits(LexicalStructure.RParen);
            }
            if (assignmentStatement.getE().getType() == Type.IMAGE) {

                String imageName = getIdentifierName(assignmentStatement.getlValue().getName());
                assignmentStatement.getlValue().visit(this, arg);
                emits(LexicalStructure.Assign);
                emits("ImageOps.copyAndResize", LexicalStructure.LParen);
                assignmentStatement.getE().visit(this, arg);
                emits(LexicalStructure.Comma);
                emits(imageName, ".getWidth()");
                emits(LexicalStructure.Comma);
                emits(imageName, ".getHeight()");
                emits(LexicalStructure.RParen);

            } else if (assignmentStatement.getE().getType() == Type.PIXEL
                    && assignmentStatement.getlValue().getPixelSelector() != null) {

                // get the name of the image.

                String imageName = getIdentifierName(assignmentStatement.getlValue().getName());
                var selector = assignmentStatement.getlValue().getPixelSelector();

                // do these exist?
                boolean xIsSwizzle = (selector.xExpr() instanceof IdentExpr)
                        && (getIdentifierNameOrNot(selector.xExpr().firstToken().text()).equals(""));
                boolean yIsSwizzle = (selector.yExpr() instanceof IdentExpr)
                        && (getIdentifierNameOrNot(selector.yExpr().firstToken().text()).equals(""));

                // if (!( || !(selector.yExpr() instanceof IdentExpr)) {
                // throw new CodeGenException("Pixel Selectors must be swizzles.");
                // }
                String swizzleXName = "", swizzleYName = "";

                if (yIsSwizzle) {
                    swizzleYName = makeIdentifierName(((IdentExpr) (selector.yExpr())).getName());
                    emitl("for", "(", "int", swizzleYName, "= 0;", swizzleYName, "<", imageName, ".getHeight()", ";",
                            swizzleYName, "++)", LBRACE);
                    deferredEmit.add(RBRACE);
                }

                if (xIsSwizzle) {
                    swizzleXName = makeIdentifierName(((IdentExpr) (selector.xExpr())).getName());
                    emitl("\tfor", "(", "int", swizzleXName, "= 0;", swizzleXName, "<", imageName, ".getWidth()", ";",
                            swizzleXName, "++)", LBRACE);
                    deferredEmit.add(RBRACE);
                }

                emits("ImageOps.setRGB", LexicalStructure.LParen, imageName, ",");
                if (xIsSwizzle) {
                    emits(swizzleXName);
                } else {
                    selector.xExpr().visit(this, arg);
                }

                emits(LexicalStructure.Comma);

                if (yIsSwizzle) {
                    emits(swizzleYName);
                } else {
                    selector.yExpr().visit(this, arg);
                }

                emits(LexicalStructure.Comma);

                if (assignmentStatement.getE() instanceof PostfixExpr) {
                    var postfix = (PostfixExpr) (assignmentStatement.getE());
                    var before = ((PostfixExpr) (assignmentStatement.getE())).primary();
                    if (before instanceof IdentExpr) {
                        var name = getIdentifierName(before.firstToken().text());
                        postfix.visit(this, name);
                    } else {
                        postfix.visit(this, null);
                    }
                } else {
                    // String[] swizzles = Can my compiler figure out the swizzles on its own?
                    assignmentStatement.getE().visit(this, arg);
                }
                emits(LexicalStructure.RParen);
                endStatement();

                // braces
                flushDeferred();

                // I want to use more swizzles in the future, but can't have any of the current
                // swizzles in the way!
                VarTableEntry rmSwizzleX = new VarTableEntry("x", scopeLevel);
                uniqueIdentifierMapper.remove(rmSwizzleX);

                VarTableEntry rmSwizzleY = new VarTableEntry("y", scopeLevel);
                uniqueIdentifierMapper.remove(rmSwizzleY);

            } else if (assignmentStatement.getE().getType() == Type.PIXEL) {
                assignmentStatement.getlValue().visit(this, arg);
                emits(LexicalStructure.Assign);

                String imageName = getIdentifierName((String) assignmentStatement.getlValue().getName());
                emits("CodeGenUtilities.setAllPixelsAndGive", LexicalStructure.LParen);

                emits(imageName, LexicalStructure.Comma);
                assignmentStatement.getE().visit(this, arg);
                emits(LexicalStructure.RParen);
            }
        } else {
            assignmentStatement.getlValue().visit(this, arg);
            emits(LexicalStructure.Assign);
            assignmentStatement.getE().visit(this, arg);
        }
        endStatement();

        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        emits(LexicalStructure.LParen);
        if (binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.IMAGE) {
            emits("ImageOps.binaryImageImageOp(", binaryOpToImageOp(binaryExpr.getOp().kind()),
                    ",");
            binaryExpr.getLeftExpr().visit(this, arg);
            emits(",");
            binaryExpr.getRightExpr().visit(this, arg);
            emits(")");
        } else if (binaryExpr.getLeftExpr().getType() == Type.IMAGE
                && binaryExpr.getRightExpr().getType() == Type.INT) {
            emits("ImageOps.binaryImageScalarOp(", binaryOpToImageOp(binaryExpr.getOp().kind()),
                    ",");
            binaryExpr.getLeftExpr().visit(this, arg);
            emits(",");
            binaryExpr.getRightExpr().visit(this, arg);
            emits(")");
        } else if (binaryExpr.getLeftExpr().getType() == Type.IMAGE
                && binaryExpr.getRightExpr().getType() == Type.PIXEL) {
            emits("ImageOps.binaryImagePixelOp(", binaryOpToImageOp(binaryExpr.getOp().kind()),
                    ",");
            binaryExpr.getLeftExpr().visit(this, arg);
            emits(",");
            binaryExpr.getRightExpr().visit(this, arg);
            emits(")");
        } else if (binaryExpr.getLeftExpr().getType() == Type.PIXEL
                && binaryExpr.getRightExpr().getType() == Type.PIXEL) {
            if (binaryExpr.getOpKind() != Kind.PLUS && binaryExpr.getOpKind() != Kind.TIMES
                    && binaryExpr.getOpKind() != Kind.MINUS &&
                    binaryExpr.getOpKind() != Kind.DIV && binaryExpr.getOpKind() != Kind.MOD) {
                emits("ImageOps.binaryPackedPixelBooleanOp(", binaryOpToImageBooleanOp(binaryExpr.getOp().kind()),
                        ",");

            } else {
                emits("ImageOps.binaryPackedPixelPixelOp(", binaryOpToImageOp(binaryExpr.getOp().kind()),
                        ",");
            }
            binaryExpr.getLeftExpr().visit(this, arg);
            emits(",");
            binaryExpr.getRightExpr().visit(this, arg);
            emits(")");
        } else if (binaryExpr.getLeftExpr().getType() == Type.PIXEL
                && binaryExpr.getRightExpr().getType() == Type.INT) {
            emits("ImageOps.binaryPackedPixelIntOp(", binaryOpToImageOp(binaryExpr.getOp().kind()), ",");
            binaryExpr.getLeftExpr().visit(this, arg);
            emits(",");
            binaryExpr.getRightExpr().visit(this, arg);
            emits(")");

        } else if (binaryExpr.getLeftExpr().getType() == Type.INT
                && binaryExpr.getRightExpr().getType() == Type.PIXEL) {
            emits("ImageOps.binaryPackedPixelIntOp(", binaryOpToImageOp(binaryExpr.getOp().kind()), ",");
            binaryExpr.getRightExpr().visit(this, arg);
            emits(",");
            binaryExpr.getLeftExpr().visit(this, arg);
            emits(")");

        } else {
            switch (binaryExpr.getOp().kind()) {
                case EXP:
                    emits(LexicalStructure.LParen, "int", LexicalStructure.RParen);
                    emits("Math.round", LexicalStructure.LParen);
                    emits("Math.pow", LexicalStructure.LParen);
                    binaryExpr.getLeftExpr().visit(this, arg);
                    emits(LexicalStructure.Comma);
                    binaryExpr.getRightExpr().visit(this, arg);
                    emits(LexicalStructure.RParen, LexicalStructure.RParen);
                    break;
                case EQ:
                    if (binaryExpr.getLeftExpr().getType() == Type.STRING) {
                        binaryExpr.getLeftExpr().visit(this, arg);
                        emits(".equals", LexicalStructure.LParen);
                        binaryExpr.getRightExpr().visit(this, arg);
                        emits(LexicalStructure.RParen);
                        break;
                    }
                default:
                    binaryExpr.getLeftExpr().visit(this, arg);
                    emits(LexicalStructure.kind2Char(binaryExpr.getOpKind()));
                    binaryExpr.getRightExpr().visit(this, arg);
                    break;
            }
        }
        emits(LexicalStructure.RParen);
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {

        emitl(LBRACE);
        scopeLevel++;
        for (var elem : block.getElems()) {
            indent();
            elem.visit(this, arg);
        }
        scopeLevel--;
        emitl(RBRACE);
        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        statementBlock.getBlock().visit(this, arg);
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        switch (channelSelector.color()) {
            case RES_red:
                emits("PixelOps.red", LexicalStructure.LParen);
                break;
            case RES_green:
                emits("PixelOps.green", LexicalStructure.LParen);
                break;
            case RES_blue:
                emits("PixelOps.blue", LexicalStructure.LParen);
                break;
            default:
                break;
        }
        // don't include; The primary expr above needs to be visited first.
        // emits(LexicalStructure.RParen);
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        emits(LexicalStructure.LParen);
        conditionalExpr.getGuardExpr().visit(this, arg);
        emits(LexicalStructure.Question);
        conditionalExpr.getTrueExpr().visit(this, imageNameOrNot(conditionalExpr.getTrueExpr(), arg));
        emits(LexicalStructure.Colon);
        conditionalExpr.getFalseExpr().visit(this, imageNameOrNot(conditionalExpr.getFalseExpr(), arg));
        emits(LexicalStructure.RParen);
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {

        // stupid

        // weird, why??
        // wait
        // if (declaration.getNameDef().getType() == Type.IMAGE);
        // emits("final");

        Dimension maybeDimension = (Dimension) declaration.getNameDef().visit(this, arg);

        if (maybeDimension != null && maybeDimension instanceof Dimension) {
            if (declaration.getNameDef().getType() == Type.IMAGE && declaration.getInitializer() == null) {
                emits(LexicalStructure.Assign);
                emits("ImageOps.makeImage", LexicalStructure.LParen);
                maybeDimension.getWidth().visit(this, arg);
                emits(LexicalStructure.Comma);
                maybeDimension.getHeight().visit(this, arg);
                emits(LexicalStructure.RParen);
                endStatement();

                return null;
            }
        }

        if (declaration.getInitializer() != null) {
            emits(LexicalStructure.Assign);
            if (maybeDimension != null) {
                // declare image on its own.
                emits("ImageOps.copyAndResize", LexicalStructure.LParen);

                // call copyInto
                if (declaration.getNameDef().getType() == Type.IMAGE
                        && declaration.getInitializer().getType() == Type.STRING) {
                    emits("FileURLIO.readImage", LexicalStructure.LParen);
                    declaration.getInitializer().visit(this, true);
                    emits(LexicalStructure.RParen);

                } else if (declaration.getNameDef().getType() == Type.IMAGE
                        && declaration.getInitializer().getType() == Type.IMAGE) {
                    declaration.getInitializer().visit(this, true);
                }
                emits(LexicalStructure.Comma);
                // include resize.
                maybeDimension.getWidth().visit(this, arg);
                emits(LexicalStructure.Comma);
                maybeDimension.getHeight().visit(this, arg);
                emits(LexicalStructure.RParen);

            } else {
                if (declaration.getNameDef().getType() == Type.IMAGE
                        && declaration.getInitializer().getType() == Type.STRING) {
                    emits("FileURLIO.readImage", LexicalStructure.LParen);
                    declaration.getInitializer().visit(this, true);
                    emits(LexicalStructure.RParen);
                } else if (declaration.getNameDef().getType() == Type.IMAGE
                        && declaration.getInitializer().getType() == Type.IMAGE) {
                    emits("ImageOps.cloneImage", LexicalStructure.LParen);
                    declaration.getInitializer().visit(this, true);
                    emits(LexicalStructure.RParen);
                } else {
                    declaration.getInitializer().visit(this, true);
                }
            }

        }
        endStatement();

        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        // dimension.getWidth().visit(this, arg);
        // emits(",");
        // dimension.getHeight().visit(this, arg);
        // no benefit from doing this.
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {

        String continueDoName = makeIdentifierName("continueDo");
        emitl("boolean", continueDoName, LexicalStructure.Assign, "false", LexicalStructure.Semi);
        indent();
        emitl("while", LexicalStructure.LParen, LexicalStructure.Bang, continueDoName, LexicalStructure.RParen, LBRACE);
        indent();
        emitl(continueDoName, LexicalStructure.Assign, "true", LexicalStructure.Semi);

        var blocks = doStatement.getGuardedBlocks();

        for (var gBlock : blocks) {
            gBlock.visit(this, continueDoName);
        }

        emitl(RBRACE);

        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        emits("PixelOps.pack", LexicalStructure.LParen);
        expandedPixelExpr.getRed().visit(this, arg);
        emits(LexicalStructure.Comma);
        expandedPixelExpr.getGreen().visit(this, arg);
        emits(LexicalStructure.Comma);
        expandedPixelExpr.getBlue().visit(this, arg);
        emits(LexicalStructure.RParen);
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        indent();
        Integer index = 0;
        String falsifyConditionName = null;
        if (arg instanceof Integer) {
            index = (Integer) arg;
        } else if (arg instanceof String) {
            falsifyConditionName = (String) arg;
        }

        if (index == 0) {
            emits("if", LexicalStructure.LParen);
        }
        /*
         * else if (index == -1) {
         * emits("else");
         * guardedBlock.getBlock().visit(this, arg);
         * return null;
         * }
         */
        else {
            emits("else if", LexicalStructure.LParen);
        }

        guardedBlock.getGuard().visit(this, arg);
        emits(LexicalStructure.RParen);
        if (falsifyConditionName != null) {

            emits("\n");
            emitl(LBRACE);
            indent();
            emitl(falsifyConditionName, LexicalStructure.Assign, "false", LexicalStructure.Semi);
            indent();
            guardedBlock.getBlock().visit(this, arg);
            emitl(RBRACE);
        } else {
            guardedBlock.getBlock().visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        // emits("if",LexicalStructure.LParen);
        // emits(LexicalStructure.LParen);
        var blocks = ifStatement.getGuardedBlocks();
        var size = blocks.size();
        Integer index = 0;
        for (var gBlock : blocks) {
            if (index == size - 1 && size != 1) {
                index = -1;
            }
            gBlock.visit(this, index);
            index++;
        }
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        if (lValue.getNameDef() != null) {
            lValue.getNameDef().visit(this, arg);
        } else {
            var name = getIdentifierName(lValue.getName());
            emits(name);
        }
        // TODO Handle the pixel crap.

        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        var name = makeIdentifierName(nameDef.getName());
        Dimension result = null;
        emits(typeToJavaType(nameDef.getType()));
        if (nameDef.getDimension() != null) {
            result = nameDef.getDimension();
            nameDef.getDimension().visit(this, arg);
        }
        emits(name);

        return result;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        if (arg != null && arg instanceof String) {
            emits("ImageOps.getRGB", LexicalStructure.LParen, (String) arg, LexicalStructure.Comma);
            pixelSelector.xExpr().visit(this, arg);
            emits(LexicalStructure.Comma);
            pixelSelector.yExpr().visit(this, arg);
            emits(LexicalStructure.RParen);
        }

        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        // the primary expression will always be an integer.
        // no extension methods in Java
        // call primary last.
        // structure ->
        // get specific pixel ->
        // isolate colors ->
        // image

        if (postfixExpr.pixel() != null) {

            // get image name
            String colorMethod = "";
            if (postfixExpr.channel() != null) {

                if (postfixExpr.channel().color() == Kind.RES_red)
                    colorMethod = "red";
                if (postfixExpr.channel().color() == Kind.RES_green)
                    colorMethod = "green";
                if (postfixExpr.channel().color() == Kind.RES_blue)
                    colorMethod = "blue";
                emits("PixelOps.", colorMethod, LexicalStructure.LParen);

                emits("ImageOps.getRGB", LexicalStructure.LParen);
                if (postfixExpr.primary() instanceof IdentExpr) {
                    String imageName = getIdentifierName(((IdentExpr) ((postfixExpr.primary()))).getName());
                    emits(imageName);
                } else {
                    postfixExpr.primary().visit(this, arg);
                }

                emits(LexicalStructure.Comma);
                postfixExpr.pixel().xExpr().visit(this, arg);
                emits(LexicalStructure.Comma);
                postfixExpr.pixel().yExpr().visit(this, arg);
                emits(LexicalStructure.RParen);
                emits(LexicalStructure.RParen);

            } else {
                emits("ImageOps.getRGB", LexicalStructure.LParen);
                postfixExpr.primary().visit(this, arg);
                emits(LexicalStructure.Comma);
                postfixExpr.pixel().xExpr().visit(this, arg);
                emits(LexicalStructure.Comma);
                postfixExpr.pixel().yExpr().visit(this, arg);
                emits(LexicalStructure.RParen);
                //emits(LexicalStructure.RParen);
            }

            // postfixExpr.primary().visit(this, arg);
        } else if (postfixExpr.channel() != null) {
            String colorMethod = "";
            if (postfixExpr.primary().getType() == Type.PIXEL) {
                if (postfixExpr.channel().color() == Kind.RES_red)
                    colorMethod = "red";
                if (postfixExpr.channel().color() == Kind.RES_green)
                    colorMethod = "green";
                if (postfixExpr.channel().color() == Kind.RES_blue)
                    colorMethod = "blue";
                emits("PixelOps.", colorMethod, LexicalStructure.LParen);
            } else {
                if (postfixExpr.channel().color() == Kind.RES_red)
                    colorMethod = "extractRed";
                if (postfixExpr.channel().color() == Kind.RES_green)
                    colorMethod = "extractGrn";
                if (postfixExpr.channel().color() == Kind.RES_blue)
                    colorMethod = "extractBlu";
                emits("ImageOps.", colorMethod, LexicalStructure.LParen);
            }
            if (postfixExpr.primary() instanceof IdentExpr) {
                String imageName = getIdentifierName(((IdentExpr) ((postfixExpr.primary()))).getName());
                emits(imageName);
            } else {
                postfixExpr.primary().visit(this, arg);
            }

            emits(LexicalStructure.RParen);
        }

        return null;
    }

    private String typeToJavaType(Type type) {
        return switch (type) {
            case STRING -> "String";
            case PIXEL -> "int";
            case BOOLEAN -> "Boolean";
            case IMAGE -> "BufferedImage";
            default -> type.toString().toLowerCase();
        };

    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {

        // var ident = program.getName();
        // var type = program.getType();
        // // arg is the package name.
        // var pckg = (String) arg;

        // emit package
        emitl("package", (String) arg, LexicalStructure.Semi);
        emitl("import", (String) arg, ".runtime", ".ConsoleIO", LexicalStructure.Semi);
        emitl("import", (String) arg, ".runtime", ".PixelOps", LexicalStructure.Semi);
        emitl("import", (String) arg, ".runtime", ".FileURLIO", LexicalStructure.Semi);
        emitl("import", (String) arg, ".runtime", ".ImageOps", LexicalStructure.Semi);
        emitl("import", "java", ".awt", ".image", ".BufferedImage", LexicalStructure.Semi);

        // emit class
        emitl("public class", program.getName(), LBRACE);
        // emit method
        emits("public static", typeToJavaType(program.getType()), "apply");

        // emit params
        emits(LexicalStructure.LParen);
        boolean first = false;

        scopeLevel = 1;

        for (var param : program.getParams()) {
            if (first)
                emits(LexicalStructure.Comma);
            param.visit(this, arg);
            first = true;
        }

        scopeLevel = 0;

        emits(LexicalStructure.RParen);

        // emit block
        program.getBlock().visit(this, arg);

        // end class
        emits(RBRACE);

        return codebase.toString();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        emits("return");
        returnStatement.getE().visit(this, arg);
        emits(LexicalStructure.Semi);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        emits(LexicalStructure.LParen);
        switch (unaryExpr.getOp()) {
            case MINUS:
            case BANG:
                emits(LexicalStructure.kind2Char(unaryExpr.getOp()));
                unaryExpr.getExpr().visit(this, arg);
                break;
            case RES_width:
                emits("CodeGenUtilities.widthOf (");
                unaryExpr.getExpr().visit(this, arg);
                emits(")");
                break;
            case RES_height:
                emits("CodeGenUtilities.heightOf (");
                unaryExpr.getExpr().visit(this, arg);
                emits(")");
                break;
            default:
                throw new UnsupportedOperationException();
        }
        emits(LexicalStructure.RParen);
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        if (writeStatement.getExpr().getType() == Type.PIXEL) {
            emits("ConsoleIO.writePixel", LexicalStructure.LParen);
        } else {
            emits("ConsoleIO.write", LexicalStructure.LParen);
        }
        writeStatement.getExpr().visit(this, arg);
        emits(LexicalStructure.RParen, LexicalStructure.Semi);
        return null;
    }

    private void emits(String... strings) {
        StringBuilder temp = new StringBuilder();
        for (var string : strings) {
            temp.append(string + " ");
            if (string.equals(";")) {
                temp.append("\n");
            }
        }
        codebase.append(temp);
    }

    private void emitl(String... strings) {
        StringBuilder temp = new StringBuilder();
        for (var string : strings) {
            temp.append(string + " ");
        }
        temp.append('\n');
        codebase.append(temp);
    }

    private void endStatement() {
        codebase.append(";");
        codebase.append("\n");
    }

    private void indent() {
        for (int i = 0; i < scopeLevel; i++) {
            codebase.append("\t");
        }
    }

    private String getIdentifierName(String identName) throws CodeGenException {
        for (long i = scopeLevel; i >= 0; --i) {
            var key = new VarTableEntry(identName, i);
            var tryGetName = uniqueIdentifierMapper.get(key);
            if (tryGetName != null) {
                return tryGetName;
            }
        }

        throw new CodeGenException("The symbol " + identName + " does not exist.");
    }

    private String getIdentifierNameOrNot(String identName) throws CodeGenException {
        for (long i = scopeLevel; i >= 0; --i) {
            var key = new VarTableEntry(identName, i);
            var tryGetName = uniqueIdentifierMapper.get(key);
            if (tryGetName != null) {
                return tryGetName;
            }
        }

        return "";
    }

    private String makeIdentifierName(String identName) {
        var key = new VarTableEntry(identName, scopeLevel);
        var stringName = "$" + identName + (++id);
        uniqueIdentifierMapper.put(key, stringName);
        return stringName;
    }

    private void flushDeferred() {
        while (!deferredEmit.isEmpty()) {
            emits(deferredEmit.poll());
        }
    }

    private Color stringToColor(String color) {
        return switch (color) {
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "CYAN" -> Color.CYAN;
            case "DARK_GRAY" -> Color.DARK_GRAY;
            case "GRAY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LIGHT_GRAY" -> Color.LIGHT_GRAY;
            case "MAGENTA" -> Color.MAGENTA;
            case "ORANGE" -> Color.ORANGE;
            case "PINK" -> Color.PINK;
            case "RED" -> Color.RED;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            default -> Color.WHITE;
        };

    }

    private String binaryOpToImageOp(Kind op) {
        return "ImageOps.OP." + op.toString();
    }

    private String binaryOpToImageBooleanOp(Kind op) throws PLCCompilerException {
        if (op == Kind.EQ)
            return "ImageOps.BoolOP.EQUALS";
        else
            throw new CodeGenException("Unsupported pixel boolean operation : " + op.toString());
    }

    private Object imageNameOrNot(Object expression, Object otherwise) throws PLCCompilerException {
        if (!(expression instanceof Expr)) {
            throw new CodeGenException("Did not supply an expression!");
        }

        Type type = ((Expr) (expression)).getType();
        if (type == Type.IMAGE || type == Type.PIXEL) {
            if (expression instanceof IdentExpr) {
                return getIdentifierName(((IdentExpr) (expression)).firstToken().text());
            } else if (expression instanceof PostfixExpr) {
                return getIdentifierName(((PostfixExpr) (expression)).primary().firstToken().text());
            }
        }
        return otherwise;

    }

}