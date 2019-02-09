package org.elm.lang.core.resolve.reference

/**
 * A reference which is qualified by a module name (or an imported module's alias)
 *
 * @property qualifierPrefix the module name or alias (e.g. `Foo` in `Foo.bar`)
 * @property nameWithoutQualifier the bare, unqualified name (e.g. `bar` in `Foo.bar`)
 */
interface QualifiedReference : ElmReference {
    val qualifierPrefix: String
    val nameWithoutQualifier: String
}