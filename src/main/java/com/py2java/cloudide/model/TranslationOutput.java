package com.py2java.cloudide.model;

import java.util.List;

public record TranslationOutput(String java, List<String> warnings, String explanation) {
}
