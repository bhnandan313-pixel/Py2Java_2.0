package com.py2java.cloudide.translator;

import com.py2java.cloudide.model.TranslationOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedCodeTranslationService implements CodeTranslationService {
    private static final Pattern FUNCTION = Pattern.compile(
            "def\\s+([A-Za-z_]\\w*)\\s*\\((.*?)\\)\\s*:", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "([A-Za-z_]\\w*)\\s*=\\s*(.+)$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern INTEGER = Pattern.compile("-?\\d+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern CALL = Pattern.compile(
            "[A-Za-z_]\\w*\\s*\\(", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String EXPLANATION =
            "Rule-based conversion of common statements. Review types, control flow, functions, and library usage before compiling.";

    @Override
    public TranslationOutput translate(String code) {
        List<String> lines = splitLines(expandTabs(code));
        List<String> body = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> declared = new HashSet<>();
        List<Integer> indentStack = new ArrayList<>(List.of(0));

        for (int index = 0; index < lines.size(); index++) {
            String raw = lines.get(index);
            String statement = raw.strip();
            if (statement.isEmpty() || statement.startsWith("#")) {
                if (statement.startsWith("#")) {
                    body.add("        // " + statement.substring(1).strip());
                }
                continue;
            }

            int indent = leadingSpaceCount(raw);
            while (indentStack.size() > 1 && indent < indentStack.getLast()) {
                body.add("    ".repeat(indentStack.size()) + "}");
                indentStack.removeLast();
            }

            if (statement.startsWith("def ")) {
                Matcher matcher = FUNCTION.matcher(statement);
                if (matcher.lookingAt()) {
                    String parameters = java.util.Arrays.stream(matcher.group(2).split(",", -1))
                            .map(String::strip)
                            .filter(parameter -> !parameter.isEmpty())
                            .map(parameter -> "var " + parameter.split("=", -1)[0].strip())
                            .collect(java.util.stream.Collectors.joining(", "));
                    body.add("    public static void " + matcher.group(1) + "(" + parameters + ") {");
                    indentStack.add(indent + 4);
                } else {
                    warnings.add("Line " + (index + 1) + ": function signature needs manual review.");
                }
                continue;
            }
            if (statement.startsWith("if ") && statement.endsWith(":")) {
                body.add("    ".repeat(indentStack.size() + 1)
                        + "if (" + pythonExpression(statement.substring(3, statement.length() - 1)) + ") {");
                indentStack.add(indent + 4);
                continue;
            }
            if (statement.equals("else:")) {
                body.add("    ".repeat(indentStack.size() + 1) + "else {");
                indentStack.add(indent + 4);
                continue;
            }
            if (statement.startsWith("print(") && statement.endsWith(")")) {
                body.add("    ".repeat(indentStack.size() + 1)
                        + "System.out.println("
                        + pythonExpression(statement.substring(6, statement.length() - 1)) + ");");
                continue;
            }

            Matcher assignment = ASSIGNMENT.matcher(statement);
            if (assignment.matches()) {
                String name = assignment.group(1);
                String value = assignment.group(2);
                String type = value.startsWith("'") || value.startsWith("\"") ? "String"
                        : INTEGER.matcher(value).matches() ? "int"
                        : DECIMAL.matcher(value).matches() ? "double" : "var";
                if (declared.contains(name)) {
                    body.add("    ".repeat(indentStack.size() + 1)
                            + name + " = " + pythonExpression(value) + ";");
                } else {
                    body.add("    ".repeat(indentStack.size() + 1)
                            + type + " " + name + " = " + pythonExpression(value) + ";");
                    declared.add(name);
                }
                continue;
            }
            if (statement.startsWith("import ") || statement.startsWith("from ")
                    || statement.startsWith("class ") || statement.equals("try:")
                    || statement.startsWith("except ") || statement.startsWith("with ")) {
                warnings.add("Line " + (index + 1) + ": '" + statement.split("\\s+", 2)[0]
                        + "' is not translated automatically.");
                body.add("    ".repeat(indentStack.size() + 1) + "// TODO: " + statement);
                continue;
            }
            if (statement.startsWith("return")) {
                body.add("    ".repeat(indentStack.size() + 1) + statement + ";");
                continue;
            }
            if (CALL.matcher(statement).lookingAt()) {
                body.add("    ".repeat(indentStack.size() + 1) + pythonExpression(statement) + ";");
                continue;
            }
            warnings.add("Line " + (index + 1) + ": review this statement manually.");
            body.add("    ".repeat(indentStack.size() + 1) + "// TODO: " + statement);
        }

        while (indentStack.size() > 1) {
            body.add("    ".repeat(indentStack.size()) + "}");
            indentStack.removeLast();
        }
        String java = "public class Main {\n    public static void main(String[] args) {\n"
                + String.join("\n", body) + "\n    }\n}\n";
        return new TranslationOutput(java, List.copyOf(warnings), EXPLANATION);
    }

    private String pythonExpression(String expression) {
        return expression.strip()
                .replaceAll("(?U)\\bTrue\\b", "true")
                .replaceAll("(?U)\\bFalse\\b", "false")
                .replaceAll("(?U)\\bNone\\b", "null")
                .replaceAll("(?U)\\band\\b", "&&")
                .replaceAll("(?U)\\bor\\b", "||")
                .replaceAll("(?U)\\bnot\\b", "!");
    }

    private String expandTabs(String code) {
        StringBuilder expanded = new StringBuilder();
        int column = 0;
        for (int index = 0; index < code.length(); index++) {
            char character = code.charAt(index);
            if (character == '\t') {
                int spaces = 4 - column % 4;
                expanded.append(" ".repeat(spaces));
                column += spaces;
            } else {
                expanded.append(character);
                if (character == '\n' || character == '\r' || character == '\u000b'
                        || character == '\f' || character == '\u001c' || character == '\u001d'
                        || character == '\u001e' || character == '\u0085'
                        || character == '\u2028' || character == '\u2029') {
                    column = 0;
                } else {
                    column++;
                }
            }
        }
        return expanded.toString();
    }

    private List<String> splitLines(String code) {
        String[] parts = code.split("\\r\\n|[\\n\\r\\u000b\\f\\u001c-\\u001e\\u0085\\u2028\\u2029]", -1);
        int length = parts.length;
        if (length > 1 && endsWithLineBreak(code)) {
            length--;
        }
        return java.util.Arrays.asList(parts).subList(0, length);
    }

    private boolean endsWithLineBreak(String code) {
        if (code.isEmpty()) {
            return false;
        }
        char last = code.charAt(code.length() - 1);
        return last == '\n' || last == '\r' || last == '\u000b' || last == '\f'
                || last == '\u001c' || last == '\u001d' || last == '\u001e'
                || last == '\u0085' || last == '\u2028' || last == '\u2029';
    }

    private int leadingSpaceCount(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == ' ') {
            index++;
        }
        return index;
    }
}
