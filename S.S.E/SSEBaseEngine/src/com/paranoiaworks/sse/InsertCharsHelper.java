package com.paranoiaworks.sse;

import javax.swing.text.BadLocationException;
import javax.swing.text.GapContent;

public class InsertCharsHelper extends GapContent {

    public InsertCharsHelper() {
        super();
    }

    public void insertChars(int where, char[] chars) throws BadLocationException {
        if (where > length() || where < 0) {
            throw new BadLocationException("Invalid insert", length());
        }
        replace(where, 0, chars, chars.length);
    }
}
