package org.elm.lang.core.parser.manual;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.psi.tree.IElementType;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import static org.elm.lang.core.psi.ElmTypes.DOT;

public class PathParser implements GeneratedParserUtilBase.Parser {

    private final IElementType sectionType;
    private IElementType memberType;
    private IElementType alternativeMemberType;
    private Parser memberParser;
    private Parser alternativeMemberParser;


    public PathParser(IElementType sectionType, IElementType memberType, IElementType alternativeMemberType, Parser memberParser, Parser alternativeMemberParser) {
        this.sectionType = sectionType;
        this.memberType = memberType;
        this.alternativeMemberType = alternativeMemberType;
        this.memberParser = memberParser;
        this.alternativeMemberParser = alternativeMemberParser;
    }

    @Override
    public boolean parse(PsiBuilder builder, int level) {
        if (!recursion_guard_(builder, level, this.getGuardText())) return false;
        if (!nextTokenIs(builder, this.memberType)) return false;
        boolean result;
        boolean isContinued = builder.rawLookup(1) == DOT;
        PsiBuilder.Marker marker = enter_section_(builder);
        result = this.memberParser.parse(builder, level + 1); //consumeToken(builder, this.memberType);
        result = result && (!isContinued || listOfMembers(builder, level + 1));
        exit_section_(builder, marker, this.sectionType, result);
        return result;
    }

    private boolean listOfMembers(PsiBuilder builder, int level) {
        if (!recursion_guard_(builder, level, this.getGuardText(1))) return false;
        int c = current_position_(builder);
        boolean isContinued;
        while (true) {
            if (builder.rawLookup(0) != DOT) {
                break;
            }
            if (builder.rawLookup(1) != this.memberType
                    && !trySwitchingMemberType(builder)) {
                break;
            }
            isContinued = builder.rawLookup(2) == DOT;
            if (!dotMember(builder, level + 1)) break;
            if (!empty_element_parsed_guard_(builder, this.getGuardText(1), c)) break;
            c = current_position_(builder);
            if (!isContinued) break;
        }
        return true;
    }

    private boolean trySwitchingMemberType(PsiBuilder builder) {
        if (this.alternativeMemberParser != null
                && builder.rawLookup(1) == this.alternativeMemberType) {
            this.memberType = this.alternativeMemberType;
            this.alternativeMemberType = null;
            this.memberParser = this.alternativeMemberParser;
            this.alternativeMemberParser = null;
            return true;
        }
        return false;
    }

    private boolean dotMember(PsiBuilder builder, int level) {
        if (!recursion_guard_(builder, level, this.getGuardText(1, 0))) return false;
        boolean result;
        PsiBuilder.Marker m = enter_section_(builder);
        result = consumeToken(builder, DOT);
        result = result && this.memberParser.parse(builder, level + 1);
        exit_section_(builder, m, null, result);
        return result;
    }

    private String getGuardText(Integer ... numbers) {
        StringBuilder result = new StringBuilder(this.sectionType.toString().toLowerCase());
        for (int number: numbers) {
            result.append('_');
            result.append(number);
        }
        return result.toString();
    }
}
