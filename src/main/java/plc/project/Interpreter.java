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

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Global> globals = ast.getGlobals();
        globals.forEach(global -> visit(global));
        List<Ast.Function> functions = ast.getFunctions();
        functions.forEach(function -> visit(function));

        Environment.Function main = scope.lookupFunction("main", 0);
        return main.invoke(Collections.emptyList());

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject value = Environment.NIL;
        if (ast.getMutable()) {
            if (ast.getValue().isPresent()) {
                value = visit(ast.getValue().get());
                scope.defineVariable(ast.getName(), true, value);
            } else {
                scope.defineVariable(ast.getName(), true, value);
            }
        } else {
            if (ast.getValue().isPresent()) {
                value = visit(ast.getValue().get());
                scope.defineVariable(ast.getName(), false, value);
            } else {
                throw new RuntimeException("Value must be declared for immutable type");
            }
        }
        return Environment.NIL;
    }

//        throw new UnsupportedOperationException();
//        scope.defineFunction(ast.getName());

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope currentScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope childScope = scope;
            try {
                scope = new Scope(currentScope);
                List<String> params = ast.getParameters();
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(params.get(i), true, args.get(i));

                }

                List<Ast.Statement> statements = ast.getStatements();
                for (int j = 0; j < statements.size(); j++) {
                    visit(statements.get(j));
                }
                return Environment.NIL;
            } catch (Return Ret) {
                return Ret.value;
            } finally {
                scope = childScope;
            }
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        Ast.Expression expression = ast.getExpression();
        visit(expression);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional<Ast.Expression> expression = ast.getValue();
        boolean present = expression.isPresent();
        if (!present)
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        else
            scope.defineVariable(ast.getName(), true, visit(expression.get()));
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        if (receiver instanceof Ast.Expression.Access) {
            Ast.Expression.Access accessReceiver = (Ast.Expression.Access) receiver;
            Environment.Variable env = scope.lookupVariable(accessReceiver.getName());
            if (!env.getMutable())
                throw new RuntimeException("Cannot modify immutable environment variable!");

            if (accessReceiver.getOffset().isPresent()) {
                BigInteger offset = requireType(BigInteger.class, visit(((Ast.Expression.Access) ast.getReceiver()).getOffset().get()));
                List vars = requireType(List.class, env.getValue());
                if (offset.compareTo(BigInteger.ZERO) < 0 || offset.compareTo(BigInteger.valueOf(vars.size() - 1)) > 0)
                    throw new RuntimeException("Offset out of range!");

                vars.set(offset.intValue(), visit(ast.getValue()).getValue());
            } else {
                env.setValue(visit(ast.getValue()));
            }
        } else {
            throw new RuntimeException("Receiver is not of type Ast.Expression.Access!");
        }

        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        scope = new Scope(scope);
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                visit(ast.getThenStatements().get(i));
            }
        } else {
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                visit(ast.getElseStatements().get(i));
            }
        }

        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        scope = new Scope(scope);
        List<Ast.Statement.Case> cases = ast.getCases();
        for (Ast.Statement.Case current : ast.getCases()) {
            if (current.getValue().isPresent()) {
                if (requireType(Comparable.class, visit(ast.getCondition())).equals(requireType(Comparable.class, visit(current)))) {
                    current.getStatements().forEach(curr -> visit(curr));
                    return Environment.NIL;
                }
            }
        }

        int offset = cases.size() - 1;
        List<Ast.Statement> statements = cases.get(offset).getStatements();
        statements.forEach(curr -> visit(curr));
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        Optional<Ast.Expression> expression = ast.getValue();
        return visit(expression.get());
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        }

        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Ast.Expression expression = ast.getValue();
        throw new Return(visit(expression));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (Objects.isNull(literal))
            return Environment.NIL;
        else
            return Environment.create(literal);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        Ast.Expression expression = ast.getExpression();
        return visit(expression);
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject lhs;
        Environment.PlcObject rhs;
        if (Objects.nonNull(ast)) {
            visit(ast.getLeft());
            if (Objects.equals(ast.getOperator(), "&&")) {
                return Environment.create(requireType(Boolean.class, visit(ast.getLeft())) && requireType(Boolean.class, visit(ast.getRight())));
            } else if (Objects.equals(ast.getOperator(), "||")) {
                return Environment.create(requireType(Boolean.class, visit(ast.getLeft())) || requireType(Boolean.class, visit(ast.getRight())));
            } else if (Objects.equals(ast.getOperator(), ">")) {
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) > 0);
            } else if (Objects.equals(ast.getOperator(), "<")) {
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) < 0);
            } else if (Objects.equals(ast.getOperator(), "==")) {
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) == 0);
            } else if (Objects.equals(ast.getOperator(), "!=")) {
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) != 0);
            } else if (Objects.equals(ast.getOperator(), "+")) {
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof String || rhs.getValue() instanceof String) {
                    return Environment.create(lhs.getValue().toString() + rhs.getValue().toString());
                } else if (lhs.getValue() instanceof BigInteger) {
                    BigInteger bigInt = (BigInteger) lhs.getValue();
                    return Environment.create(bigInt.add(requireType(BigInteger.class, rhs)));
                } else if (lhs.getValue() instanceof BigDecimal) {
                    BigDecimal bigDec = (BigDecimal) lhs.getValue();
                    return Environment.create(bigDec.add(requireType(BigDecimal.class, rhs)));
                } else {
                    throw new RuntimeException("Invalid addition objects!");
                }
            } else if (Objects.equals(ast.getOperator(), "-")) {
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigInteger) {
                    BigInteger bigInt = (BigInteger) lhs.getValue();
                    return Environment.create(bigInt.subtract(requireType(BigInteger.class, rhs)));
                } else if (lhs.getValue() instanceof BigDecimal) {
                    BigDecimal bigDec = (BigDecimal) lhs.getValue();
                    return Environment.create(bigDec.subtract(requireType(BigDecimal.class, rhs)));
                } else {
                    throw new RuntimeException("Invalid subtraction objects!");
                }
            } else if (Objects.equals(ast.getOperator(), "*")) {
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigInteger) {
                    BigInteger bigInt = (BigInteger) lhs.getValue();
                    return Environment.create(bigInt.multiply(requireType(BigInteger.class, rhs)));
                } else if (lhs.getValue() instanceof BigDecimal) {
                    BigDecimal bigDec = (BigDecimal) lhs.getValue();
                    return Environment.create(bigDec.multiply(requireType(BigDecimal.class, rhs)));
                } else {
                    throw new RuntimeException("Invalid multiplication objects!");
                }
            } else if (Objects.equals(ast.getOperator(), "/")) {
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigInteger) {
                    if (requireType(BigInteger.class, rhs).compareTo(BigInteger.ZERO) == 0)
                        throw new RuntimeException("Cannot divide by 0");

                    BigInteger bigInt = (BigInteger) lhs.getValue();
                    return Environment.create(bigInt.divide(requireType(BigInteger.class, rhs)));
                } else if (lhs.getValue() instanceof BigDecimal) {
                    if (requireType(BigDecimal.class, rhs).compareTo(BigDecimal.ZERO) == 0)
                        throw new RuntimeException("Cannot divide by 0.0");

                    BigDecimal bigDec = (BigDecimal) lhs.getValue();
                    return Environment.create(bigDec.divide(requireType(BigDecimal.class, rhs), RoundingMode.HALF_EVEN));
                } else {
                    throw new RuntimeException("Invalid division objects!");
                }
            } else if (Objects.equals(ast.getOperator(), "^")) {
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigInteger) {
                    BigInteger power = BigInteger.ONE;
                    for (BigInteger i = BigInteger.ZERO; i.compareTo(requireType(BigInteger.class, rhs)) < 0; i = i.add(BigInteger.ONE))
                        power = power.multiply((BigInteger) lhs.getValue());

                    if (requireType(BigInteger.class, rhs).compareTo(BigInteger.ZERO) < 0)
                        power = BigInteger.ONE.divide(power);

                    return Environment.create(power);
                } else if (lhs.getValue() instanceof BigDecimal) {
                    BigDecimal result = BigDecimal.ONE;
                    for (BigInteger i = BigInteger.ZERO; i.compareTo(requireType(BigInteger.class, rhs)) < 0; i = i.add(BigInteger.ONE))
                        result = result.multiply((BigDecimal) lhs.getValue());


                    if (requireType(BigInteger.class, rhs).compareTo(BigInteger.ZERO) < 0)
                        result = BigDecimal.ONE.divide(result, RoundingMode.HALF_EVEN);

                    return Environment.create(result);
                } else {
                    throw new RuntimeException("Invalid division objects!");
                }
            }

            visit(ast.getRight());
        }

        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        boolean present = ast.getOffset().isPresent();
        if (present) {
            BigInteger offset = requireType(BigInteger.class, visit(ast.getOffset().get()));
            List indices = requireType(List.class, scope.lookupVariable(ast.getName()).getValue());
            if (offset.compareTo(BigInteger.ZERO) < 0 || offset.compareTo(BigInteger.valueOf(indices.size() - 1)) > 0)
                throw new RuntimeException("Offset out of range!");

            return Environment.create(indices.get(offset.intValue()));
        } else {
            return (scope.lookupVariable(ast.getName())).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> args = new java.util.ArrayList<>(Collections.emptyList());
        for (Ast.Expression arg : ast.getArguments())
            args.add(visit(arg));

        return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> values = new java.util.ArrayList<>(Collections.emptyList());
        for (Ast.Expression astValue : ast.getValues())
            values.add(visit(astValue).getValue());

        return Environment.create(values);
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
