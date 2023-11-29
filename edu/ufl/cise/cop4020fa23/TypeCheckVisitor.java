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
import edu.ufl.cise.cop4020fa23.ast.WriteStatement;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.List;

public class TypeCheckVisitor implements ASTVisitor {

    private SymbolTable table;
    private Program ASTRoot;

    public TypeCheckVisitor() {
        super();
        table = new SymbolTable();
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        if (constExpr.firstToken().text().equals("Z")) {
            constExpr.setType(Type.INT);
            return Type.INT;
        } else {
            constExpr.setType(Type.PIXEL);
            return Type.PIXEL;
        }
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        numLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitIdentExpr ");
        // System.out.println("Exit visitIdentExpr ");
        if (arg != null) {
            identExpr.setType((Type) arg);
            return (Type) arg;
        } else {
            return null;
        }
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitNameDef ");

        if (arg != null) {
            var shouldDeferAdd = (boolean) arg;
            if (shouldDeferAdd) {
                table.putDeferred(nameDef);
            } else {
                table.put(nameDef.getName(), nameDef);
            }
        } else {
            table.put(nameDef.getName(), nameDef);
        }

        if (nameDef.getTypeToken().kind() == Kind.RES_void) {
            throw new TypeCheckException(nameDef.firstToken().sourceLocation(),
                    "Cannot declare variable of type 'void'.");
        }

        if (nameDef.getDimension() != null) {
            nameDef.getDimension().visit(this, arg);
        }

        // System.out.println("Exit visitNameDef ");
        return nameDef.getType();
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        // System.out.println("Touch VisitProgram ");

        ASTRoot = program;
        ASTRoot.setType(res2Type(ASTRoot.getTypeToken()));

        List<NameDef> parameters = ASTRoot.getParams();
        for (var param : parameters) {
            param.visit(this, arg);
        }
        ASTRoot.getBlock().visit(this, arg);

        // System.out.println("Exit VisitProgram ");
        return ASTRoot;
    }

    // no type
    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        // System.out.println("Touch VisitBlock ");
        table.enterScope();
        var code = block.getElems();
        for (var line : code) {
            line.visit(this, arg);
        }
        table.exitScope();
        // no type
        return code;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitDeclaration ");
        declaration.getNameDef().visit(this, true);

        Type varType = res2Type(declaration.firstToken());

        if (declaration.getInitializer() != null) {
            if (declaration.getInitializer() instanceof IdentExpr && !table.has((declaration.getInitializer()).firstToken().text())) {
                throw new TypeCheckException("The name " + declaration.getInitializer().firstToken().text() + " does not exist!");
            }

            boolean implicitCast = false;
            Type initType = declaration.getNameDef().getType();
            if (varType == Type.IMAGE) {
                if (declaration.getInitializer() instanceof IdentExpr) {
                    var otherType = table.typeOf(declaration.getInitializer().firstToken().text());
                    declaration.getInitializer().visit(this, otherType);

                    if (otherType != Type.IMAGE && otherType != Type.STRING) {
                        throw new TypeCheckException("Identifier " + declaration.getInitializer().firstToken().text() + " should be of type Image or String. Got : " + otherType.toString());
                    }
                    initType = otherType;
                    
                }
                else {
                    initType = (Type)declaration.getInitializer().visit(this, arg);
                }
            }

            if (varType == Type.IMAGE && (initType == Type.STRING || declaration.getInitializer().firstToken().kind() == Kind.STRING_LIT)) {
                implicitCast = true;
            }

            declaration.getInitializer().visit(this, initType);
            initType = declaration.getInitializer().getType();
            declaration.getInitializer().setType((Type) initType);

            if (initType != varType && !implicitCast) {
                throw new TypeCheckException(declaration.firstToken().sourceLocation(),
                        "The initializer does not match the expected type: " + varType.toString());
            }

            
        }
        table.flushDeferredPuts();
        // System.out.println("Exit visitDeclaration ");
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitExpandedPixelExpr ");
        var x = expandedPixelExpr.getRed();
        if (x instanceof IdentExpr) {
            expandedPixelExpr.getRed().visit(this, table.typeOf(x.firstToken().text()));
        } else {
            expandedPixelExpr.getRed().visit(this, Type.INT);
        }
        var y = expandedPixelExpr.getGreen();
        if (y instanceof IdentExpr) {
            expandedPixelExpr.getGreen().visit(this, table.typeOf(y.firstToken().text()));
        } else {
            expandedPixelExpr.getGreen().visit(this, Type.INT);
        }
        var z = expandedPixelExpr.getBlue();
        if (z instanceof IdentExpr) {
            expandedPixelExpr.getBlue().visit(this, table.typeOf(y.firstToken().text()));
        } else {
            expandedPixelExpr.getBlue().visit(this, Type.INT);
        }
        expandedPixelExpr.setType(Type.PIXEL);

