package cat.nyaa.playtimetracker.condition;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.*;

public class ConditionTokenizer {

    public static final int TOKEN_UNKNOWN = 0;
    public static final int TOKEN_END = 1;
    public static final int TOKEN_VARIABLE = 2;
    public static final int TOKEN_LITERAL_NUMERIC = 3;
    public static final int TOKEN_BLOCK_LEFT_PARENTHESES = 4; // (
    public static final int TOKEN_BLOCK_RIGHT_PARENTHESES = 5; // )
    public static final int TOKEN_OP_CMP_EQ = 6; // ==
    public static final int TOKEN_OP_CMP_NE = 7; // !=
    public static final int TOKEN_OP_CMP_GT = 8; // >
    public static final int TOKEN_OP_CMP_GE = 9; // >=
    public static final int TOKEN_OP_CMP_LT = 10; // <
    public static final int TOKEN_OP_CMP_LE = 11; // <=
    public static final int TOKEN_OP_LOGIC_AND = 12; // &&
    public static final int TOKEN_OP_LOGIC_OR = 13;  // ||
    public static final int TOKEN_OP_LOGIC_NOT = 14; // !

    private final Char2ObjectMap<FixedToken> fixedTokens;

    public ConditionTokenizer() {
        this.fixedTokens = new Char2ObjectOpenHashMap<>();
        register("(", TOKEN_BLOCK_LEFT_PARENTHESES);
        register(")", TOKEN_BLOCK_RIGHT_PARENTHESES);
        register("==", TOKEN_OP_CMP_EQ);
        register("!=", TOKEN_OP_CMP_NE);
        register(">", TOKEN_OP_CMP_GT);
        register(">=", TOKEN_OP_CMP_GE);
        register("<", TOKEN_OP_CMP_LT);
        register("<=", TOKEN_OP_CMP_LE);
        register("&&", TOKEN_OP_LOGIC_AND);
        register("||", TOKEN_OP_LOGIC_OR);
        register("!", TOKEN_OP_LOGIC_NOT);
    }

    private void register(String value, int type) {
        char c = value.charAt(0);
        FixedToken token = new FixedToken(type, value);
        FixedToken old = this.fixedTokens.get(c);
        if (old == null) {
            this.fixedTokens.put(c, token);
        } else {
            token = old.insert(token);
            if(token != old) {
                this.fixedTokens.put(c, token);
            }
        }
    }

    public Reader parse(String expression) {
        return new Reader(this, expression);
    }

    private boolean skipWhitespace(Reader reader) {
        while (reader.index < reader.expression.length()) {
            char c = reader.expression.charAt(reader.index);
            if (Character.isWhitespace(c)) {
                reader.index++;
            } else {
                return false;
            }
        }
        return true;
    }

    private Token nextFixed(Reader reader) {
        int i = reader.index;
        char c = reader.expression.charAt(i);
        FixedToken fixedToken = this.fixedTokens.get(c);
        char[] buf = null;
        while (fixedToken != null) {
            final int size = fixedToken.value.length;
            final int iEnd = i + size;
            if (iEnd <= reader.expression.length()) {
                if (buf == null) {
                    buf = new char[size];
                    reader.expression.getChars(i, iEnd, buf, 0);
                }
                if (Arrays.equals(buf, 0, size, fixedToken.value, 0, size)) {
                    reader.index = iEnd;
                    return new Token(fixedToken.type, new String(fixedToken.value));
                }
            }
            fixedToken = fixedToken.fallback;
        }
        return null;
    }

    private Token nextVariable(Reader reader) {
        int offset = reader.index;
        char c = reader.expression.charAt(offset);
        if(!Character.isJavaIdentifierStart(c)) {
            return null;
        }
        for (offset++; offset < reader.expression.length(); offset++) {
            char c0 = reader.expression.charAt(offset);
            if (!Character.isJavaIdentifierPart(c0)) {
                break;
            }
        }
        String identifier = reader.expression.substring(reader.index, offset);
        reader.index = offset;
        return new Token(TOKEN_VARIABLE, identifier);
    }

