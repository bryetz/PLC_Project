package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

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
        List<Ast.Global> globals = ast.getGlobals();
        for (int i = 0; i < globals.size(); i++) {
            visit(globals.get(i));
        }

        List<Ast.Function> functions = ast.getFunctions();
        for (int i = 0; i < functions.size(); i++) {
            visit(functions.get(i));
        }

        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        boolean present = ast.getValue().isPresent();
        if(present) {
            Ast.Expression val = ast.getValue().get();
            visit(val);
            requireAssignable(Environment.getType(ast.getTypeName()), val.getType());
        }

        String name = ast.getName();
        Environment.Variable variable = scope.defineVariable(name, name, Environment.getType(ast.getTypeName()),ast.getMutable(), Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        String name = ast.getName();
        Environment.Type type = Environment.Type.NIL;
        List<Environment.Type> params = new ArrayList<>(Collections.emptyList());

        List<String> paramTypeNames = ast.getParameterTypeNames();

        for(String parameterType:paramTypeNames)
            params.add(Environment.getType(parameterType));

        if(ast.getReturnTypeName().isPresent())
            type = Environment.getType(ast.getReturnTypeName().get());

        Environment.Function fun = scope.defineFunction(name, name, params, type, args->Environment.NIL);
        ast.setFunction(fun);

        scope = new Scope(scope);
        function = ast;

        List<Ast.Statement> statements = ast.getStatements();
        for (int i = 0; i < statements.size(); i++)
            visit(statements.get(i));

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression is not an Ast.Expression.Function!");
        }

        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type envType = null;
        Optional<Ast.Expression> val = ast.getValue();
        Optional<String> type = ast.getTypeName();
        boolean valPresent = val.isPresent();
        boolean typePresent = type.isPresent();
        if (!valPresent && !typePresent) {
            throw new RuntimeException("Must have at least a type present or a value present!");
        }

        if(typePresent) {
            envType = Environment.getType(type.get());
        }

        if (valPresent) {
            visit(val.get());
            if(Objects.isNull(envType)) {
                envType = val.get().getType();
            }

            requireAssignable(envType, val.get().getType());
        }

        String name = ast.getName();
        Environment.Variable var = scope.defineVariable(name, name, envType, true, Environment.NIL);
        ast.setVariable(var);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        Ast.Expression value = ast.getValue();
        if (!(receiver instanceof Ast.Expression.Access))
            throw new RuntimeException("The receiver has to be an access expression.");

        visit(receiver);
        visit(value);
        requireAssignable(receiver.getType(), value.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);
        requireAssignable(Environment.Type.BOOLEAN, condition.getType());
        if (ast.getThenStatements().isEmpty())
            throw new RuntimeException("Cannot have zero then statements!");

        scope = new Scope(scope);
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }

        scope = scope.getParent();


        scope = new Scope(scope);
        for (Ast.Statement statement : ast.getElseStatements()) {
            visit(statement);
        }

        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);

        List<Ast.Statement.Case> cases = ast.getCases();
        int offset = cases.size() - 1;
        for(int i = 0; i < offset; i++) {
            Ast.Statement.Case caseBlock = cases.get(i);
            Ast.Expression val = caseBlock.getValue().get();
            visit(val);
            requireAssignable(condition.getType(), val.getType());
            visit(caseBlock);
        }

        boolean present = cases.get(offset).getValue().isPresent();
        if (present) {
            throw new RuntimeException("Default case must be present in switch statement!");
        } else {
            visit(cases.get(offset));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement statement : statements)
            visit(statement);

        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);
        requireAssignable(Environment.Type.BOOLEAN, condition.getType());
        scope = new Scope(scope);
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement statement : statements)
            visit(statement);

        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        Ast.Expression val = ast.getValue();
        visit(val);
        Environment.Function fun = function.getFunction();
        requireAssignable(fun.getReturnType(), val.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (Objects.isNull(literal)) {
            ast.setType(Environment.Type.NIL);
        } else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (ast.getLiteral() instanceof BigInteger) {
            BigInteger bigInt = (BigInteger) ast.getLiteral();
            if (bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("BigInteger out of range!");
            }

            ast.setType(Environment.Type.INTEGER);
        } else if (ast.getLiteral() instanceof BigDecimal) {
            BigDecimal bigDec = (BigDecimal) literal;
            double litDub = bigDec.doubleValue();
            if (litDub == Double.POSITIVE_INFINITY || litDub == Double.NEGATIVE_INFINITY) {
                throw new RuntimeException("BigDecimal is out of range!");
            }

            ast.setType(Environment.Type.DECIMAL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if ((ast.getExpression() instanceof Ast.Expression.Binary)) {
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
            return null;
        }

        throw new RuntimeException("Group expression should be an instance of binary expression!");
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (Objects.nonNull(ast)) {
            String op = ast.getOperator();
            visit(ast.getLeft());
            visit(ast.getRight());
            if (op.equals("&&")) {
                if ((ast.getLeft().getType().equals(Environment.Type.BOOLEAN)) && (ast.getRight().getType().equals(Environment.Type.BOOLEAN))) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                } else {
                    throw new RuntimeException("BOOLEAN type must be on both sides of comparison!");
                }
            } else if (op.equals("||")) {
                if ((ast.getLeft().getType().equals(Environment.Type.BOOLEAN)) && (ast.getRight().getType().equals(Environment.Type.BOOLEAN))) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                } else {
                    throw new RuntimeException("BOOLEAN type must be on both sides of comparison!");
                }
            } else if (op.equals(">")) {
                if ((ast.getLeft().getType().equals(Environment.Type.COMPARABLE)) && (ast.getRight().getType().equals(Environment.Type.COMPARABLE))) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                } else {
                    throw new RuntimeException("COMPARABLE type must be present on both sides!");
                }
            } else if (op.equals("<")) {
                if ((ast.getLeft().getType().equals(Environment.Type.COMPARABLE)) && (ast.getRight().getType().equals(Environment.Type.COMPARABLE))) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                } else {
                    throw new RuntimeException("COMPARABLE type must be present on both sides!");
                }
            } else if (op.equals("==")) {
                if ((ast.getLeft().getType().equals(Environment.Type.COMPARABLE)) && (ast.getRight().getType().equals(Environment.Type.COMPARABLE))) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                } else {
                    throw new RuntimeException("COMPARABLE type must be present on both sides!");
                }
            } else if (op.equals("!=")) {
                if ((ast.getLeft().getType().equals(Environment.Type.COMPARABLE)) && (ast.getRight().getType().equals(Environment.Type.COMPARABLE))) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                } else {
                    throw new RuntimeException("COMPARABLE type must be present on both sides!");
                }
            } else if (op.equals("+")) {
                if ((ast.getLeft().getType().equals(Environment.Type.STRING)) || (ast.getRight().getType().equals(Environment.Type.STRING))) {
                    ast.setType(Environment.Type.STRING);
                    return null;
                }

                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                        ast.setType(ast.getLeft().getType());
                        return null;
                    } else {
                        throw new RuntimeException("Right side is not of required type " + (ast.getLeft().getType().getName()));
                    }
                } else {
                    throw new RuntimeException("Only STRING, INTEGER, and DECIMAL are supported for this operation!");
                }
            } else if (op.equals("-")) {
                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                        ast.setType(ast.getLeft().getType());
                        return null;
                    } else {
                        throw new RuntimeException("Right side is not of required type " + (ast.getLeft().getType().getName()));
                    }
                } else {
                    throw new RuntimeException("Only INTEGER and DECIMAL are supported for this operation!");
                }
            } else if (op.equals("*")) {
                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                        ast.setType(ast.getLeft().getType());
                        return null;
                    } else {
                        throw new RuntimeException("Right side is not of required type " + (ast.getLeft().getType().getName()));
                    }
                } else {
                    throw new RuntimeException("Only INTEGER and DECIMAL are supported for this operation!");
                }
            } else if (op.equals("/")) {
                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                        ast.setType(ast.getLeft().getType());
                        return null;
                    } else {
                        throw new RuntimeException("Right side is not of required type " + (ast.getLeft().getType().getName()));
                    }
                } else {
                    throw new RuntimeException("Only INTEGER and DECIMAL are supported for this operation!");
                }
            } else if (op.equals("^")) {
                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    if (ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                        ast.setType(ast.getLeft().getType());
                        return null;
                    } else {
                        throw new RuntimeException("Power must be of type INTEGER");
                    }
                } else {
                    throw new RuntimeException("Base must be of type INTEGER");
                }
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        Optional<Ast.Expression> offset = ast.getOffset();
        String name = ast.getName();
        boolean present = offset.isPresent();
        if (present) {
            visit(offset.get());
            if (offset.get().getType() != Environment.Type.INTEGER) {
                throw new RuntimeException("Offset should be of type integer!");
            }
        }

        Environment.Variable env = scope.lookupVariable(name);
        ast.setVariable(env);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List<Ast.Expression> arguments = ast.getArguments();
        Environment.Function fun = scope.lookupFunction(name, arguments.size());
        ast.setFunction(fun);
        List<Environment.Type> parameters = fun.getParameterTypes();
        for (int i = 0; i < parameters.size(); i++) {
            visit(arguments.get(i));
            requireAssignable(fun.getParameterTypes().get(i), arguments.get(i).getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        Environment.Type type = ast.getType();
        if (type.equals(Environment.Type.ANY))
            return null;

        for (Ast.Expression val : ast.getValues()) {
            visit(val);
            Environment.Type valType = val.getType();
            requireAssignable(type, valType);
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type))
            return;
        else if (target.equals(Environment.Type.ANY))
            return;
        else if (target.equals(Environment.Type.COMPARABLE)) {
            if ((type.equals(Environment.Type.INTEGER)) || (type.equals(Environment.Type.DECIMAL)) || (type.equals(Environment.Type.CHARACTER)) || (type.equals(Environment.Type.STRING)))
                return;
            else
                throw new RuntimeException("Invalid COMPARABLE! Cannot be of type " + type.getName());
        } else {
            throw new RuntimeException("Cannot assign target to specified type!");
        }
    }
}
