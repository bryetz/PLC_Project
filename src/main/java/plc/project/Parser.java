package plc.project;

import jdk.nashorn.internal.runtime.ParserException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }
    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
//        parseExpression();
//        if(match("=")){
//
//        }
//        return
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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
        while(peek("&&")||peek("||")){
            match(Token.Type.OPERATOR);
            Token t=tokens.get(-1);
            Ast.Expression right= parseComparisonExpression();
            left =new Ast.Expression.Binary(t.getLiteral(),left, right);

        }
       return left;
       // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();
        while(peek("<")||peek(">")||peek("==")||peek("!=")){
            match(Token.Type.OPERATOR);
            Token t=tokens.get(-1);
            Ast.Expression right= parseAdditiveExpression();
            left =new Ast.Expression.Binary(t.getLiteral(),left, right);

        }
        return left;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();
        while(peek("+")||peek("-")){
            match(Token.Type.OPERATOR);
            Token t=tokens.get(-1);
            Ast.Expression right= parseMultiplicativeExpression();
            left =new Ast.Expression.Binary(t.getLiteral(),left, right);

        }
        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();
        while(peek("*")||peek("/")||peek("^")){
            match(Token.Type.OPERATOR);
            Token t=tokens.get(-1);
            Ast.Expression right= parsePrimaryExpression();
            left =new Ast.Expression.Binary(t.getLiteral(),left, right);

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
      if(match("TRUE")){
          return new Ast.Expression.Literal(true);
      } else if(match("NIL")){
          return new Ast.Expression.Literal(null);
      } else if(match("FALSE")) {
          return new Ast.Expression.Literal(false);
      } else if(match(Token.Type.INTEGER)){
          return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
      }
      else if(match(Token.Type.DECIMAL)){
          return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
      }
      else if(match(Token.Type.CHARACTER)){
            String charToken=tokens.get(-1).getLiteral();
            charToken=charToken.substring(1, charToken.length()-1);
            charToken=charToken.replace("\\\\", "\\");
            charToken=charToken.replace("\\'", "'");
            charToken=charToken.replace("\\\"","\"");
            charToken=charToken.replace("\\b", "\b");
            charToken=charToken.replace("\\n","\n");
            charToken=charToken.replace("\\r", "\r");
            charToken=charToken.replace("\\t","\t");
            char c=charToken.charAt(0);
            return new Ast.Expression.Literal(c);
      } else if(match(Token.Type.STRING)){
          String stringToken=tokens.get(-1).getLiteral();
          stringToken=stringToken.substring(1, stringToken.length()-1);
          stringToken=stringToken.replace("\\\"","\"");
          stringToken=stringToken.replace("\\'","'");
          stringToken=stringToken.replace("\\\\","\\");
          stringToken=stringToken.replace("\\b","\b");
          stringToken=stringToken.replace("\\n","\n");
          stringToken=stringToken.replace("\\r","\r");
          stringToken=stringToken.replace("\\t","\t");
          return new Ast.Expression.Literal(stringToken);
      }
       else if(match("(")){
            parseExpression();
            if(!match(")")){

                throw new ParseException("No closing parentheses for primary expression", -1);
            }
        }
       else if(match(Token.Type.IDENTIFIER)){
           Token t=tokens.get(-1);
          List<Ast.Expression> arguments = new java.util.ArrayList<>(Collections.emptyList());
           if(match("(")) {
                if(peek(")")) {
                    match(")");
                    return new Ast.Expression.Function(t.getLiteral(), arguments);
                }
               Ast.Expression exp=parseExpression();

                arguments.add(new Ast.Expression.Access(Optional.empty(),tokens.get(-1).getLiteral()));
                   while(match(",")){
                       parseExpression();
                       arguments.add(new Ast.Expression.Access(Optional.empty(),tokens.get(-1).getLiteral()));

                   }
                   if(!peek(")")){
//                    throw new ParseException("No closing parenthesis for function ",-1);
               } else{
                       match(")");
                   }
               return new Ast.Expression.Function(t.getLiteral(),arguments);
               } else if(match("[")){
               Optional<Ast.Expression> a= Optional.of(new Ast.Expression.Literal(tokens.get(-1).getLiteral()));
               if(!peek("]")) {
                   throw new ParseException("No closing bracket for function ",-1);
               }

               match("]");
               arguments.add(new Ast.Expression.Access(a,tokens.get(-1).getLiteral()));
           }

           }

       else {
          throw new ParseException("Invalid Primary Expression", -1);
      }
       Ast.Expression ae= new Ast.Expression();
        return ae;
        //throw new UnsupportedOperationException(); //TODO
    }


    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
//        throw new UnsupportedOperationException(); //TODO (in lecture)
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
            boolean peek=peek(patterns);
            if(peek){
                for(int i=0; i<patterns.length; i++){
                    tokens.advance();
                }
            }
//        throw new UnsupportedOperationException(); //TODO (in lecture)
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

        public int getIndex(){
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