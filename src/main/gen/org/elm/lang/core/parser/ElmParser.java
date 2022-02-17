// This is a generated file. Not intended for manual editing.
package org.elm.lang.core.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.elm.lang.core.psi.ElmTypes.*;
import static org.elm.lang.core.parser.manual.ElmManualParseRules.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static com.intellij.lang.WhitespacesBinders.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class ElmParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return ElmFile(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(ANONYMOUS_FUNCTION_EXPR, BIN_OP_EXPR, CASE_OF_EXPR, CHAR_CONSTANT_EXPR,
      EXPRESSION, FIELD_ACCESSOR_FUNCTION_EXPR, FIELD_ACCESS_EXPR, FUNCTION_CALL_EXPR,
      GLSL_CODE_EXPR, IF_ELSE_EXPR, LET_IN_EXPR, LIST_EXPR,
      NEGATE_EXPR, NUMBER_CONSTANT_EXPR, OPERATOR_AS_FUNCTION_EXPR, PARENTHESIZED_EXPR,
      RECORD_EXPR, STRING_CONSTANT_EXPR, TUPLE_EXPR, UNIT_EXPR,
      VALUE_EXPR),
  };

  /* ********************************************************** */
  // BACKSLASH Pattern+ ARROW Expression
  public static boolean AnonymousFunctionExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AnonymousFunctionExpr")) return false;
    if (!nextTokenIs(b, BACKSLASH)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BACKSLASH);
    r = r && AnonymousFunctionExpr_1(b, l + 1);
    r = r && consumeToken(b, ARROW);
    r = r && Expression(b, l + 1);
    exit_section_(b, m, ANONYMOUS_FUNCTION_EXPR, r);
    return r;
  }

  // Pattern+
  private static boolean AnonymousFunctionExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AnonymousFunctionExpr_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Pattern(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!Pattern(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "AnonymousFunctionExpr_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // UNDERSCORE
  public static boolean AnythingPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AnythingPattern")) return false;
    if (!nextTokenIs(b, UNDERSCORE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, UNDERSCORE);
    exit_section_(b, m, ANYTHING_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // AS UPPER_CASE_IDENTIFIER
  public static boolean AsClause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AsClause")) return false;
    if (!nextTokenIs(b, AS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, AS, UPPER_CASE_IDENTIFIER);
    exit_section_(b, m, AS_CLAUSE, r);
    return r;
  }

  /* ********************************************************** */
  // LiteralExprGroup
  //     | NegateExpr
  //     | OperatorAsFunctionExpr
  //     | UnitExpr
  //     | FieldAccessExpr
  //     | TupleOrParenExpr
  //     | ValueExpr
  //     | FieldAccessorFunctionExpr
  //     | ListExpr
  //     | RecordExpr
  //     | IfElseExpr
  //     | CaseOfExpr
  //     | LetInExpr
  //     | AnonymousFunctionExpr
  //     | GlslCodeExpr
  static boolean Atom(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Atom")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expr>");
    r = LiteralExprGroup(b, l + 1);
    if (!r) r = NegateExpr(b, l + 1);
    if (!r) r = OperatorAsFunctionExpr(b, l + 1);
    if (!r) r = UnitExpr(b, l + 1);
    if (!r) r = FieldAccessExpr(b, l + 1);
    if (!r) r = parseTupleOrParenExpr(b, l + 1, ElmParser::Expression);
    if (!r) r = ValueExpr(b, l + 1);
    if (!r) r = FieldAccessorFunctionExpr(b, l + 1);
    if (!r) r = ListExpr(b, l + 1);
    if (!r) r = RecordExpr(b, l + 1);
    if (!r) r = IfElseExpr(b, l + 1);
    if (!r) r = CaseOfExpr(b, l + 1);
    if (!r) r = LetInExpr(b, l + 1);
    if (!r) r = AnonymousFunctionExpr(b, l + 1);
    if (!r) r = GlslCodeExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Operator CallOrAtom (Operator CallOrAtom)*
  public static boolean BinOpExprUpper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinOpExprUpper")) return false;
    if (!nextTokenIs(b, OPERATOR_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _UPPER_, BIN_OP_EXPR, null);
    r = Operator(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, CallOrAtom(b, l + 1));
    r = p && BinOpExprUpper_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (Operator CallOrAtom)*
  private static boolean BinOpExprUpper_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinOpExprUpper_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!BinOpExprUpper_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "BinOpExprUpper_2", c)) break;
    }
    return true;
  }

  // Operator CallOrAtom
  private static boolean BinOpExprUpper_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinOpExprUpper_2_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = Operator(b, l + 1);
    p = r; // pin = 1
    r = r && CallOrAtom(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // Atom FunctionCallExpr?
  static boolean CallOrAtom(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallOrAtom")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Atom(b, l + 1);
    r = r && CallOrAtom_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // FunctionCallExpr?
  private static boolean CallOrAtom_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallOrAtom_1")) return false;
    FunctionCallExpr(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // Pattern ARROW Expression
  public static boolean CaseOfBranch(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfBranch")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CASE_OF_BRANCH, "<case of branch>");
    r = Pattern(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, ARROW));
    r = p && Expression(b, l + 1) && r;
    exit_section_(b, l, m, r, p, ElmParser::case_branch_recover);
    return r || p;
  }

  /* ********************************************************** */
  // CASE Expression CaseOfTail
  public static boolean CaseOfExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfExpr")) return false;
    if (!nextTokenIs(b, CASE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CASE_OF_EXPR, null);
    r = consumeToken(b, CASE);
    p = r; // pin = CASE
    r = r && report_error_(b, Expression(b, l + 1));
    r = p && CaseOfTail(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // OF CaseOfTail2?
  static boolean CaseOfTail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfTail")) return false;
    if (!nextTokenIs(b, OF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, OF);
    p = r; // pin = 1
    r = r && CaseOfTail_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // CaseOfTail2?
  private static boolean CaseOfTail_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfTail_1")) return false;
    CaseOfTail2(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // VIRTUAL_OPEN_SECTION CaseOfBranch MoreCaseOfBranches? (VIRTUAL_END_SECTION|<<eof>>)
  static boolean CaseOfTail2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfTail2")) return false;
    if (!nextTokenIs(b, VIRTUAL_OPEN_SECTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, VIRTUAL_OPEN_SECTION);
    p = r; // pin = 1
    r = r && report_error_(b, CaseOfBranch(b, l + 1));
    r = p && report_error_(b, CaseOfTail2_2(b, l + 1)) && r;
    r = p && CaseOfTail2_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // MoreCaseOfBranches?
  private static boolean CaseOfTail2_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfTail2_2")) return false;
    MoreCaseOfBranches(b, l + 1);
    return true;
  }

  // VIRTUAL_END_SECTION|<<eof>>
  private static boolean CaseOfTail2_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CaseOfTail2_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VIRTUAL_END_SECTION);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // OPEN_CHAR StringPart CLOSE_CHAR
  public static boolean CharConstantExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CharConstantExpr")) return false;
    if (!nextTokenIs(b, OPEN_CHAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CHAR_CONSTANT_EXPR, null);
    r = consumeToken(b, OPEN_CHAR);
    p = r; // pin = 1
    r = r && report_error_(b, StringPart(b, l + 1));
    r = p && consumeToken(b, CLOSE_CHAR) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // SinglePattern ('::' SinglePattern)+
  public static boolean ConsPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConsPattern")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONS_PATTERN, "<cons pattern>");
    r = SinglePattern(b, l + 1);
    r = r && ConsPattern_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ('::' SinglePattern)+
  private static boolean ConsPattern_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConsPattern_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ConsPattern_1_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!ConsPattern_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ConsPattern_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // '::' SinglePattern
  private static boolean ConsPattern_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConsPattern_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "::");
    r = r && SinglePattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ValueDeclaration
  //     | TypeAliasDeclaration
  //     | TypeDeclaration
  //     | TypeAnnotation
  //     | PortAnnotation
  //     | InfixDeclaration
  static boolean Declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Declaration")) return false;
    boolean r;
    r = ValueDeclaration(b, l + 1);
    if (!r) r = TypeAliasDeclaration(b, l + 1);
    if (!r) r = TypeDeclaration(b, l + 1);
    if (!r) r = TypeAnnotation(b, l + 1);
    if (!r) r = PortAnnotation(b, l + 1);
    if (!r) r = InfixDeclaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // !<<eof>> Module
  static boolean ElmFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ElmFile")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = ElmFile_0(b, l + 1);
    p = r; // pin = 1
    r = r && Module(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !<<eof>>
  private static boolean ElmFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ElmFile_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ExposedValue
  //     | ExposedType
  //     | ExposedOperator
  static boolean ExposedItem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposedItem")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = ExposedValue(b, l + 1);
    if (!r) r = ExposedType(b, l + 1);
    if (!r) r = ExposedOperator(b, l + 1);
    exit_section_(b, l, m, r, false, ElmParser::exposed_item_recover);
    return r;
  }

  /* ********************************************************** */
  // OperatorAsFunction_inner
  public static boolean ExposedOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposedOperator")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OperatorAsFunction_inner(b, l + 1);
    exit_section_(b, m, EXPOSED_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // UPPER_CASE_IDENTIFIER (LEFT_PARENTHESIS DOUBLE_DOT RIGHT_PARENTHESIS)?
  public static boolean ExposedType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposedType")) return false;
    if (!nextTokenIs(b, UPPER_CASE_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, EXPOSED_TYPE, null);
    r = consumeToken(b, UPPER_CASE_IDENTIFIER);
    p = r; // pin = 1
    r = r && ExposedType_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (LEFT_PARENTHESIS DOUBLE_DOT RIGHT_PARENTHESIS)?
  private static boolean ExposedType_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposedType_1")) return false;
    ExposedType_1_0(b, l + 1);
    return true;
  }

  // LEFT_PARENTHESIS DOUBLE_DOT RIGHT_PARENTHESIS
  private static boolean ExposedType_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposedType_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFT_PARENTHESIS, DOUBLE_DOT, RIGHT_PARENTHESIS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER
  public static boolean ExposedValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposedValue")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, m, EXPOSED_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // EXPOSING LEFT_PARENTHESIS (DOUBLE_DOT | ExposedItem MoreExposedItems?) RIGHT_PARENTHESIS
  public static boolean ExposingList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposingList")) return false;
    if (!nextTokenIs(b, EXPOSING)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, EXPOSING_LIST, null);
    r = consumeTokens(b, 1, EXPOSING, LEFT_PARENTHESIS);
    p = r; // pin = 1
    r = r && report_error_(b, ExposingList_2(b, l + 1));
    r = p && consumeToken(b, RIGHT_PARENTHESIS) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // DOUBLE_DOT | ExposedItem MoreExposedItems?
  private static boolean ExposingList_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposingList_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOUBLE_DOT);
    if (!r) r = ExposingList_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ExposedItem MoreExposedItems?
  private static boolean ExposingList_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposingList_2_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ExposedItem(b, l + 1);
    r = r && ExposingList_2_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // MoreExposedItems?
  private static boolean ExposingList_2_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExposingList_2_1_1")) return false;
    MoreExposedItems(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // CallOrAtom BinOpExprUpper?
  public static boolean Expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, EXPRESSION, "<expression>");
    r = CallOrAtom(b, l + 1);
    r = r && Expression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // BinOpExprUpper?
  private static boolean Expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expression_1")) return false;
    BinOpExprUpper(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER EQ Expression
  public static boolean Field(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Field")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FIELD, null);
    r = consumeTokens(b, 2, LOWER_CASE_IDENTIFIER, EQ);
    p = r; // pin = 2
    r = r && Expression(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // FieldAccessStart FieldAccessSegment+
  public static boolean FieldAccessExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldAccessExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, FIELD_ACCESS_EXPR, "<field access expr>");
    r = FieldAccessStart(b, l + 1);
    r = r && FieldAccessExpr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // FieldAccessSegment+
  private static boolean FieldAccessExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldAccessExpr_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = FieldAccessSegment(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!FieldAccessSegment(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FieldAccessExpr_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // DotWithoutWhitespace LOWER_CASE_IDENTIFIER
  public static boolean FieldAccessSegment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldAccessSegment")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, FIELD_ACCESS_EXPR, "<field access segment>");
    r = parseDotWithoutWhitespace(b, l + 1);
    r = r && consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ValueExpr | ParenthesizedExpr | RecordExpr
  static boolean FieldAccessStart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldAccessStart")) return false;
    boolean r;
    r = ValueExpr(b, l + 1);
    if (!r) r = ParenthesizedExpr(b, l + 1);
    if (!r) r = RecordExpr(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // DotWithoutTrailingWhitespace LOWER_CASE_IDENTIFIER
  public static boolean FieldAccessorFunctionExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldAccessorFunctionExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FIELD_ACCESSOR_FUNCTION_EXPR, "<field accessor function expr>");
    r = parseDotWithoutTrailingWhitespace(b, l + 1);
    r = r && consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER COLON TypeExpression
  public static boolean FieldType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldType")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LOWER_CASE_IDENTIFIER, COLON);
    r = r && TypeExpression(b, l + 1);
    exit_section_(b, m, FIELD_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // Atom+
  public static boolean FunctionCallExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionCallExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, FUNCTION_CALL_EXPR, "<function call expr>");
    r = Atom(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!Atom(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FunctionCallExpr", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER FunctionDeclarationPattern*
  public static boolean FunctionDeclarationLeft(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionDeclarationLeft")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    r = r && FunctionDeclarationLeft_1(b, l + 1);
    exit_section_(b, m, FUNCTION_DECLARATION_LEFT, r);
    return r;
  }

  // FunctionDeclarationPattern*
  private static boolean FunctionDeclarationLeft_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionDeclarationLeft_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!FunctionDeclarationPattern(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FunctionDeclarationLeft_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // AnythingPattern
  //         | LowerPattern
  //         | TuplePattern
  //         | UnitExpr
  //         | ListPattern // Always a partial pattern, but not a syntax error
  //         | RecordPattern
  //         | LiteralExprGroup // Always a partial pattern, but not a syntax error
  //         | ParenthesizedPattern
  static boolean FunctionDeclarationPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionDeclarationPattern")) return false;
    boolean r;
    r = AnythingPattern(b, l + 1);
    if (!r) r = LowerPattern(b, l + 1);
    if (!r) r = TuplePattern(b, l + 1);
    if (!r) r = UnitExpr(b, l + 1);
    if (!r) r = ListPattern(b, l + 1);
    if (!r) r = RecordPattern(b, l + 1);
    if (!r) r = LiteralExprGroup(b, l + 1);
    if (!r) r = ParenthesizedPattern(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // START_GLSL_CODE GLSL_CODE_CONTENT* END_GLSL_CODE
  public static boolean GlslCodeExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "GlslCodeExpr")) return false;
    if (!nextTokenIs(b, START_GLSL_CODE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, START_GLSL_CODE);
    r = r && GlslCodeExpr_1(b, l + 1);
    r = r && consumeToken(b, END_GLSL_CODE);
    exit_section_(b, m, GLSL_CODE_EXPR, r);
    return r;
  }

  // GLSL_CODE_CONTENT*
  private static boolean GlslCodeExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "GlslCodeExpr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, GLSL_CODE_CONTENT)) break;
      if (!empty_element_parsed_guard_(b, "GlslCodeExpr_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // IF Expression THEN Expression (ELSE IF Expression THEN Expression)* ELSE Expression
  public static boolean IfElseExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfElseExpr")) return false;
    if (!nextTokenIs(b, IF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IF_ELSE_EXPR, null);
    r = consumeToken(b, IF);
    p = r; // pin = IF
    r = r && report_error_(b, Expression(b, l + 1));
    r = p && report_error_(b, consumeToken(b, THEN)) && r;
    r = p && report_error_(b, Expression(b, l + 1)) && r;
    r = p && report_error_(b, IfElseExpr_4(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, ELSE)) && r;
    r = p && Expression(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (ELSE IF Expression THEN Expression)*
  private static boolean IfElseExpr_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfElseExpr_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!IfElseExpr_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "IfElseExpr_4", c)) break;
    }
    return true;
  }

  // ELSE IF Expression THEN Expression
  private static boolean IfElseExpr_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfElseExpr_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ELSE, IF);
    r = r && Expression(b, l + 1);
    r = r && consumeToken(b, THEN);
    r = r && Expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ImportClause
  static boolean Import(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Import")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = ImportClause(b, l + 1);
    exit_section_(b, l, m, r, false, ElmParser::generic_recover);
    return r;
  }

  /* ********************************************************** */
  // IMPORT UpperCaseQID [AsClause] [ExposingList]
  public static boolean ImportClause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ImportClause")) return false;
    if (!nextTokenIs(b, IMPORT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_CLAUSE, null);
    r = consumeToken(b, IMPORT);
    r = r && UpperCaseQID(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, ImportClause_2(b, l + 1));
    r = p && ImportClause_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // [AsClause]
  private static boolean ImportClause_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ImportClause_2")) return false;
    AsClause(b, l + 1);
    return true;
  }

  // [ExposingList]
  private static boolean ImportClause_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ImportClause_3")) return false;
    ExposingList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // Import (VIRTUAL_END_DECL Import)*
  static boolean ImportList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ImportList")) return false;
    if (!nextTokenIs(b, IMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Import(b, l + 1);
    r = r && ImportList_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (VIRTUAL_END_DECL Import)*
  private static boolean ImportList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ImportList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ImportList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ImportList_1", c)) break;
    }
    return true;
  }

  // VIRTUAL_END_DECL Import
  private static boolean ImportList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ImportList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VIRTUAL_END_DECL);
    r = r && Import(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // INFIX ('left' | 'right' | 'non') NUMBER_LITERAL OperatorAsFunction_inner EQ InfixFuncRef
  public static boolean InfixDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InfixDeclaration")) return false;
    if (!nextTokenIs(b, INFIX)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, INFIX_DECLARATION, null);
    r = consumeToken(b, INFIX);
    r = r && InfixDeclaration_1(b, l + 1);
    r = r && consumeToken(b, NUMBER_LITERAL);
    r = r && OperatorAsFunction_inner(b, l + 1);
    r = r && consumeToken(b, EQ);
    p = r; // pin = EQ
    r = r && InfixFuncRef(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // 'left' | 'right' | 'non'
  private static boolean InfixDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InfixDeclaration_1")) return false;
    boolean r;
    r = consumeToken(b, "left");
    if (!r) r = consumeToken(b, "right");
    if (!r) r = consumeToken(b, "non");
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER
  public static boolean InfixFuncRef(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InfixFuncRef")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, m, INFIX_FUNC_REF, r);
    return r;
  }

  /* ********************************************************** */
  // ValueDeclaration
  //     | TypeAnnotation
  static boolean InnerDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InnerDeclaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = ValueDeclaration(b, l + 1);
    if (!r) r = TypeAnnotation(b, l + 1);
    exit_section_(b, l, m, r, false, ElmParser::inner_decl_recover);
    return r;
  }

  /* ********************************************************** */
  // FunctionDeclarationLeft
  //     | Pattern
  static boolean InternalValueDeclarationLeft(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InternalValueDeclarationLeft")) return false;
    boolean r;
    r = FunctionDeclarationLeft(b, l + 1);
    if (!r) r = Pattern(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LET LetInTail
  public static boolean LetInExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LetInExpr")) return false;
    if (!nextTokenIs(b, LET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, LET_IN_EXPR, null);
    r = consumeToken(b, LET);
    p = r; // pin = LET
    r = r && LetInTail(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // VIRTUAL_OPEN_SECTION InnerDeclaration MoreInnerDeclarations? VIRTUAL_END_SECTION IN Expression
  static boolean LetInTail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LetInTail")) return false;
    if (!nextTokenIs(b, VIRTUAL_OPEN_SECTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, VIRTUAL_OPEN_SECTION);
    p = r; // pin = 1
    r = r && report_error_(b, InnerDeclaration(b, l + 1));
    r = p && report_error_(b, LetInTail_2(b, l + 1)) && r;
    r = p && report_error_(b, consumeTokens(b, -1, VIRTUAL_END_SECTION, IN)) && r;
    r = p && Expression(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // MoreInnerDeclarations?
  private static boolean LetInTail_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LetInTail_2")) return false;
    MoreInnerDeclarations(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // LEFT_SQUARE_BRACKET [<<non_empty_list2 Expression>>] RIGHT_SQUARE_BRACKET
  public static boolean ListExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr")) return false;
    if (!nextTokenIs(b, LEFT_SQUARE_BRACKET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, LIST_EXPR, null);
    r = consumeToken(b, LEFT_SQUARE_BRACKET);
    p = r; // pin = 1
    r = r && report_error_(b, ListExpr_1(b, l + 1));
    r = p && consumeToken(b, RIGHT_SQUARE_BRACKET) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // [<<non_empty_list2 Expression>>]
  private static boolean ListExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1")) return false;
    non_empty_list2(b, l + 1, ElmParser::Expression);
    return true;
  }

  /* ********************************************************** */
  // LEFT_SQUARE_BRACKET [<<non_empty_list Pattern>>] RIGHT_SQUARE_BRACKET
  public static boolean ListPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListPattern")) return false;
    if (!nextTokenIs(b, LEFT_SQUARE_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_SQUARE_BRACKET);
    r = r && ListPattern_1(b, l + 1);
    r = r && consumeToken(b, RIGHT_SQUARE_BRACKET);
    exit_section_(b, m, LIST_PATTERN, r);
    return r;
  }

  // [<<non_empty_list Pattern>>]
  private static boolean ListPattern_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListPattern_1")) return false;
    non_empty_list(b, l + 1, ElmParser::Pattern);
    return true;
  }

  /* ********************************************************** */
  // CharConstantExpr
  //     | NumberConstantExpr
  //     | StringConstantExpr
  static boolean LiteralExprGroup(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LiteralExprGroup")) return false;
    boolean r;
    r = CharConstantExpr(b, l + 1);
    if (!r) r = NumberConstantExpr(b, l + 1);
    if (!r) r = StringConstantExpr(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER
  public static boolean LowerPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LowerPattern")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, m, LOWER_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER
  public static boolean LowerTypeName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LowerTypeName")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, m, LOWER_TYPE_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // ModuleDeclaration? VIRTUAL_END_DECL? ImportList? VIRTUAL_END_DECL? TopDeclList?
  static boolean Module(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Module")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Module_0(b, l + 1);
    r = r && Module_1(b, l + 1);
    r = r && Module_2(b, l + 1);
    r = r && Module_3(b, l + 1);
    r = r && Module_4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ModuleDeclaration?
  private static boolean Module_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Module_0")) return false;
    ModuleDeclaration(b, l + 1);
    return true;
  }

  // VIRTUAL_END_DECL?
  private static boolean Module_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Module_1")) return false;
    consumeToken(b, VIRTUAL_END_DECL);
    return true;
  }

  // ImportList?
  private static boolean Module_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Module_2")) return false;
    ImportList(b, l + 1);
    return true;
  }

  // VIRTUAL_END_DECL?
  private static boolean Module_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Module_3")) return false;
    consumeToken(b, VIRTUAL_END_DECL);
    return true;
  }

  // TopDeclList?
  private static boolean Module_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Module_4")) return false;
    TopDeclList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // PORT? MODULE UpperCaseQID ExposingList
  //     | 'effect' MODULE UpperCaseQID WHERE RecordExpr ExposingList
  public static boolean ModuleDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ModuleDeclaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MODULE_DECLARATION, "<module declaration>");
    r = ModuleDeclaration_0(b, l + 1);
    if (!r) r = ModuleDeclaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, ElmParser::module_recover);
    return r;
  }

  // PORT? MODULE UpperCaseQID ExposingList
  private static boolean ModuleDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ModuleDeclaration_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = ModuleDeclaration_0_0(b, l + 1);
    r = r && consumeToken(b, MODULE);
    r = r && UpperCaseQID(b, l + 1);
    p = r; // pin = UpperCaseQID
    r = r && ExposingList(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // PORT?
  private static boolean ModuleDeclaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ModuleDeclaration_0_0")) return false;
    consumeToken(b, PORT);
    return true;
  }

  // 'effect' MODULE UpperCaseQID WHERE RecordExpr ExposingList
  private static boolean ModuleDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ModuleDeclaration_1")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, "effect");
    r = r && consumeToken(b, MODULE);
    r = r && UpperCaseQID(b, l + 1);
    p = r; // pin = UpperCaseQID
    r = r && report_error_(b, consumeToken(b, WHERE));
    r = p && report_error_(b, RecordExpr(b, l + 1)) && r;
    r = p && ExposingList(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // (VIRTUAL_END_DECL CaseOfBranch)+
  static boolean MoreCaseOfBranches(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreCaseOfBranches")) return false;
    if (!nextTokenIs(b, VIRTUAL_END_DECL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = MoreCaseOfBranches_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!MoreCaseOfBranches_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "MoreCaseOfBranches", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // VIRTUAL_END_DECL CaseOfBranch
  private static boolean MoreCaseOfBranches_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreCaseOfBranches_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, VIRTUAL_END_DECL);
    p = r; // pin = 1
    r = r && CaseOfBranch(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // (COMMA ExposedItem)+
  static boolean MoreExposedItems(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreExposedItems")) return false;
    if (!nextTokenIs(b, COMMA)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = MoreExposedItems_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!MoreExposedItems_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "MoreExposedItems", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA ExposedItem
  private static boolean MoreExposedItems_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreExposedItems_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && ExposedItem(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // (VIRTUAL_END_DECL InnerDeclaration)+
  static boolean MoreInnerDeclarations(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreInnerDeclarations")) return false;
    if (!nextTokenIs(b, VIRTUAL_END_DECL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = MoreInnerDeclarations_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!MoreInnerDeclarations_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "MoreInnerDeclarations", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // VIRTUAL_END_DECL InnerDeclaration
  private static boolean MoreInnerDeclarations_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreInnerDeclarations_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, VIRTUAL_END_DECL);
    p = r; // pin = 1
    r = r && InnerDeclaration(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // (PIPE UnionVariant)+
  static boolean MoreUnionVariants(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreUnionVariants")) return false;
    if (!nextTokenIs(b, PIPE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = MoreUnionVariants_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!MoreUnionVariants_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "MoreUnionVariants", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // PIPE UnionVariant
  private static boolean MoreUnionVariants_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MoreUnionVariants_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, PIPE);
    p = r; // pin = 1
    r = r && UnionVariant(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // MinusWithoutTrailingWhitespace Atom
  public static boolean NegateExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NegateExpr")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _COLLAPSE_, NEGATE_EXPR, "<negate expr>");
    r = parseMinusWithoutTrailingWhitespace(b, l + 1);
    p = r; // pin = 1
    r = r && Atom(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // UpperCaseQID
  public static boolean NullaryConstructorArgumentPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NullaryConstructorArgumentPattern")) return false;
    if (!nextTokenIs(b, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UpperCaseQID(b, l + 1);
    exit_section_(b, m, NULLARY_CONSTRUCTOR_ARGUMENT_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // NUMBER_LITERAL
  public static boolean NumberConstantExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NumberConstantExpr")) return false;
    if (!nextTokenIs(b, NUMBER_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NUMBER_LITERAL);
    exit_section_(b, m, NUMBER_CONSTANT_EXPR, r);
    return r;
  }

  /* ********************************************************** */
  // OPERATOR_IDENTIFIER
  public static boolean Operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operator")) return false;
    if (!nextTokenIs(b, OPERATOR_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OPERATOR_IDENTIFIER);
    exit_section_(b, m, OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // OperatorAsFunction_inner
  public static boolean OperatorAsFunctionExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "OperatorAsFunctionExpr")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = OperatorAsFunction_inner(b, l + 1);
    exit_section_(b, m, OPERATOR_AS_FUNCTION_EXPR, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_PARENTHESIS OPERATOR_IDENTIFIER RIGHT_PARENTHESIS
  static boolean OperatorAsFunction_inner(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "OperatorAsFunction_inner")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFT_PARENTHESIS, OPERATOR_IDENTIFIER, RIGHT_PARENTHESIS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_PARENTHESIS Expression RIGHT_PARENTHESIS
  public static boolean ParenthesizedExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParenthesizedExpr")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PARENTHESIS);
    r = r && Expression(b, l + 1);
    r = r && consumeToken(b, RIGHT_PARENTHESIS);
    exit_section_(b, m, PARENTHESIZED_EXPR, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_PARENTHESIS Pattern RIGHT_PARENTHESIS
  static boolean ParenthesizedPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParenthesizedPattern")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PARENTHESIS);
    r = r && Pattern(b, l + 1);
    r = r && consumeToken(b, RIGHT_PARENTHESIS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (ConsPattern | SinglePattern) [AS LowerPattern]
  public static boolean Pattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pattern")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, PATTERN, "<pattern>");
    r = Pattern_0(b, l + 1);
    r = r && Pattern_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ConsPattern | SinglePattern
  private static boolean Pattern_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pattern_0")) return false;
    boolean r;
    r = ConsPattern(b, l + 1);
    if (!r) r = SinglePattern(b, l + 1);
    return r;
  }

  // [AS LowerPattern]
  private static boolean Pattern_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pattern_1")) return false;
    Pattern_1_0(b, l + 1);
    return true;
  }

  // AS LowerPattern
  private static boolean Pattern_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pattern_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AS);
    r = r && LowerPattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PORT LOWER_CASE_IDENTIFIER COLON TypeExpression
  public static boolean PortAnnotation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PortAnnotation")) return false;
    if (!nextTokenIs(b, PORT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PORT_ANNOTATION, null);
    r = consumeTokens(b, 3, PORT, LOWER_CASE_IDENTIFIER, COLON);
    p = r; // pin = COLON
    r = r && TypeExpression(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // RecordBaseIdentifier PIPE
  static boolean RecordBase(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordBase")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = RecordBaseIdentifier(b, l + 1);
    r = r && consumeToken(b, PIPE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER
  public static boolean RecordBaseIdentifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordBaseIdentifier")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, m, RECORD_BASE_IDENTIFIER, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_BRACE RecordInner? RIGHT_BRACE
  public static boolean RecordExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordExpr")) return false;
    if (!nextTokenIs(b, LEFT_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, RECORD_EXPR, null);
    r = consumeToken(b, LEFT_BRACE);
    p = r; // pin = 1
    r = r && report_error_(b, RecordExpr_1(b, l + 1));
    r = p && consumeToken(b, RIGHT_BRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // RecordInner?
  private static boolean RecordExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordExpr_1")) return false;
    RecordInner(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // RecordBase? RecordInnerFields
  static boolean RecordInner(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordInner")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = RecordInner_0(b, l + 1);
    r = r && RecordInnerFields(b, l + 1);
    exit_section_(b, l, m, r, false, ElmParser::record_inner_recover);
    return r;
  }

  // RecordBase?
  private static boolean RecordInner_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordInner_0")) return false;
    RecordBase(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // Field (COMMA Field)*
  static boolean RecordInnerFields(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordInnerFields")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = Field(b, l + 1);
    p = r; // pin = 1
    r = r && RecordInnerFields_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (COMMA Field)*
  private static boolean RecordInnerFields_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordInnerFields_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!RecordInnerFields_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "RecordInnerFields_1", c)) break;
    }
    return true;
  }

  // COMMA Field
  private static boolean RecordInnerFields_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordInnerFields_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && Field(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // LEFT_BRACE <<non_empty_list LowerPattern>> RIGHT_BRACE
  public static boolean RecordPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordPattern")) return false;
    if (!nextTokenIs(b, LEFT_BRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACE);
    r = r && non_empty_list(b, l + 1, ElmParser::LowerPattern);
    r = r && consumeToken(b, RIGHT_BRACE);
    exit_section_(b, m, RECORD_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_BRACE [[RecordBase] <<non_empty_list2 FieldType>>] RIGHT_BRACE
  public static boolean RecordType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordType")) return false;
    if (!nextTokenIs(b, LEFT_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, RECORD_TYPE, null);
    r = consumeToken(b, LEFT_BRACE);
    p = r; // pin = 1
    r = r && report_error_(b, RecordType_1(b, l + 1));
    r = p && consumeToken(b, RIGHT_BRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // [[RecordBase] <<non_empty_list2 FieldType>>]
  private static boolean RecordType_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordType_1")) return false;
    RecordType_1_0(b, l + 1);
    return true;
  }

  // [RecordBase] <<non_empty_list2 FieldType>>
  private static boolean RecordType_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordType_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = RecordType_1_0_0(b, l + 1);
    r = r && non_empty_list2(b, l + 1, ElmParser::FieldType);
    exit_section_(b, m, null, r);
    return r;
  }

  // [RecordBase]
  private static boolean RecordType_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RecordType_1_0_0")) return false;
    RecordBase(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ParenthesizedPattern
  //     | AnythingPattern
  //     | LowerPattern
  //     | UnionPattern
  //     | TuplePattern
  //     | UnitExpr
  //     | ListPattern
  //     | RecordPattern
  //     | LiteralExprGroup
  static boolean SinglePattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SinglePattern")) return false;
    boolean r;
    r = ParenthesizedPattern(b, l + 1);
    if (!r) r = AnythingPattern(b, l + 1);
    if (!r) r = LowerPattern(b, l + 1);
    if (!r) r = UnionPattern(b, l + 1);
    if (!r) r = TuplePattern(b, l + 1);
    if (!r) r = UnitExpr(b, l + 1);
    if (!r) r = ListPattern(b, l + 1);
    if (!r) r = RecordPattern(b, l + 1);
    if (!r) r = LiteralExprGroup(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // TypeRefWithoutArgs
  //     | TypeVariable
  //     | RecordType
  //     | TupleType
  //     | LEFT_PARENTHESIS TypeExpression RIGHT_PARENTHESIS
  static boolean SingleTypeExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SingleTypeExpression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeRefWithoutArgs(b, l + 1);
    if (!r) r = TypeVariable(b, l + 1);
    if (!r) r = RecordType(b, l + 1);
    if (!r) r = TupleType(b, l + 1);
    if (!r) r = SingleTypeExpression_4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LEFT_PARENTHESIS TypeExpression RIGHT_PARENTHESIS
  private static boolean SingleTypeExpression_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SingleTypeExpression_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PARENTHESIS);
    r = r && TypeExpression(b, l + 1);
    r = r && consumeToken(b, RIGHT_PARENTHESIS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // OPEN_QUOTE StringParts CLOSE_QUOTE
  public static boolean StringConstantExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StringConstantExpr")) return false;
    if (!nextTokenIs(b, OPEN_QUOTE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, STRING_CONSTANT_EXPR, null);
    r = consumeToken(b, OPEN_QUOTE);
    p = r; // pin = 1
    r = r && report_error_(b, StringParts(b, l + 1));
    r = p && consumeToken(b, CLOSE_QUOTE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // REGULAR_STRING_PART | STRING_ESCAPE | INVALID_STRING_ESCAPE
  static boolean StringPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StringPart")) return false;
    boolean r;
    r = consumeToken(b, REGULAR_STRING_PART);
    if (!r) r = consumeToken(b, STRING_ESCAPE);
    if (!r) r = consumeToken(b, INVALID_STRING_ESCAPE);
    return r;
  }

  /* ********************************************************** */
  // StringPart*
  static boolean StringParts(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StringParts")) return false;
    Marker m = enter_section_(b, l, _NONE_);
    while (true) {
      int c = current_position_(b);
      if (!StringPart(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "StringParts", c)) break;
    }
    exit_section_(b, l, m, true, false, ElmParser::string_recover);
    return true;
  }

  /* ********************************************************** */
  // !<<eof>> Declaration
  static boolean TopDecl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TopDecl")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = TopDecl_0(b, l + 1);
    p = r; // pin = 1
    r = r && Declaration(b, l + 1);
    exit_section_(b, l, m, r, p, ElmParser::generic_recover);
    return r || p;
  }

  // !<<eof>>
  private static boolean TopDecl_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TopDecl_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // TopDecl (VIRTUAL_END_DECL TopDecl)*
  static boolean TopDeclList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TopDeclList")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TopDecl(b, l + 1);
    r = r && TopDeclList_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (VIRTUAL_END_DECL TopDecl)*
  private static boolean TopDeclList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TopDeclList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TopDeclList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TopDeclList_1", c)) break;
    }
    return true;
  }

  // VIRTUAL_END_DECL TopDecl
  private static boolean TopDeclList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TopDeclList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VIRTUAL_END_DECL);
    r = r && TopDecl(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_PARENTHESIS Expression (COMMA Expression)+ RIGHT_PARENTHESIS
  public static boolean TupleExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleExpr")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TUPLE_EXPR, null);
    r = consumeToken(b, LEFT_PARENTHESIS);
    r = r && Expression(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, TupleExpr_2(b, l + 1));
    r = p && consumeToken(b, RIGHT_PARENTHESIS) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (COMMA Expression)+
  private static boolean TupleExpr_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleExpr_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TupleExpr_2_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!TupleExpr_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TupleExpr_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA Expression
  private static boolean TupleExpr_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleExpr_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LEFT_PARENTHESIS Pattern (COMMA Pattern)+ RIGHT_PARENTHESIS
  public static boolean TuplePattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TuplePattern")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PARENTHESIS);
    r = r && Pattern(b, l + 1);
    r = r && TuplePattern_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_PARENTHESIS);
    exit_section_(b, m, TUPLE_PATTERN, r);
    return r;
  }

  // (COMMA Pattern)+
  private static boolean TuplePattern_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TuplePattern_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TuplePattern_2_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!TuplePattern_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TuplePattern_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA Pattern
  private static boolean TuplePattern_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TuplePattern_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Pattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // UnitExpr
  //     | LEFT_PARENTHESIS TypeExpression (COMMA TypeExpression)+ RIGHT_PARENTHESIS
  public static boolean TupleType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleType")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UnitExpr(b, l + 1);
    if (!r) r = TupleType_1(b, l + 1);
    exit_section_(b, m, TUPLE_TYPE, r);
    return r;
  }

  // LEFT_PARENTHESIS TypeExpression (COMMA TypeExpression)+ RIGHT_PARENTHESIS
  private static boolean TupleType_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleType_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PARENTHESIS);
    r = r && TypeExpression(b, l + 1);
    r = r && TupleType_1_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_PARENTHESIS);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA TypeExpression)+
  private static boolean TupleType_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleType_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TupleType_1_2_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!TupleType_1_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TupleType_1_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA TypeExpression
  private static boolean TupleType_1_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TupleType_1_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TypeExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TYPE ALIAS UPPER_CASE_IDENTIFIER LowerTypeName* EQ TypeExpression
  public static boolean TypeAliasDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeAliasDeclaration")) return false;
    if (!nextTokenIs(b, TYPE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TYPE_ALIAS_DECLARATION, null);
    r = consumeTokens(b, 3, TYPE, ALIAS, UPPER_CASE_IDENTIFIER);
    p = r; // pin = 3
    r = r && report_error_(b, TypeAliasDeclaration_3(b, l + 1));
    r = p && report_error_(b, consumeToken(b, EQ)) && r;
    r = p && TypeExpression(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // LowerTypeName*
  private static boolean TypeAliasDeclaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeAliasDeclaration_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!LowerTypeName(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TypeAliasDeclaration_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER COLON TypeExpression
  public static boolean TypeAnnotation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeAnnotation")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TYPE_ANNOTATION, null);
    r = consumeTokens(b, 2, LOWER_CASE_IDENTIFIER, COLON);
    p = r; // pin = COLON
    r = r && TypeExpression(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // TYPE UPPER_CASE_IDENTIFIER (LowerTypeName)* EQ UnionVariant MoreUnionVariants?
  public static boolean TypeDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeDeclaration")) return false;
    if (!nextTokenIs(b, TYPE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TYPE_DECLARATION, null);
    r = consumeTokens(b, 2, TYPE, UPPER_CASE_IDENTIFIER);
    p = r; // pin = 2
    r = r && report_error_(b, TypeDeclaration_2(b, l + 1));
    r = p && report_error_(b, consumeToken(b, EQ)) && r;
    r = p && report_error_(b, UnionVariant(b, l + 1)) && r;
    r = p && TypeDeclaration_5(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (LowerTypeName)*
  private static boolean TypeDeclaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeDeclaration_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TypeDeclaration_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TypeDeclaration_2", c)) break;
    }
    return true;
  }

  // (LowerTypeName)
  private static boolean TypeDeclaration_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeDeclaration_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = LowerTypeName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // MoreUnionVariants?
  private static boolean TypeDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeDeclaration_5")) return false;
    MoreUnionVariants(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // TypeExpressionInner (ARROW TypeExpressionInner)*
  public static boolean TypeExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, TYPE_EXPRESSION, "<type expression>");
    r = TypeExpressionInner(b, l + 1);
    r = r && TypeExpression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (ARROW TypeExpressionInner)*
  private static boolean TypeExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeExpression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TypeExpression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TypeExpression_1", c)) break;
    }
    return true;
  }

  // ARROW TypeExpressionInner
  private static boolean TypeExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeExpression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ARROW);
    r = r && TypeExpressionInner(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TypeRef
  //     | SingleTypeExpression
  static boolean TypeExpressionInner(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeExpressionInner")) return false;
    boolean r;
    r = TypeRef(b, l + 1);
    if (!r) r = SingleTypeExpression(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // UpperCaseQID (SingleTypeExpression)+
  public static boolean TypeRef(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeRef")) return false;
    if (!nextTokenIs(b, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UpperCaseQID(b, l + 1);
    r = r && TypeRef_1(b, l + 1);
    exit_section_(b, m, TYPE_REF, r);
    return r;
  }

  // (SingleTypeExpression)+
  private static boolean TypeRef_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeRef_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeRef_1_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!TypeRef_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TypeRef_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // (SingleTypeExpression)
  private static boolean TypeRef_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeRef_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = SingleTypeExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // UpperCaseQID
  public static boolean TypeRefWithoutArgs(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeRefWithoutArgs")) return false;
    if (!nextTokenIs(b, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UpperCaseQID(b, l + 1);
    exit_section_(b, m, TYPE_REF, r);
    return r;
  }

  /* ********************************************************** */
  // LOWER_CASE_IDENTIFIER
  public static boolean TypeVariable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeVariable")) return false;
    if (!nextTokenIs(b, LOWER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, m, TYPE_VARIABLE, r);
    return r;
  }

  /* ********************************************************** */
  // AnythingPattern
  //     | LowerPattern
  //     | TuplePattern
  //     | NullaryConstructorArgumentPattern
  //     | UnitExpr
  //     | ListPattern
  //     | RecordPattern
  //     | LiteralExprGroup
  //     | ParenthesizedPattern
  static boolean UnionArgumentPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnionArgumentPattern")) return false;
    boolean r;
    r = AnythingPattern(b, l + 1);
    if (!r) r = LowerPattern(b, l + 1);
    if (!r) r = TuplePattern(b, l + 1);
    if (!r) r = NullaryConstructorArgumentPattern(b, l + 1);
    if (!r) r = UnitExpr(b, l + 1);
    if (!r) r = ListPattern(b, l + 1);
    if (!r) r = RecordPattern(b, l + 1);
    if (!r) r = LiteralExprGroup(b, l + 1);
    if (!r) r = ParenthesizedPattern(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // UpperCaseQID UnionArgumentPattern*
  public static boolean UnionPattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnionPattern")) return false;
    if (!nextTokenIs(b, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UpperCaseQID(b, l + 1);
    r = r && UnionPattern_1(b, l + 1);
    exit_section_(b, m, UNION_PATTERN, r);
    return r;
  }

  // UnionArgumentPattern*
  private static boolean UnionPattern_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnionPattern_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!UnionArgumentPattern(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "UnionPattern_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // UPPER_CASE_IDENTIFIER SingleTypeExpression*
  public static boolean UnionVariant(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnionVariant")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, UNION_VARIANT, "<union variant>");
    r = consumeToken(b, UPPER_CASE_IDENTIFIER);
    p = r; // pin = 1
    r = r && UnionVariant_1(b, l + 1);
    exit_section_(b, l, m, r, p, ElmParser::union_variant_recover);
    return r || p;
  }

  // SingleTypeExpression*
  private static boolean UnionVariant_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnionVariant_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!SingleTypeExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "UnionVariant_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LEFT_PARENTHESIS RIGHT_PARENTHESIS
  public static boolean UnitExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnitExpr")) return false;
    if (!nextTokenIs(b, LEFT_PARENTHESIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFT_PARENTHESIS, RIGHT_PARENTHESIS);
    exit_section_(b, m, UNIT_EXPR, r);
    return r;
  }

  /* ********************************************************** */
  // UPPER_CASE_IDENTIFIER (DotWithoutWhitespace UPPER_CASE_IDENTIFIER)*
  public static boolean UpperCaseQID(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UpperCaseQID")) return false;
    if (!nextTokenIs(b, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, UPPER_CASE_IDENTIFIER);
    r = r && UpperCaseQID_1(b, l + 1);
    exit_section_(b, m, UPPER_CASE_QID, r);
    return r;
  }

  // (DotWithoutWhitespace UPPER_CASE_IDENTIFIER)*
  private static boolean UpperCaseQID_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UpperCaseQID_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!UpperCaseQID_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "UpperCaseQID_1", c)) break;
    }
    return true;
  }

  // DotWithoutWhitespace UPPER_CASE_IDENTIFIER
  private static boolean UpperCaseQID_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UpperCaseQID_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parseDotWithoutWhitespace(b, l + 1);
    r = r && consumeToken(b, UPPER_CASE_IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // InternalValueDeclarationLeft EQ Expression
  public static boolean ValueDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ValueDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, VALUE_DECLARATION, "<value declaration>");
    r = InternalValueDeclarationLeft(b, l + 1);
    r = r && consumeToken(b, EQ);
    p = r; // pin = EQ
    r = r && Expression(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ValueQID
  //     | UpperCaseQID
  public static boolean ValueExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ValueExpr")) return false;
    if (!nextTokenIs(b, "<value expr>", LOWER_CASE_IDENTIFIER, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VALUE_EXPR, "<value expr>");
    r = ValueQID(b, l + 1);
    if (!r) r = UpperCaseQID(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (UPPER_CASE_IDENTIFIER DotWithoutWhitespace)* LOWER_CASE_IDENTIFIER
  public static boolean ValueQID(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ValueQID")) return false;
    if (!nextTokenIs(b, "<value qid>", LOWER_CASE_IDENTIFIER, UPPER_CASE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VALUE_QID, "<value qid>");
    r = ValueQID_0(b, l + 1);
    r = r && consumeToken(b, LOWER_CASE_IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (UPPER_CASE_IDENTIFIER DotWithoutWhitespace)*
  private static boolean ValueQID_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ValueQID_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ValueQID_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ValueQID_0", c)) break;
    }
    return true;
  }

  // UPPER_CASE_IDENTIFIER DotWithoutWhitespace
  private static boolean ValueQID_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ValueQID_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, UPPER_CASE_IDENTIFIER);
    r = r && parseDotWithoutWhitespace(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(VIRTUAL_END_DECL|VIRTUAL_END_SECTION)
  static boolean case_branch_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "case_branch_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !case_branch_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // VIRTUAL_END_DECL|VIRTUAL_END_SECTION
  private static boolean case_branch_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "case_branch_recover_0")) return false;
    boolean r;
    r = consumeToken(b, VIRTUAL_END_DECL);
    if (!r) r = consumeToken(b, VIRTUAL_END_SECTION);
    return r;
  }

  /* ********************************************************** */
  // !(COMMA | RIGHT_PARENTHESIS | VIRTUAL_END_DECL)
  static boolean exposed_item_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exposed_item_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !exposed_item_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // COMMA | RIGHT_PARENTHESIS | VIRTUAL_END_DECL
  private static boolean exposed_item_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "exposed_item_recover_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, RIGHT_PARENTHESIS);
    if (!r) r = consumeToken(b, VIRTUAL_END_DECL);
    return r;
  }

  /* ********************************************************** */
  // COMMA | LEFT_BRACE | LEFT_PARENTHESIS | LEFT_SQUARE_BRACKET
  static boolean expr_delimiters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr_delimiters")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, LEFT_BRACE);
    if (!r) r = consumeToken(b, LEFT_PARENTHESIS);
    if (!r) r = consumeToken(b, LEFT_SQUARE_BRACKET);
    return r;
  }

  /* ********************************************************** */
  // !VIRTUAL_END_DECL
  static boolean generic_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, VIRTUAL_END_DECL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !(VIRTUAL_END_DECL|VIRTUAL_END_SECTION)
  static boolean inner_decl_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inner_decl_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !inner_decl_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // VIRTUAL_END_DECL|VIRTUAL_END_SECTION
  private static boolean inner_decl_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inner_decl_recover_0")) return false;
    boolean r;
    r = consumeToken(b, VIRTUAL_END_DECL);
    if (!r) r = consumeToken(b, VIRTUAL_END_SECTION);
    return r;
  }

  /* ********************************************************** */
  // !(VIRTUAL_END_DECL | IMPORT | Declaration)
  static boolean module_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !module_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // VIRTUAL_END_DECL | IMPORT | Declaration
  private static boolean module_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_recover_0")) return false;
    boolean r;
    r = consumeToken(b, VIRTUAL_END_DECL);
    if (!r) r = consumeToken(b, IMPORT);
    if (!r) r = Declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // <<p>> (COMMA <<p>>)*
  static boolean non_empty_list(PsiBuilder b, int l, Parser _p) {
    if (!recursion_guard_(b, l, "non_empty_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = _p.parse(b, l);
    r = r && non_empty_list_1(b, l + 1, _p);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA <<p>>)*
  private static boolean non_empty_list_1(PsiBuilder b, int l, Parser _p) {
    if (!recursion_guard_(b, l, "non_empty_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!non_empty_list_1_0(b, l + 1, _p)) break;
      if (!empty_element_parsed_guard_(b, "non_empty_list_1", c)) break;
    }
    return true;
  }

  // COMMA <<p>>
  private static boolean non_empty_list_1_0(PsiBuilder b, int l, Parser _p) {
    if (!recursion_guard_(b, l, "non_empty_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && _p.parse(b, l);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // <<p>> (COMMA <<p>>)*
  static boolean non_empty_list2(PsiBuilder b, int l, Parser _p) {
    if (!recursion_guard_(b, l, "non_empty_list2")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = _p.parse(b, l);
    p = r; // pin = 1
    r = r && non_empty_list2_1(b, l + 1, _p);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (COMMA <<p>>)*
  private static boolean non_empty_list2_1(PsiBuilder b, int l, Parser _p) {
    if (!recursion_guard_(b, l, "non_empty_list2_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!non_empty_list2_1_0(b, l + 1, _p)) break;
      if (!empty_element_parsed_guard_(b, "non_empty_list2_1", c)) break;
    }
    return true;
  }

  // COMMA <<p>>
  private static boolean non_empty_list2_1_0(PsiBuilder b, int l, Parser _p) {
    if (!recursion_guard_(b, l, "non_empty_list2_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && _p.parse(b, l);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !(RIGHT_BRACE|VIRTUAL_END_SECTION|VIRTUAL_END_DECL)
  static boolean record_inner_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "record_inner_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !record_inner_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // RIGHT_BRACE|VIRTUAL_END_SECTION|VIRTUAL_END_DECL
  private static boolean record_inner_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "record_inner_recover_0")) return false;
    boolean r;
    r = consumeToken(b, RIGHT_BRACE);
    if (!r) r = consumeToken(b, VIRTUAL_END_SECTION);
    if (!r) r = consumeToken(b, VIRTUAL_END_DECL);
    return r;
  }

  /* ********************************************************** */
  // !(CLOSE_QUOTE|VIRTUAL_END_DECL|expr_delimiters)
  static boolean string_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !string_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // CLOSE_QUOTE|VIRTUAL_END_DECL|expr_delimiters
  private static boolean string_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_recover_0")) return false;
    boolean r;
    r = consumeToken(b, CLOSE_QUOTE);
    if (!r) r = consumeToken(b, VIRTUAL_END_DECL);
    if (!r) r = expr_delimiters(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // !(PIPE|VIRTUAL_END_DECL)
  static boolean union_variant_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_variant_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !union_variant_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PIPE|VIRTUAL_END_DECL
  private static boolean union_variant_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_variant_recover_0")) return false;
    boolean r;
    r = consumeToken(b, PIPE);
    if (!r) r = consumeToken(b, VIRTUAL_END_DECL);
    return r;
  }

}
