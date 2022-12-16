package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        indent++;
        if(!ast.getGlobals().isEmpty()) {
            newline(0);
            for(int i=0; i<ast.getGlobals().size(); i++) {
                newline(indent);
                print(ast.getGlobals().get(i));
            }
        }
        newline(0);
        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);
        for(int i=0; i<ast.getFunctions().size(); i++){
            newline(indent);
            print(ast.getFunctions().get(i));
        }
        newline(0);
        newline(0);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(!ast.getMutable()){
            print("final");
        }
        print(ast.getVariable().getType().getJvmName());
        if(ast.getValue().isPresent()){
            if(ast.getValue().get() instanceof Ast.Expression.PlcList){
                print("[]");
            }
        }
        print(" ");
        print(ast.getName());
        if(ast.getValue().isPresent()){
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getName(), "(");
        if(ast.getParameters().size() > 1) {
            for(int i = 0; i < ast.getParameters().size() - 1; i++) {
                print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName(), " ", ast.getParameters().get(i), ", ");
            }
            print(Environment.getType(ast.getParameterTypeNames().get(ast.getParameters().size() - 1)).getJvmName(), " ", ast.getParameters().get(ast.getParameters().size() - 1));
        }
        else if(ast.getParameters().size() == 1) {
            print(Environment.getType(ast.getParameterTypeNames().get(0)).getJvmName());
            print(" ");
            print(ast.getParameters().get(0));
        }
        print(")");
        print(" {");
        indent++;
        for(int i=0; i<ast.getStatements().size();i++){
            newline(indent);
            print(ast.getStatements().get(i));
        }
        indent--;
        if(!(ast.getStatements().isEmpty())) {
            newline(indent);
            }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name=ast.getName();
        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(name);
        if(ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ") {");
        indent++;
        for(int i=0; i<ast.getThenStatements().size();i++){
            newline(indent);
            print(ast.getThenStatements().get(i));
        }
        indent--;
        if(!(ast.getThenStatements().isEmpty())) {
            newline(indent);
        }
        print("}");
        if(!(ast.getElseStatements().isEmpty())) {
            print(" else {");
            indent++;
            for(int i=0; i<ast.getElseStatements().size();i++){
                newline(indent);
                print(ast.getElseStatements().get(i));
            }
            indent--;
            if(!(ast.getElseStatements().isEmpty())) {
                newline(indent);
            }
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (", ast.getCondition(), ") {");
        indent++;
        for(Ast.Statement.Case c: ast.getCases()) {
            print(c);
        }
        indent--;
        if(!(ast.getCases().isEmpty())) {
            newline(indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        newline(indent);
        if(!ast.getValue().isPresent()) {
            print("default:");
        }
        else {
            print("case ", ast.getValue().get(), ":");
        }
        indent++;
        for(int i=0; i<ast.getStatements().size();i++){
            newline(indent);
            print(ast.getStatements().get(i));
        }
        if(ast.getValue().isPresent()){
            newline(indent);
            print("break;");
        }
        indent--;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");
        indent++;
        for(int i=0; i<ast.getStatements().size();i++){
            newline(indent);
            print(ast.getStatements().get(i));
        }
        indent--;
        if(!(ast.getStatements().isEmpty())) {
            newline(indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Environment.Type type = ast.getType();
        if(type == Environment.Type.STRING) {
            print("\"", ast.getLiteral(), "\"");
        }
        else if(type == Environment.Type.CHARACTER) {
            print("'", ast.getLiteral(), "'");
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if(ast.getOperator().equals("^")) {
            print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ")");
        }
        else {
            print(ast.getLeft(), " ", ast.getOperator(), " ", ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        if(ast.getOffset().isPresent()) {
            print("[", ast.getOffset().get(), "]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName() + "(");
        if(ast.getArguments().size() == 1) {
            print(ast.getArguments().get(0));
        }
        else if(ast.getArguments().size() > 1) {
            for(int i = 0; i < ast.getArguments().size() - 1; i++) {
                print(ast.getArguments().get(i), ", ");
            }
            print(ast.getArguments().get(ast.getArguments().size() - 1));
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        if(ast.getValues().size() > 1) {
            for(int i = 0; i < ast.getValues().size() - 1; i++) {
                print(ast.getValues().get(i), ", ");
            }
            print(ast.getValues().get(ast.getValues().size()-1));
        }
        else if(ast.getValues().size()== 1) {
            print(ast.getValues().get(0));
        }
        print("}");
        return null;
    }

}
