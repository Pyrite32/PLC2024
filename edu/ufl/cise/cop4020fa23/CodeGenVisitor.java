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
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;

public class CodeGenVisitor implements ASTVisitor {

    SymbolTable variables = new SymbolTable();
    StringBuilder codebase = new StringBuilder();
    String packageName;

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg)
            throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
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

    private String javaify(Type type) {
        return type.toString().toLowerCase();
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        
        var ident = program.getName();
        var type = program.getType();
        // arg is the package name.
        codify("public class ", ident, "{ public static ", javaify(type) , " apply ("  );
        codify(")");

        // temporary
        codify("{}");

        codify("}");

        // TODO Auto-generated method stub
        return codebase.toString();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        // TODO Auto-generated method stub
        return null;
    }

    private void codify(String... strings) {
        StringBuilder temp = new StringBuilder();
        for (var string : strings) {
            temp.append(string);
        }
        codebase.append(temp);
    }

    
    
}
