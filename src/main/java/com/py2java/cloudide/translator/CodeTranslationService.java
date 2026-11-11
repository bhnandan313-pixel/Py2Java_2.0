package com.py2java.cloudide.translator;

import com.py2java.cloudide.model.TranslationOutput;

public interface CodeTranslationService {
    TranslationOutput translate(String code);
}
