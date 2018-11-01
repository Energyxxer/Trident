package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;

import java.util.List;

/**
 * Defines a profile for the lexical analysis of files.
 */
public abstract class ScannerProfile {
    /**
     * Contains a list of all custom analysis contexts.
     * */
    public List<ScannerContext> contexts;
    /**
     * Contains the token stream to which the tokens are being sent.
     * */
    protected TokenStream stream = null;

    /**
     * Compares two adjacent characters and determines whether these
     * characters can be placed in the same token.
     *
     * @param ch0 First character to be compared.
     * @param ch1 Second character to be compared.
     *
     * @return true if characters can be merged, false otherwise.
     * */
    public boolean canMerge(char ch0, char ch1) {
        return false;
    }

    /**
     * Method through which all tokens pass.
     *
     * @param token Token to be filtered.
     *
     * @return false if the token should be added to the stream, true otherwise.
     * */
    public boolean filter(Token token) {
        return false;
    }

    /**
     * Sets the current stream to the given value.
     *
     * @param stream The stream to assign to this profile.
     * */
    public void setStream(TokenStream stream) {
        this.stream = stream;
    }

    /**
     * Puts information about the language in the given header token's
     * attribute map.
     *
     * @param header Header token to put attributes onto.
     *
     * */
    public abstract void putHeaderInfo(Token header);
}
