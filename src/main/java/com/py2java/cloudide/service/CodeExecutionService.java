package com.py2java.cloudide.service;

import com.py2java.cloudide.model.ExecutionResult;
import com.py2java.cloudide.model.TerminalResult;

public interface CodeExecutionService {
    boolean dockerAvailable();

    ExecutionResult run(String language, String code);

    TerminalResult terminal(String command);
}
