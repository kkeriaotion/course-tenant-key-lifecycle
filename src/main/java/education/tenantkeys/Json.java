package education.tenantkeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            map.forEach((key, item) -> entries.add(write(String.valueOf(key)) + ":" + write(item)));
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Iterable<?> items) {
            List<String> values = new ArrayList<>();
            items.forEach(item -> values.add(write(item)));
            return "[" + String.join(",", values) + "]";
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    static Map<String, Object> readObject(String source) {
        Object value = new Parser(source).parse();
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException("Expected JSON object");
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static final class Parser {
        private final String source;
        private int position;

        Parser(String source) { this.source = source; }

        Object parse() {
            Object value = value();
            whitespace();
            if (position != source.length()) throw new IllegalArgumentException("Trailing JSON content");
            return value;
        }

        private Object value() {
            whitespace();
            if (position >= source.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            return switch (source.charAt(position)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            position++;
            Map<String, Object> map = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return map;
            do {
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                map.put(key, value());
                whitespace();
            } while (take(','));
            expect('}');
            return map;
        }

        private List<Object> array() {
            position++;
            List<Object> list = new ArrayList<>();
            whitespace();
            if (take(']')) return list;
            do {
                list.add(value());
                whitespace();
            } while (take(','));
            expect(']');
            return list;
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < source.length()) {
                char c = source.charAt(position++);
                if (c == '"') return result.toString();
                if (c != '\\') { result.append(c); continue; }
                if (position >= source.length()) throw new IllegalArgumentException("Bad JSON escape");
                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append((char) Integer.parseInt(source.substring(position, position += 4), 16));
                    default -> throw new IllegalArgumentException("Bad JSON escape");
                }
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }

        private Object number() {
            int start = position;
            while (position < source.length() && "-+0123456789.eE".indexOf(source.charAt(position)) >= 0) position++;
            String token = source.substring(start, position);
            return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private Object literal(String text, Object value) {
            if (!source.startsWith(text, position)) throw new IllegalArgumentException("Bad JSON literal");
            position += text.length();
            return value;
        }

        private void whitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean take(char expected) {
            if (position < source.length() && source.charAt(position) == expected) { position++; return true; }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected " + expected);
        }
    }
}
