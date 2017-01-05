package org.elm.lang.core.parser.manual;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import static org.elm.lang.core.psi.ElmTypes.CASE;
import static org.elm.lang.core.psi.ElmTypes.CASE_OF;

public class CaseOfParser implements GeneratedParserUtilBase.Parser {
    private final GeneratedParserUtilBase.Parser header;
    private final GeneratedParserUtilBase.Parser branch;
    private final GeneratedParserUtilBase.Parser oneOrMoreSeparations;
    private int indentation;

    public CaseOfParser(
            GeneratedParserUtilBase.Parser header,
            GeneratedParserUtilBase.Parser branch,
            GeneratedParserUtilBase.Parser oneOrMoreSeparations
    ) {
        this.header = header;
        this.branch = branch;
        this.oneOrMoreSeparations = oneOrMoreSeparations;
    }

    @Override
    public boolean parse(final PsiBuilder builder, final int level) {
        if (!recursion_guard_(builder, level, "CaseOfParser")) {
            return false;
        }
        if (!nextTokenIs(builder, CASE)) {
            return false;
        }
        boolean result;
        PsiBuilder.Marker marker = enter_section_(builder);
        result = this.header.parse(builder, level + 1);
        result = result && IndentationTokenTypeRemapper.use(new IndentationTokenTypeRemapper.Callback<Boolean>() {
            @Override
            public Boolean call(IndentationTokenTypeRemapper reMapper, Boolean result) {
                CaseOfParser.this.indentation = IndentationHelper.getIndentationOfPreviousToken(builder);
                reMapper.pushIndentation(CaseOfParser.this.indentation);
                builder.setTokenTypeRemapper(reMapper);
                result = result && CaseOfParser.this.branch.parse(builder, level + 1);
                result = result && CaseOfParser.this.separatedBranches(builder, level + 1);
                return result;
            }
        }, result);
        exit_section_(builder, marker, CASE_OF, result);
        return result;
    }

    // (case_of_one_or_more_separation case_of_branch)*
    private boolean separatedBranches(PsiBuilder builder, int level) {
        if (!recursion_guard_(builder, level, "separatedBranches")) {
            return false;
        }
        int currentPosition = current_position_(builder);
        while (true) {
            if (!this.separatedBranch(builder, level + 1)) break;
            if (!empty_element_parsed_guard_(builder, "separatedBranches", currentPosition)) break;
            currentPosition = current_position_(builder);
        }
        return true;
    }

    // case_of_one_or_more_separation case_of_branch
    private boolean separatedBranch(PsiBuilder builder, int level) {
        if (!recursion_guard_(builder, level, "separatedBranch")) {
            return false;
        }
        boolean result;
        PsiBuilder.Marker marker = enter_section_(builder);
        result = this.oneOrMoreSeparations.parse(builder, level + 1);
        if (result) {
            result = this.indentation == IndentationHelper.getIndentationOfPreviousToken(builder);
        }
        result = result && this.branch.parse(builder, level + 1);
        exit_section_(builder, marker, null, result);
        return result;
    }
}