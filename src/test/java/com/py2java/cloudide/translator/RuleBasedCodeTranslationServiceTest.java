package com.py2java.cloudide.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.py2java.cloudide.model.TranslationOutput;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedCodeTranslationServiceTest {
    private final RuleBasedCodeTranslationService service = new RuleBasedCodeTranslationService();

    @Test
    void translatesSupportedStatementsWithoutChangingOutputShape() {
        TranslationOutput result = service.translate("name = \"World\"\nprint(\"Hello, \" + name)");

        assertEquals("""
                public class Main {
                    public static void main(String[] args) {
                        String name = "World";
                        System.out.println("Hello, " + name);
                    }
                }
                """, result.java());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void preservesWarningsAndIndentationBehaviorForUnsupportedStatements() {
        TranslationOutput result = service.translate("import os\nvalue = None\nunknown");

        assertEquals("""
                public class Main {
                    public static void main(String[] args) {
                        // TODO: import os
                        var value = null;
                        // TODO: unknown
                    }
                }
                """, result.java());
        assertEquals(List.of(
                "Line 1: 'import' is not translated automatically.",
                "Line 3: review this statement manually."), result.warnings());
    }

    @Test
    void translatesTabsAndExpressionKeywords() {
        TranslationOutput result = service.translate("if True and not False:\n\tprint(None)");

        assertEquals("""
                public class Main {
                    public static void main(String[] args) {
                        if (true && ! false) {
                            System.out.println(null);
                        }
                    }
                }
                """, result.java());
    }

    @Test
    void preservesUnicodeContinuationCharactersInPythonNames() {
        TranslationOutput result = service.translate("nameé = \"World\"\nprint(nameé)");

        assertEquals("""
                public class Main {
                    public static void main(String[] args) {
                        String nameé = "World";
                        System.out.println(nameé);
                    }
                }
                """, result.java());
        assertEquals(List.of(), result.warnings());
    }
}
