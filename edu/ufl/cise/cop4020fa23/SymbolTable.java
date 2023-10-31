package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.Declaration;
import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.ast.SyntheticNameDef;
import edu.ufl.cise.cop4020fa23.ast.Type;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Queue;

public class SymbolTable {

    private Stack<HashMap<String, NameDef>> names;
    private List<List<NameDef>> inherited;
    private Queue<NameDef> addQueue;

    // private Stack<Boolean> swizzlingX;
    // private Stack<Boolean> swizzlingY;
    // private Stack<Boolean> swizzlingZ;

    // private int index;

    public SymbolTable() {
        super();
        names = new Stack<HashMap<String, NameDef>>();
        names.push(new HashMap<String, NameDef>());

        inherited = new ArrayList<List<NameDef>>();
        inherited.add(new ArrayList<NameDef>());

        addQueue = new LinkedList<NameDef>();

        // no
        // set up swizzle stacks
        // swizzlingX = new Stack<Boolean>();
        // swizzlingY = new Stack<Boolean>();
        // swizzlingZ = new Stack<Boolean>();

        // swizzlingX.push(false);
        // swizzlingY.push(false);
        // swizzlingZ.push(false);

        // index = 0;
    }

    private HashMap<String, NameDef> current() {
        return names.peek();
    }

    public void put(String key, NameDef value) throws TypeCheckException {
        if (current().get(key) != null) {
            throw new TypeCheckException(value.firstToken().sourceLocation(),
                    "Attempted to record duplicate name definition");
        }
        current().put(key, value);
    }

    public void put(String key, Declaration value) throws TypeCheckException {
        put(key, value.getNameDef());
    }

    // only one that is swizzle friendly

    public Type typeOf(String key) throws TypeCheckException {
        
        

        var val = find(key);

        if (val == null) {
            throw new TypeCheckException(key + " does not exist in the symbol table.");
        }
        return val.getType();
    }

    public NameDef get(String key) throws TypeCheckException {
        var val = find(key);
        if (val == null) {
            throw new TypeCheckException(key + " does not exist in the symbol table.");
        }
        return val;
    }

    public boolean has(String key) {
        try {
            find(key);    
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void enterScope() {
        // inherit upper-scope symbols
        var upperScopeElems = current().values();
        var nextInherited = new ArrayList<NameDef>();
        nextInherited.addAll(upperScopeElems);
        inherited.add(nextInherited);

        // load a new hashmap
        names.add(new HashMap<String, NameDef>());
    }

    public void exitScope() {
        // delete upper-scope symbols
        inherited.remove(inherited.size() - 1);

        // delete hashmap
        names.pop();
    }

    public void addTemporarySwizzle(String swizzle) {
        var syndef = new SyntheticNameDef(swizzle);
        current().put(swizzle, syndef);
    }

    public void removeTemporarySwizzle(String swizzle) throws TypeCheckException {
        var syndef = current().get(swizzle);
        if (syndef == null) {
            throw new TypeCheckException("The expected swizzle " + swizzle + " could not be found");
        }
        if (!(syndef instanceof SyntheticNameDef)) {
            throw new TypeCheckException("The identifier " + swizzle + " does not link to a swizzle.");
        }
        current().remove(swizzle);
    }

    public void clearSwizzles() {
        var swizzles = current().values();
        var swizzleNameList = new LinkedList<String>();
        for (var swizzle : swizzles) {
            if (swizzle instanceof SyntheticNameDef)
                swizzleNameList.add(swizzle.getName());
        }
        for (var name : swizzleNameList) {
            current().remove(name);
        }
    }

    // called only when finding a symbol
    private NameDef find(String name) throws TypeCheckException {

        // try to find in current
        var currentFind = current().get(name);
        if (currentFind != null) {
            return currentFind;
        }

        for (int i = inherited.size() - 1; i >= 0; i--) {
            var list = inherited.get(i);
            for (var nd : list) {
                if (nd.getName().equals(name)) {
                    return nd;
                }
            }
        }
        throw new TypeCheckException("the symbol " + name + " could not be found.");
    }

    public void putDeferred(NameDef val) {
        addQueue.add(val);
    }

    public void flushDeferredPuts() throws TypeCheckException {
        while (addQueue.size() != 0 ) {
            var out = addQueue.poll();
            put(out.getName(), out);
        }
    }
}
