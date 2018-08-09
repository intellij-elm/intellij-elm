package org.elm.lang.core.psi.tags

import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmParametricTypeRef
import org.elm.lang.core.psi.elements.ElmTypeRef

/** An element that can be a parameter of an [ElmTypeRef], but not necessarily an [ElmParametricTypeRef] */
interface ElmTypeRefParameter : ElmPsiElement