        if (expandedPixelExpr.getRed().getType() != Type.INT) {
            throw new TypeCheckException("Pixel selector red is required to be an Int- got "
                    + expandedPixelExpr.getRed().getType() + " instead");
        }
        if (expandedPixelExpr.getGreen().getType() != Type.INT) {
            throw new TypeCheckException("Pixel selector green is required to be an Int- got "
                    + expandedPixelExpr.getGreen().getType() + " instead");
        }
        if (expandedPixelExpr.getBlue().getType() != Type.INT) {
            throw new TypeCheckException("Pixel selector blue is required to be an Int- got "
                    + expandedPixelExpr.getBlue().getType() + " instead");
        }

        // System.out.println("Exit visitExpandedPixelExpr ");
        return Type.PIXEL;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitReturnStatement ");
        if (returnStatement.getE() instanceof IdentExpr) {
            var type = table.typeOf(returnStatement.getE().firstToken().text());
            returnStatement.getE().visit(this, type);
        }
        returnStatement.getE().visit(this, arg);

        var finalType = returnStatement.getE().getType();
        if (finalType != ASTRoot.getType()) {
            throw new TypeCheckException(returnStatement.firstToken().sourceLocation(), "Return type "
                    + finalType.toString() + " does not match the expected type " + ASTRoot.getType().toString());
        }

        // System.out.println("Exit visitReturnStatement ");
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitUnaryExpr ");
        var expr = unaryExpr.getExpr();
        Type exprType = null;
        if (expr instanceof IdentExpr) {
            exprType = table.typeOf(expr.firstToken().text());
            expr.visit(this, exprType);
        } else {
            exprType = (Type) expr.visit(this, arg);
        }

        Type finalType = null;

        var op = unaryExpr.getOp();
        switch (op) {
            case BANG:
                if (exprType != Type.BOOLEAN)
                    throw new TypeCheckException(unaryExpr.firstToken().sourceLocation(),
                            "Cannot apply '!' to type " + exprType.toString());
                finalType = Type.BOOLEAN;
                break;
            case MINUS:
                if (exprType != Type.INT)
                    throw new TypeCheckException(unaryExpr.firstToken().sourceLocation(),
                            "Cannot apply '-' to type " + exprType.toString());
                finalType = Type.INT;
                break;
            case RES_width, RES_height:
                if (exprType != Type.IMAGE)
                    throw new TypeCheckException(unaryExpr.firstToken().sourceLocation(),
                            "Cannot apply 'width' or 'height' to type " + exprType.toString());
                finalType = Type.INT;
                break;
            default:
                break;
        }

        // System.out.println("Exit visitUnaryExpr ");
        unaryExpr.setType(finalType);
        return finalType;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitWriteStatement ");
        Type type = null;
        if (writeStatement.getExpr() instanceof IdentExpr) {
            type = table.typeOf(writeStatement.getExpr().firstToken().text());
            writeStatement.getExpr().visit(this, type);
        } else {
            type = (Type) writeStatement.getExpr().visit(this, arg);
        }
        // System.out.println("Exit visitWriteStatement ");
        return type;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitDimension ");
        if (dimension.getWidth() instanceof IdentExpr) {
            table.addTemporarySwizzle(dimension.getWidth().firstToken().text());
            dimension.getWidth().visit(this, Type.INT);
            
        }
        else {
            dimension.getWidth().visit(this, arg);
        }

        if (dimension.getWidth() instanceof IdentExpr) {
            table.addTemporarySwizzle(dimension.getHeight().firstToken().text());
            dimension.getHeight().visit(this, Type.INT);

        }
        else {
            dimension.getHeight().visit(this, arg);
        }


