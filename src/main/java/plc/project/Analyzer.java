package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {

        for (int i=0; i<ast.getGlobals().size(); i++) {
            visit(ast.getGlobals().get(i));
        }
        for (int i=0; i<ast.getGlobals().size(); i++) {
            visit(ast.getGlobals().get(i));
        }
        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        Environment.Type variableType = Environment.getType(ast.getTypeName());
        String name = ast.getName();
        Environment.PlcObject value=Environment.NIL;
        if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
            requireAssignable(variableType, ast.getValue().get().getType());
        }
        Environment.Variable variable = scope.defineVariable(name, name, variableType,ast.getMutable(), value);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        String name=ast.getName();
        Environment.Type returnType=Environment.Type.NIL;
        List<Environment.Type> parameterTypes =new ArrayList<>();
        List<String> parameterTypeNames = ast.getParameterTypeNames();
        for(String parameterType:parameterTypeNames){
            parameterTypes.add(Environment.getType(parameterType));
        }
        if(ast.getReturnTypeName().isPresent()) {
            returnType=Environment.getType(ast.getReturnTypeName().get());
        }

        Environment.Function funct = scope.defineFunction(name,name,parameterTypes,returnType,args->Environment.NIL);
        ast.setFunction(funct);
        function = ast;
        scope = new Scope(scope);

        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression is not an Ast.Expression.Function.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name=ast.getName();

        Environment.Type type = null;
        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());

        }
        else if (!(ast.getTypeName().isPresent()) && !(ast.getValue().isPresent())) {
            throw new RuntimeException("The declaration has to be a type or value");
        }
        else {
            type = null;
        }
        if(ast.getValue().isPresent()) {
            if (type == null) {
                type = ast.getValue().get().getType();
            }
            visit(ast.getValue().get());
            requireAssignable(type, ast.getValue().get().getType());
        }
       Environment.Variable var= scope.defineVariable(ast.getName(),ast.getName(),type,true,Environment.NIL);
        ast.setVariable(var);
        return null;
        }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver= ast.getReceiver();
        Ast.Expression value= ast.getValue();
        if (!(receiver instanceof Ast.Expression.Access)) {
            throw new RuntimeException("The receiver has to be an access expression.");
        }
        visit(receiver);
        visit(value);
        requireAssignable(receiver.getType(), value.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
