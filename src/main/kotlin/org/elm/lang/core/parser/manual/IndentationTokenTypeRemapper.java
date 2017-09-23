package org.elm.lang.core.parser.manual;

import com.intellij.lang.ITokenTypeRemapper;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import java.util.*;

import org.elm.lang.core.psi.ElmTypes;

public class IndentationTokenTypeRemapper implements ITokenTypeRemapper {
    private static final Map<Long, IndentationTokenTypeRemapper> instances =
            Collections.synchronizedMap(new HashMap<Long, IndentationTokenTypeRemapper>());

    private Stack<Integer> indentations;

    private IndentationTokenTypeRemapper() {
        this.reset();
    }

    public static <T> T use(Callback<T> callback, T input) {
        IndentationTokenTypeRemapper instance = getInstance();
        Stack<Integer> indentationsBackup = instance.indentations;
        instance.indentations = (Stack<Integer>)instance.indentations.clone();
        input = callback.call(instance, input);
        instance.indentations = indentationsBackup;
        return input;
    }

    public void pushIndentation(int indentation) {
        if (indentation > 0) {
            this.indentations.push(indentation);
        }
    }

    @Override
    public IElementType filter(IElementType type, int start, int end, CharSequence text) {
        if (!this.indentations.empty() && TokenType.WHITE_SPACE.equals(type)) {
            int i = IndentationHelper.getIndentation(text, start, end);
            if (i > 0) {
                if (this.indentations.search(i) > 0) {
                    return ElmTypes.SEPARATION_BY_INDENTATION;
                }
            }
        } else if (ElmTypes.FRESH_LINE.equals(type) || end == text.length()) {
            this.reset();
        }
        return type;
    }

    public void reset() {
        if (this.indentations == null || !this.indentations.empty()) {
            this.indentations = new Stack<Integer>();
        }
    }

    private static IndentationTokenTypeRemapper getInstance() {
        long key = Thread.currentThread().getId();
        if (!instances.containsKey(key)) {
            instances.put(key, new IndentationTokenTypeRemapper());
        }
        return instances.get(key);
    }

    // T is a hack for lack of non-final closures in Java
    public interface Callback<T> {
        T call(IndentationTokenTypeRemapper instance, T input);
    }
}