        if (dimension.getWidth().getType() != Type.INT) {
            throw new TypeCheckException(dimension.getWidth().firstToken().sourceLocation(), "Width must be of type 'INT'");
        }
        if (dimension.getHeight().getType() != Type.INT) {
            throw new TypeCheckException(dimension.getWidth().firstToken().sourceLocation(), "Height must be of type 'INT'");
        }

        // System.out.println("Exit visitDimension ");
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitDoStatement ");
        var blocks = doStatement.getGuardedBlocks();

        table.enterScope();
        for (var block : blocks) {
            block.visit(this, arg);
        }
        table.exitScope();
        // System.out.println("Exit visitDoStatement ");
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg)
            throws PLCCompilerException {
        // System.out.println("Touch visitAssignmentStatement ");

        assignmentStatement.getlValue().visit(this, true);
        if (assignmentStatement.getE() instanceof IdentExpr) {
            var type = table.typeOf(assignmentStatement.getE().firstToken().text());
            assignmentStatement.getE().visit(this, type);
        }
        else {
            assignmentStatement.getE().visit(this, arg);
        }

        var typeL = assignmentStatement.getlValue().getType();
        var typeR = assignmentStatement.getE().getType();
        if (typeL != typeR) {
            if (typeL == Type.PIXEL && typeR == Type.INT) {
            } else if (typeL == Type.IMAGE && (typeR == Type.PIXEL || typeR == Type.INT || typeR == Type.STRING)) {
            } else
                throw new TypeCheckException(assignmentStatement.firstToken().sourceLocation(),
                        "Failed to cast between type " + typeL + " and " + typeR);
        }

        // the only time swizzles are used is in assignment statements I THINK, ALTHOUGH
        // I'M NOT 100% SURE
        table.clearSwizzles();

        // System.out.println("Exit visitAssignmentStatement ");
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitBinaryExpr ");

        // only adding works
        var left = binaryExpr.getLeftExpr();
        var right = binaryExpr.getRightExpr();

        Type leftType = null;
        Type rightType = null;

        if (left instanceof IdentExpr) {
            leftType = table.typeOf(left.firstToken().text());
            left.visit(this, leftType);
        } else {
            leftType = (Type) left.visit(this, arg);
        }

        if (right instanceof IdentExpr) {
            rightType = table.typeOf(right.firstToken().text());
            right.visit(this, rightType);
        } else {
            rightType = (Type) right.visit(this, arg);
        }

        Type finalType = null;
        var opKind = binaryExpr.getOpKind();

        if (opKind == Kind.EQ) {
            if (rightType != leftType)
                throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                        "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                + rightType.toString());
            finalType = Type.BOOLEAN;
        } else if (opKind == Kind.PLUS) {
            if (rightType != leftType)
                throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                        "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                + rightType.toString());
            finalType = leftType;
        }

        else if (leftType == Type.PIXEL) {
            if (opKind == Kind.BITAND || opKind == Kind.BITOR) {
                if (rightType != Type.PIXEL)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.PIXEL;

            }

             if (opKind == Kind.EXP) {
                if (rightType != Type.INT)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.PIXEL;
            }

            if (opKind == Kind.MINUS || opKind == Kind.TIMES || opKind == Kind.DIV || opKind == Kind.MOD) {
                if (opKind == Kind.TIMES || opKind == Kind.DIV || opKind == Kind.MOD) {
                    if (leftType == Type.PIXEL) {
                        finalType = Type.PIXEL;
                    } else if (leftType == Type.INT) {
                        finalType = Type.INT;
                    } else {
                        throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                                "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                        + rightType.toString());
                    }

                } else {
                    if (leftType != Type.PIXEL)
                        throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                                "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                        + rightType.toString());
                    finalType = Type.PIXEL;
                }

            }

        } else if (leftType == Type.BOOLEAN) {
            if (opKind == Kind.AND || opKind == Kind.OR) {
                if (rightType != Type.BOOLEAN)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.BOOLEAN;
            }
            else throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
        } else if (leftType == Type.INT) {
            if (opKind == Kind.LT || opKind == Kind.GT || opKind == Kind.LE || opKind == Kind.GE) {
                if (rightType != Type.INT)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.BOOLEAN;
            }
            if (opKind == Kind.EXP) {
                if (rightType != Type.INT)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.INT;
            }

            if (opKind == Kind.MINUS || opKind == Kind.TIMES || opKind == Kind.DIV || opKind == Kind.MOD) {
                if (rightType != Type.INT)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.INT;
            }

        } else if (leftType == Type.PIXEL) {
            if (opKind == Kind.EXP) {
                if (rightType != Type.INT)
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                finalType = Type.INT;
            }
        } else if (leftType == Type.IMAGE) {
            if (opKind == Kind.TIMES || opKind == Kind.DIV || opKind == Kind.MOD) {
                if (rightType == leftType || rightType == Type.INT) {
                    finalType = leftType;
                } else {
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                }
            }
            else if (opKind == Kind.MINUS) {
                if (rightType == leftType) {
                    finalType = leftType;
                } else {
                    throw new TypeCheckException(binaryExpr.firstToken().sourceLocation(),
                            "Cannot apply " + opKind.toString() + " Between" + leftType.toString() + " and "
                                    + rightType.toString());
                }
            } else {
                throw new TypeCheckException(right.firstToken().sourceLocation(),
                        "Unsupported operator " + opKind.toString());
            }
        }

        // System.out.println("Exit visitBinaryExpr ");
        binaryExpr.setType(finalType);
        return finalType;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        // System.out.println("Touch VisitBlockStatement ");
        statementBlock.getBlock().visit(this, arg);
        // System.out.println("Exit VisitBlockStatement ");
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitChannelSelector ");
        // System.out.println("Exit visitChannelSelector ");
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitConditionalExpr ");

        var condition = conditionalExpr.getGuardExpr();

        if (condition instanceof IdentExpr) {
            var type = table.typeOf(condition.firstToken().text());
            condition.visit(this, type);
            if (type != Type.BOOLEAN) {
                throw new TypeCheckException(conditionalExpr.firstToken().sourceLocation(),
                        "Cannot convert " + type.toString() + " to Boolean.");
            }
        } else {
            var type = (Type) condition.visit(this, arg);
            condition.setType(type);
            if (type != Type.BOOLEAN) {
                throw new TypeCheckException(conditionalExpr.firstToken().sourceLocation(),
                        "Cannot convert " + type.toString() + " to Boolean.");
            }
        }

        var pass = conditionalExpr.getTrueExpr();
        Type trueType = null;
        Type falseType = null;
        if (pass instanceof IdentExpr) {
            trueType = table.typeOf(pass.firstToken().text());
            pass.visit(this, trueType);
        } else {
            trueType = (Type) pass.visit(this, arg);
            pass.setType(trueType);
        }

        var fail = conditionalExpr.getFalseExpr();

        if (fail instanceof IdentExpr) {
            trueType = table.typeOf(fail.firstToken().text());
            fail.visit(this, trueType);
        } else {
            falseType = (Type) fail.visit(this, arg);
            fail.setType(falseType);
        }

        if (trueType != falseType) {
            throw new TypeCheckException(fail.firstToken().sourceLocation(),
                    "Not all code paths return the same type.");
        }

        // System.out.println("Exit visitConditionalExpr ");
        conditionalExpr.setType(trueType);
        return trueType;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitPixelSelector ");

        // don't know if this is 100 percent true -- why not just addVec2Swizzles()?
        var x = pixelSelector.xExpr();
        if (x instanceof IdentExpr) {

            // attempt to add a swizzle in this scope.
            if (!table.has(x.firstToken().text())) {
                table.addTemporarySwizzle(x.firstToken().text());
            }

            pixelSelector.xExpr().visit(this, table.typeOf(x.firstToken().text()));
        } else {
            pixelSelector.xExpr().visit(this, Type.INT);
        }
        var y = pixelSelector.yExpr();
        if (y instanceof IdentExpr) {

            // attempt to add a swizzle in this scope
            if (!table.has(y.firstToken().text())) {
                table.addTemporarySwizzle(y.firstToken().text());
            }

            pixelSelector.yExpr().visit(this, table.typeOf(y.firstToken().text()));
        } else {
            pixelSelector.yExpr().visit(this, Type.INT);
        }
        // System.out.println("Exit visitPixelSelector ");
        if (pixelSelector.xExpr().getType() != Type.INT) {
            throw new TypeCheckException(
                    "Pixel selector X is required to be an Int- got " + pixelSelector.xExpr().getType() + " instead");
        }
        if (pixelSelector.yExpr().getType() != Type.INT) {
            throw new TypeCheckException(
                    "Pixel selector Y is required to be an Int- got " + pixelSelector.yExpr().getType() + " instead");
        }
        return Type.PIXEL;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        //System.out.println("Touch visitPostfixExpr ");

        var exp = postfixExpr.primary();
        var pixel = postfixExpr.pixel();
        var channel = postfixExpr.channel();

        Type finalType = null;

        if (postfixExpr.primary() instanceof IdentExpr) {
            var type = table.typeOf(postfixExpr.primary().firstToken().text());
            postfixExpr.primary().setType(type);
        }
        if (postfixExpr.primary().getType() == null) {
            postfixExpr.primary().setType((Type) arg);
            postfixExpr.primary().visit(this, arg);
        }

        // most common case.
        if (pixel == null && channel == null) {
            postfixExpr.setType(exp.getType());
            return postfixExpr.getType();
        }
        else {
            if (exp.getType() == Type.IMAGE) {
                if (pixel != null && channel == null) {
                    finalType = Type.PIXEL;
                }
                else if (pixel == null && channel != null ) {
                    finalType = Type.IMAGE;
                }
                else if (pixel != null && channel != null) {
                    finalType = Type.INT;
                }
            }
            else if (exp.getType() == Type.PIXEL) {
                if (pixel == null && channel != null) {
                    finalType = Type.INT;
                }
                else {
                    throw new TypeCheckException(exp.firstToken().sourceLocation(), "Type " + exp.getType() + " does not support pixel selectors");
                }
            }
            else {
                throw new TypeCheckException(exp.firstToken().sourceLocation(), "Type " + exp.getType() + " does not support pixel or channel selectors");
            }

            if (channel != null)
            channel.visit(this, arg);
            if (pixel != null)
            pixel.visit(this, arg);
    
    
            // if (postfixExpr.pixel() != null) {
            //     postfixExpr.setType(Type.PIXEL);
            //     postfixExpr.pixel().visit(this, arg);
            // }
            // if (postfixExpr.channel() != null) {
            //     postfixExpr.setType(Type.INT);
            // }
            
    
            //System.out.println("Exit  visitPostfixExpr ");
            postfixExpr.setType(finalType);
            return postfixExpr.getType();

        }
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitGuardedBlock ");

        Type primaryType = null;
        if (guardedBlock.getGuard() instanceof IdentExpr) {
            primaryType = table.typeOf(guardedBlock.getGuard().firstToken().text());
        } else {
            primaryType = (Type) guardedBlock.getGuard().visit(this, arg);
        }

        
        // set the ident type.
        guardedBlock.getGuard().visit(this, primaryType);
        
        // guard must be boolean
        if (guardedBlock.getGuard().getType() != Type.BOOLEAN) {
            throw new TypeCheckException(guardedBlock.firstToken().sourceLocation(), "Guard statement must be of type 'BOOLEAN'");
        }

        // visit the block
        table.enterScope();
        guardedBlock.getBlock().visit(this, arg);
        table.exitScope();

        // System.out.println("Exit visitGuardedBlock ");
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitDoStatement ");
        var blocks = ifStatement.getGuardedBlocks();

        table.enterScope();
        for (var block : blocks) {
            block.visit(this, arg);
        }
        table.exitScope();
        // System.out.println("Exit visitDoStatement ");
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        // System.out.println("Touch visitLValue ");

        // the only time swizzles are able to be used.

        Type finalType = null;
        if (table.has(lValue.getName())) {
            finalType = table.typeOf(lValue.getName());
        }

        if (lValue.getChannelSelector() != null) {
            lValue.getChannelSelector().visit(this, arg);
            lValue.setType(Type.INT);
        }
        if (lValue.getPixelSelector() != null) {

            // if the argument is true, then swizzles are allowed.
            lValue.getPixelSelector().visit(this, true);
            lValue.setType(Type.PIXEL);
        }

        lValue.setType(finalType);
        // System.out.println("Exit visitLValue ");
        return finalType;
    }

    private Type res2Type(IToken res) throws TypeCheckException {
        switch (res.kind()) {
            case RES_void:
                return Type.VOID;
            case RES_boolean:
                return Type.BOOLEAN;
            case RES_image:
                return Type.IMAGE;
            case RES_int:
                return Type.INT;
            case RES_pixel:
                return Type.PIXEL;
            case RES_string:
                return Type.STRING;
            default:
                throw new TypeCheckException(res.sourceLocation(), "Invalid type specified.");
        }
    }

}