    private Token nextNumeric(Reader reader) {
        int offset = reader.index;
        char lastC = reader.expression.charAt(offset);
        int status; // 0: sign 1: integer-first 2: integer, 3: dot, 4: fraction, 5: unit
        if(Character.isDigit(lastC)) {
            status = 1;
        } else if(lastC == '-' || lastC == '+') {
            status = 0;
        } else {
            return null;
        }
        LABELLOOP:
        for (offset++; offset < reader.expression.length(); offset++) {
            char c0 = reader.expression.charAt(offset);
            switch (status) {
                case 0:
                    if (Character.isDigit(c0)) {
                        status = 1;
                        lastC = c0;
                        continue;
                    }
                    return null;
                case 1:
                    if (Character.isDigit(c0)) {
                        if (lastC == '0') {
                            return null;
                        }
                        status = 2;
                        lastC = c0;
                        continue;
                    }
                    if (c0 == '.') {
                        status = 3;
                        lastC = c0;
                        continue;
                    }
                    if (Character.isLetter(c0)) {
                        status = 5;
                        lastC = c0;
                        continue;
                    }
                    break LABELLOOP;
                case 2:
                    if (Character.isDigit(c0)) {
                        lastC = c0;
                        continue;
                    }
                    if (c0 == '.') {
                        status = 3;
                        lastC = c0;
                        continue;
                    }
                    if (Character.isLetter(c0)) {
                        status = 5;
                        lastC = c0;
                        continue;
                    }
                    break LABELLOOP;
                case 3:
                    if (Character.isDigit(c0)) {
                        status = 4;
                        lastC = c0;
                        continue;
                    }
                    return null;
                case 4:
                    if (Character.isDigit(c0)) {
                        lastC = c0;
                        continue;
                    }
                    if (Character.isLetter(c0)) {
                        status = 5;
                        lastC = c0;
                        continue;
                    }
                    break LABELLOOP;
                case 5:
                    if (Character.isLetter(c0)) {
                        lastC = c0;
                        continue;
                    }
                    break LABELLOOP;
            }
        }
        String numerical = reader.expression.substring(reader.index, offset);
        reader.index = offset;
        return new Token(TOKEN_LITERAL_NUMERIC, numerical);
    }

    public static class Reader implements Iterator<Token> {

        private final ConditionTokenizer tokenizer;
        public final String expression;
        private int index;
        private @Nullable ParseException lastException;
        private int lastToken;

        public Reader(ConditionTokenizer tokenizer, String expression) {
            this.tokenizer = tokenizer;
            this.expression = expression;
            this.index = 0;
            this.lastException = null;
            this.lastToken = TOKEN_UNKNOWN;
        }

        public @Nullable ParseException getException() {
            return this.lastException;
        }

        public int getLastToken() {
            return this.lastToken;
        }

        @Override
        public boolean hasNext() {
            return !(this.lastException != null || this.lastToken == TOKEN_END);
        }

        @Override
        public @Nullable Token next() {
            if (this.lastException != null) {
                return null;
            }
            if (this.tokenizer.skipWhitespace(this)) {
                this.lastToken = TOKEN_END;
                return new Token(TOKEN_END, "");
            }
            Token token = this.tokenizer.nextFixed(this);
            if (token != null) {
                this.lastToken = token.type;
                return token;
            }
            token = this.tokenizer.nextVariable(this);
            if (token != null) {
                this.lastToken = token.type;
                return token;
            }
            token = this.tokenizer.nextNumeric(this);
            if (token != null) {
                this.lastToken = token.type;
                return token;
            }
            this.lastException = new ParseException("Invalid input", this.index);
            return null;
        }
    }

    public record Token(int type, String value) {

    }


    private static class FixedToken {

        final int type;
        final char[] value;
        @Nullable FixedToken fallback;

        FixedToken(int type, String value) {
            this.type = type;
            this.value = value.toCharArray();
            this.fallback = null;
        }

        FixedToken insert(FixedToken token) {
            FixedToken current = this;
            FixedToken prev = null;
            do {
                int cmp = current.value.length - token.value.length;
                if (cmp < 0) {
                    break;
                }
                prev = current;
                current = current.fallback;
            } while (current != null);
            if (prev != null) {
                token.fallback = prev.fallback;
                prev.fallback = token;
                return this;
            } else {
                token.fallback = this;
                return token;
            }
        }
    }
}
