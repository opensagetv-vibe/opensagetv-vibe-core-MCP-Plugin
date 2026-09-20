package org.opensagetv.vibe.coremcp;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

final class Json {
    private Json() { }

    static String encode(Object value) {
        StringBuilder out = new StringBuilder(256);
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String || value instanceof Character) {
            quote(out, String.valueOf(value));
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(String.valueOf(value));
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Object raw : ((Map) value).entrySet()) {
                Map.Entry entry = (Map.Entry) raw;
                if (!first) out.append(',');
                first = false;
                quote(out, String.valueOf(entry.getKey()));
                out.append(':');
                append(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Iterable) {
            out.append('[');
            Iterator iterator = ((Iterable) value).iterator();
            boolean first = true;
            while (iterator.hasNext()) {
                if (!first) out.append(',');
                first = false;
                append(out, iterator.next());
            }
            out.append(']');
        } else if (value.getClass().isArray()) {
            out.append('[');
            for (int index = 0; index < Array.getLength(value); index++) {
                if (index > 0) out.append(',');
                append(out, Array.get(value, index));
            }
            out.append(']');
        } else {
            quote(out, String.valueOf(value));
        }
    }

    private static void quote(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        String hex = Integer.toHexString(ch);
                        out.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) out.append('0');
                        out.append(hex);
                    } else {
                        out.append(ch);
                    }
            }
        }
        out.append('"');
    }
}

