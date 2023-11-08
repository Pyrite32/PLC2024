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
import edu.ufl.cise.cop4020fa23.VarTableEntry;
import edu.ufl.cise.cop4020fa23.ast.WriteStatement;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;

import java.util.HashMap;
import java.util.UUID;

public class CodeGenVisitor implements ASTVisitor {

    private final String LBRACE = "{";
    private final String RBRACE = "}";

    SymbolTable variables = new SymbolTable();
    StringBuilder codebase = new StringBuilder();
    String packageName;

    long id = 0;
    long scopeLevel = 0;

    HashMap<VarTableEntry, String> uniqueIdentifierMapper = new HashMap<VarTableEntry, String>();

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        emits(constExpr.firstToken().text());
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
        assignmentStatement.getlValue().visit(this, arg);
        emits(LexicalStructure.Assign);
        assignmentStatement.getE().visit(this, arg);
        endStatement();

        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {

        switch (binaryExpr.getOp().kind()) {
            case EXP:
                emits(LexicalStructure.LParen, "int", LexicalStructure.RParen);
                emits("Math.round",LexicalStructure.LParen);
                emits("Math.pow", LexicalStructure.LParen);
                binaryExpr.getLeftExpr().visit(this, arg);
                emits(LexicalStructure.Comma);
                binaryExpr.getRightExpr().visit(this, arg);
                emits(LexicalStructure.RParen,LexicalStructure.RParen);
                break;
            case EQ:
                if (binaryExpr.getLeftExpr().getType() == Type.STRING) {
                    binaryExpr.getLeftExpr().visit(this, arg);
                    emits(".equals",LexicalStructure.LParen);
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
        emitl(RBRACE);
        scopeLevel--;

        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        statementBlock.getBlock().visit(this, arg);
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {

        conditionalExpr.getGuardExpr().visit(this, arg);
        emits(LexicalStructure.Question);
        conditionalExpr.getTrueExpr().visit(this, arg);
        emits(LexicalStructure.Colon);
        conditionalExpr.getFalseExpr().visit(this, arg);

        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {

        declaration.getNameDef().visit(this, arg);
        if (declaration.getInitializer() != null) {
            emits(LexicalStructure.Assign);
            declaration.getInitializer().visit(this, true);
        }
        endStatement();

        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        if (lValue.getNameDef() != null)
            lValue.getNameDef().visit(this, arg);
        else {
            var name = getIdentifierName(lValue.getName());
            emits(name);
        }

        // TODO Handle the pixel crap.

        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        var name = makeIdentifierName(nameDef.getName());

        emits(typeToJavaType(nameDef.getType()), name);

        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    private String typeToJavaType(Type type) {
        return switch (type) {
            case STRING -> "String";
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
        emitl("import",(String)arg,".runtime",".ConsoleIO",LexicalStructure.Semi);
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
        switch (unaryExpr.getOp()) {
            case MINUS:
            case BANG:
                emits(LexicalStructure.kind2Char(unaryExpr.getOp()));
                unaryExpr.getExpr().visit(this, arg);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        emits("ConsoleIO.write", LexicalStructure.LParen);
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

    private String makeIdentifierName(String identName) {

        var key = new VarTableEntry(identName, scopeLevel);
        var stringName = "$" + identName + (++id);
        uniqueIdentifierMapper.put(key, stringName);
        return stringName;
    }

}
