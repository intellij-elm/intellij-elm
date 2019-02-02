package org.elm.lang.core.lookup

import com.intellij.openapi.diagnostic.logger
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.stubs.index.ElmNamedElementIndex
import org.elm.lang.core.types.moduleName

/**
 * Like [ElmNamedElementIndex] but takes context into account. Given that a single
 * IntelliJ project can hold multiple Elm projects, it is important that we take into
 * account the caller's Elm project structure when looking for things by name.
 */
object ElmLookup {

    val log = logger<ElmLookup>()

    /** Find the named element with [name] which is visible to [clientLocation] */
    inline fun <reified T : ElmNamedElement> findByName(
            name: String,
            clientLocation: ClientLocation
    ): List<T> {
        if (clientLocation.elmProject == null) {
            if (log.isDebugEnabled) log.debug("Cannot lookup '$name' when Elm project context is unknown")
            return emptyList()
        }
        return ElmNamedElementIndex.find(name, clientLocation.intellijProject, clientLocation.searchScope())
                .filterIsInstance<T>()
    }


    /** Find the named element with [name] declared in [module] and visible to [clientLocation] */
    inline fun <reified T : ElmNamedElement> findByNameAndModule(
            name: String,
            module: String,
            clientLocation: ClientLocation
    ): T? =
            findByName<T>(name, clientLocation)
                    .find { it.moduleName == module }
}