package org.elm.lang.core.parser.manual;


import com.intellij.lang.PsiBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndentationHelper {
    private static final Pattern indentationPattern = Pattern.compile(".*[\r\n]([^\r\n]+)$", Pattern.DOTALL);

    public static int getIndentationOfPreviousToken(PsiBuilder builder) {
        // getTokenType has some side effects. Do not remove the call.
        builder.getTokenType();
        int end = builder.rawTokenTypeStart(0);
        return getLastIndentation(builder.getOriginalText().subSequence(0, end));
    }

    private static int getLastIndentation(CharSequence text) {
        Matcher m = indentationPattern.matcher(text);
        if (m.matches()) {
            return m.group(1).length();
        }
        return 0;
    }

    public static int getIndentation(CharSequence text, int start, int end) {
        CharSequence previousChars = text.subSequence(start, end);
        Matcher m = indentationPattern.matcher(previousChars);
        if (m.matches()) {
            return m.group(1).length();
        }
        return 0;
    }
}
