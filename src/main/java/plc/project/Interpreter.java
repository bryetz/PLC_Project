package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

//        scope.defineFunction("logarithm",1, args -> {
//                    if(!(args.get(0).getValue() instanceof BigDecimal)){
//                        throw new RuntimeException("Expected a BigDecimal, received " + args.get(0).getValue().getClass().getName()+".");
//                    }
//                    BigDecimal bd1 = (BigDecimal) args.get(0).getValue();
//                    BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));
//                    BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
//                    return Environment.create(result);
//                }
//        );
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Global> globals=ast.getGlobals();
        globals.forEach(global->visit(global));
        List<Ast.Function> functions = ast.getFunctions();
        functions.forEach(function->visit(function));

            Environment.Function main = scope.lookupFunction("main", 0);
            return main.invoke(Collections.emptyList());

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject value=Environment.NIL;
        if (ast.getMutable()) {
            if (ast.getValue().isPresent()) {
                value=visit(ast.getValue().get());
                scope.defineVariable(ast.getName(), true, value);
            }
            else {
                scope.defineVariable(ast.getName(), true, value);
            }
        }
        else {
            if(ast.getValue().isPresent()) {
                value=visit(ast.getValue().get());
                scope.defineVariable(ast.getName(), false, value);
            }
            else {
                throw new RuntimeException("Value must be declared for immutable type");
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope currentScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope childScope = scope;
            try {
            scope = new Scope(currentScope);
            List<String> params = ast.getParameters();
            for(int i=0; i< ast.getParameters().size(); i++) {
                scope.defineVariable(params.get(i), true, args.get(i));

            }

                List<Ast.Statement> statements = ast.getStatements();
                for (int j = 0; j < statements.size(); j++) {
                    visit(statements.get(j));
                }
                return Environment.NIL;
            } catch(Return Ret) {
                return Ret.value;
            }
            finally {
                scope=childScope;
            }
        });
            return Environment.NIL;
        }

//        throw new UnsupportedOperationException();
//        scope.defineFunction(ast.getName());


    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException();
        }
        throw new UnsupportedOperationException();
    }




    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Ast.Expression expression = ast.getValue();
        throw new Return(visit(expression));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        Object literal=ast.getLiteral();
        if(Objects.isNull(literal)) {
            return new Environment.PlcObject(this.scope,Environment.NIL.getValue());
        }
        else {
            return new Environment.PlcObject(this.scope, literal);
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
