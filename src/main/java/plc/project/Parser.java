package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 * <p>
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 * <p>
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new java.util.ArrayList<>(Collections.emptyList());
        List<Ast.Function> functions = new java.util.ArrayList<>(Collections.emptyList());
        while (peek("LIST") || peek("VAR") || peek("VAL")) {
            globals.add(parseGlobal());
        }

        while (peek("FUN")) {
            functions.add(parseFunction());
        }

        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if (peek("LIST")) {
            return parseList();
        } else if (peek("VAR")) {
            return parseMutable();
        } else if (peek("VAL")) {
            return parseImmutable();
        }

        throw new ParseException("Invalid global!", tokens.get(-1).getIndex());
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match("LIST");
        match(Token.Type.IDENTIFIER);
        String type = null;
        String lit = tokens.get(-1).getLiteral();

        if (match(":")) {
            if (match(Token.Type.IDENTIFIER)) {
                type = tokens.get(-1).getLiteral();
            }
        }

        if (!match("=")) throw new ParseException("Invalid list!", tokens.get(-1).getIndex());

        if (!match("[")) throw new ParseException("Invalid list! Missing opening bracket", tokens.get(-1).getIndex());

        Ast.Expression exp = parseExpression();
        while (match(",")) {
            parseExpression();
        }

        if (!match("]")) throw new ParseException("No closing bracket for list!", tokens.get(-1).getIndex());

        return new Ast.Global(lit, type, true, Optional.of(exp));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        match(Token.Type.IDENTIFIER);
        String token = tokens.get(-1).getLiteral();
        String type = null;
        if (match(":")) {
            if (match(Token.Type.IDENTIFIER)) {
                type = tokens.get(-1).getLiteral();
            }
        }
        if (match("=")) {
            Ast.Expression exp = parseExpression();
            return new Ast.Global(token, type, true, Optional.of(exp));
        }

        return new Ast.Global(token, type, true, Optional.empty());
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String type = null;
        match("VAL");
        match(Token.Type.IDENTIFIER);
        String token = tokens.get(-1).getLiteral();
        if (match(":")) {
            match(Token.Type.IDENTIFIER);
            type = tokens.get(-1).getLiteral();
        }
        if (!match("=")) throw new ParseException("Invalid immutable!", tokens.get(-1).getIndex());
        Ast.Expression exp = parseExpression();

        return new Ast.Global(token, type, false, Optional.of(exp));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        String type = null;
        match("FUN");
        match(Token.Type.IDENTIFIER);
        String name = tokens.get(-1).getLiteral();
        List<String> parameters = new java.util.ArrayList<>(Collections.emptyList());
        List<String> paramTypes = new java.util.ArrayList<>(Collections.emptyList());

        if (!match("(")) throw new ParseException("Missing opening parenthesis", tokens.get(-1).getIndex());
        if (match(Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(-1).getLiteral());
            if (match(":")) {
                if (match(Token.Type.IDENTIFIER)) {
                    paramTypes.add(tokens.get(-1).getLiteral());
                }
            }
            while (match(",")) {
                if (!match(Token.Type.IDENTIFIER))
                    throw new ParseException("Trailing comma not allowed!", tokens.get(-1).getIndex());
                parameters.add(tokens.get(-1).getLiteral());
                if (match(":")) {
                    if (match(Token.Type.IDENTIFIER)) {
                        paramTypes.add(tokens.get(-1).getLiteral());
                    }
                }
            }
        }

        if (!match(")")) throw new ParseException("Missing matching closing parenthesis", tokens.get(-1).getIndex());

        if (match(":")) {
            match(Token.Type.IDENTIFIER);
            type = tokens.get(-1).getLiteral();
        }

        if (!match("DO")) throw new ParseException("Invalid function!", tokens.get(-1).getIndex());

        List<Ast.Statement> statements = parseBlock();

        if (!match("END")) throw new ParseException("Invalid function!", tokens.get(-1).getIndex());

        return new Ast.Function(name, parameters, paramTypes, Optional.of(type), statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new java.util.ArrayList<>(Collections.emptyList());
        while (!peek("CASE") && !peek("DEFAULT") && !peek("END") && !peek("ELSE")) {
            statements.add(parseStatement());
        }

        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (match("LET")) {
            return parseDeclarationStatement();
        } else if (match("SWITCH")) {
            return parseSwitchStatement();
        } else if (match("IF")) {
            return parseIfStatement();
        } else if (match("WHILE")) {
            return parseWhileStatement();
        } else if (match("RETURN")) {
            return parseReturnStatement();
        }

        Ast.Expression exp = parseExpression();
        if (match("=")) {
            Ast.Expression other = parseExpression();
            if (!match(";")) throw new ParseException("Missing semicolon after statement!", tokens.get(-1).getIndex());

            return new Ast.Expression.Statement.Assignment(exp, other);
        }

        if (!match(";")) throw new ParseException("Missing semicolon after statement!", tokens.get(-1).getIndex());

        return new Ast.Statement.Expression(exp);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        String type = null;
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid statement!", tokens.get(-1).getIndex());
        } else {
            Token t = tokens.get(-1);

            if (match(":")) {
                match(Token.Type.IDENTIFIER);
                type = tokens.get(-1).getLiteral();
            }

            if (match(";")) {
                return new Ast.Expression.Statement.Declaration(t.getLiteral(), Optional.of(type), Optional.empty());
            }
        }

        Token t = tokens.get(-1);
        if (match("=")) {
            Ast.Expression other = parseExpression();
            if (!match(";")) throw new ParseException("Missing semicolon after statement!", tokens.get(-1).getIndex());

            if (Objects.isNull(type)) {
                return new Ast.Expression.Statement.Declaration(t.getLiteral(), Optional.of(other));
            }

            return new Ast.Expression.Statement.Declaration(t.getLiteral(), Optional.of(type), Optional.of(other));
        }

        throw new ParseException("Invalid declaration statement!", tokens.getIndex());
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression exp = parseExpression();
        if (!match("DO")) throw new ParseException("Invalid if statement!", tokens.get(-1).getIndex());

        List<Ast.Statement> statements = parseBlock();
        List<Ast.Statement> elseStatements = Collections.emptyList();

        if (match("ELSE")) elseStatements = parseBlock();

        if (!match("END")) throw new ParseException("Invalid if statement!", tokens.get(-1).getIndex());
        return new Ast.Statement.If(exp, statements, elseStatements);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        Ast.Expression exp = parseExpression();
        List<Ast.Statement.Case> cases = new java.util.ArrayList<>(Collections.emptyList());
        while (peek("CASE")) {
            cases.add(parseCaseStatement());
        }

        if (match("DEFAULT")) {
            cases.add(parseCaseStatement());
        }

        if (!match("END")) throw new ParseException("Invalid switch statement!", tokens.get(-1).getIndex());

        return new Ast.Statement.Switch(exp, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if (match("CASE")) {
            Ast.Expression exp = parseExpression();
            if (!match(":")) throw new ParseException("Invalid case statement!", tokens.get(-1).getIndex());
            List<Ast.Statement> statements = parseBlock();
            return new Ast.Statement.Case(Optional.of(exp), statements);
        }

        List<Ast.Statement> statements = parseBlock();
        return new Ast.Statement.Case(Optional.empty(), statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        Ast.Expression exp = parseExpression();
        if (!match("DO")) throw new ParseException("Invalid while statement!", tokens.get(-1).getIndex());
        List<Ast.Statement> statements = parseBlock();
        if (!match("END")) throw new ParseException("Invalid while statement!", tokens.get(-1).getIndex());
        return new Ast.Statement.While(exp, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        Ast.Expression exp = parseExpression();
        if (!match(";")) throw new ParseException("Missing semicolon after statement!", tokens.get(-1).getIndex());
        return new Ast.Statement.Return(exp);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = parseComparisonExpression();
        while (peek("&&") || peek("||")) {
            match(Token.Type.OPERATOR);
            Token t = tokens.get(-1);
            Ast.Expression right = parseComparisonExpression();
            left = new Ast.Expression.Binary(t.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();
        while (peek("<") || peek(">") || peek("==") || peek("!=")) {
            match(Token.Type.OPERATOR);
            Token t = tokens.get(-1);
            Ast.Expression right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(t.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();
        while (peek("+") || peek("-")) {
            match(Token.Type.OPERATOR);
            Token t = tokens.get(-1);
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(t.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();
        while (peek("*") || peek("/") || peek("^")) {
            match(Token.Type.OPERATOR);
            Token t = tokens.get(-1);
            Ast.Expression right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(t.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        } else if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        } else if (match("FALSE")) {
            return new Ast.Expression.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            String charToken = tokens.get(-1).getLiteral();
            charToken = charToken.substring(1, charToken.length() - 1);
            charToken = charToken.replace("\\\\", "\\");
            charToken = charToken.replace("\\'", "'");
            charToken = charToken.replace("\\\"", "\"");
            charToken = charToken.replace("\\b", "\b");
            charToken = charToken.replace("\\n", "\n");
            charToken = charToken.replace("\\r", "\r");
            charToken = charToken.replace("\\t", "\t");
            char c = charToken.charAt(0);
            return new Ast.Expression.Literal(c);
        } else if (match(Token.Type.STRING)) {
            String stringToken = tokens.get(-1).getLiteral();
            stringToken = stringToken.substring(1, stringToken.length() - 1);
            stringToken = stringToken.replace("\\\"", "\"");
            stringToken = stringToken.replace("\\'", "'");
            stringToken = stringToken.replace("\\\\", "\\");
            stringToken = stringToken.replace("\\b", "\b");
            stringToken = stringToken.replace("\\n", "\n");
            stringToken = stringToken.replace("\\r", "\r");
            stringToken = stringToken.replace("\\t", "\t");
            return new Ast.Expression.Literal(stringToken);
        } else if (match(")")) {
            throw new ParseException("Invalid closing parenthesis!", tokens.get(-1).getIndex());
        } else if (match("(")) {
            Ast.Expression exp = parseExpression();
            if (!match(")")) throw new ParseException("Missing closing parenthesis!", tokens.get(-1).getIndex());
            return new Ast.Expression.Group(exp);
        } else if (match(Token.Type.IDENTIFIER)) {
            Token t = tokens.get(-1);
            List<Ast.Expression> arguments = new java.util.ArrayList<>(Collections.emptyList());
            if (match("(")) {
                if (match(",")) throw new ParseException("Trailing comma not allowed!", tokens.get(-1).getIndex());

                if (match(")")) return new Ast.Expression.Function(t.getLiteral(), arguments);

                parseExpression();
                arguments.add(new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral()));
                while (match(",")) {
                    parseExpression();
                    arguments.add(new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral()));
                }

                if (!match(")")) throw new ParseException("No closing parenthesis for function!", tokens.get(-1).getIndex());

                if (match(",")) throw new ParseException("Trailing comma not allowed!", tokens.get(-1).getIndex());

                return new Ast.Expression.Function(t.getLiteral(), arguments);

            } else if (peek("[")) {
                String lit = tokens.get(-1).getLiteral();
                match("[");
                Ast.Expression exp = parseExpression();
                if (!match("]")) throw new ParseException("No closing bracket for function!", tokens.get(-1).getIndex());

                return new Ast.Expression.Access(Optional.of(exp), lit);

            } else {
                return new Ast.Expression.Access(Optional.empty(), tokens.get(-1).getLiteral());
            }
        } else {
            throw new ParseException("Invalid expression!", tokens.get(-1).getIndex());
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     * <p>
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        public int getIndex() {
            return index;
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }
}
