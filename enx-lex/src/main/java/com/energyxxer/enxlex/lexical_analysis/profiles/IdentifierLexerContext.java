package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;
import java.util.Collections;

public class IdentifierLexerContext implements LexerContext {
    private final TokenType type;
    private final String regex;

    public IdentifierLexerContext(TokenType type, String regex) {
        this.type = type;
        this.regex = regex;
    }

    @Override
    public ScannerContextResponse analyze(String str, LexerProfile profile) {
        return new ScannerContextResponse(false);
    }

    @Override
    public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
        int i = 0;
        while(i < str.length() && Character.toString(str.charAt(i)).matches(regex)) {
            i++;
        }
        if(i > 0) return new ScannerContextResponse(true, str.substring(0, i), type);
        return new ScannerContextResponse(false);
    }

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(type);
    }
}