package org.elm.lang.core.psi

/**
 * Marker interface for Qualified ID elements (QIDs)
 *
 * A qualified id is a value or type identifier that may include a module/alias prefix.
 *
 * e.g. `List.length [1,2,3]` refers to the `length` value in the `List` module.
 */
interface ElmQID: ElmPsiElement
