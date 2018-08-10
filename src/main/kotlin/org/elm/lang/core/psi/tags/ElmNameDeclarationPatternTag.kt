package org.elm.lang.core.psi.tags

import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmAnonymousFunction
import org.elm.lang.core.psi.elements.ElmCaseOfBranch
import org.elm.lang.core.psi.elements.ElmFunctionDeclarationLeft

/** An element that can be the parameter of an [ElmFunctionDeclarationLeft], [ElmAnonymousFunction],
 * or [ElmCaseOfBranch] */
interface ElmNameDeclarationPatternTag : ElmNamedElement
