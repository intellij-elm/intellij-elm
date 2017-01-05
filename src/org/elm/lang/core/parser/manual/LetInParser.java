package org.elm.lang.core.parser.manual;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_;
import static org.elm.lang.core.psi.ElmTypes.IN;
import static org.elm.lang.core.psi.ElmTypes.LET;
import static org.elm.lang.core.psi.ElmTypes.LET_IN;

public class LetInParser implements GeneratedParserUtilBase.Parser {
    private final GeneratedParserUtilBase.Parser innerValueDeclaration;
    private final GeneratedParserUtilBase.Parser otherValueDeclarations;
    private final GeneratedParserUtilBase.Parser expression;

    public LetInParser(
            Parser innerValueDeclaration, Parser otherValueDeclarations, Parser expression) {
        this.innerValueDeclaration = innerValueDeclaration;
        this.otherValueDeclarations = otherValueDeclarations;
        this.expression = expression;
    }

    @Override
    public boolean parse(final PsiBuilder builder, final int level) {
        if (!recursion_guard_(builder, level, "let_in")) return false;
        if (!nextTokenIs(builder, LET)) return false;
        boolean result;
        PsiBuilder.Marker marker = enter_section_(builder);
        result = consumeToken(builder, LET);
        result = result && IndentationTokenTypeRemapper.use(new IndentationTokenTypeRemapper.Callback<Boolean>() {
            @Override
            public Boolean call(IndentationTokenTypeRemapper reMapper, Boolean result) {
                builder.setTokenTypeRemapper(reMapper);
                if (result) {
                    int indentationValue = IndentationHelper.getIndentationOfPreviousToken(builder);
                    reMapper.pushIndentation(indentationValue);
                }
                result = result && LetInParser.this.innerValueDeclaration.parse(builder, level + 1);
                result = result && LetInParser.this.otherValueDeclarations.parse(builder, level + 1);
                result = result && consumeToken(builder, IN);
                return result;
            }
        }, result);
        result = result && this.expression.parse(builder, level + 1);
        exit_section_(builder, marker, LET_IN, result);
        return result;
    }
}
