package org.elm.lang.core.psi.tags

import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.psi.elements.ElmConsPattern
import org.elm.lang.core.psi.elements.ElmPattern

/** An element that can be the direct child of an [ElmPattern], but not necessarily an [ElmConsPattern] */
interface ElmPatternChildTag : ElmPsiElement
