package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.ElmReferenceCached
import org.elm.lang.core.stubs.index.ElmModulesIndex
import org.elm.openapiext.findFileByMaybeRelativePath
import org.elm.openapiext.findFileByPath
import org.elm.workspace.*

private val log = logger<ElmImportClause>()

/**
 * An import declaration at the top of the module.
 *
 * e.g. 'import Data.User exposing (User, name, age)'
 *
 * Role:
 * - refers to the module from which values and types should be imported
 * - possibly introduces an alias name for the module
 * - expose individual values and types from the module
 */
class ElmImportClause(node: ASTNode) : ElmPsiElementImpl(node), ElmReferenceElement {

    val moduleQID: ElmUpperCaseQID
        get() = findNotNullChildByClass(ElmUpperCaseQID::class.java)

    val asClause: ElmAsClause?
        get() = findChildByClass(ElmAsClause::class.java)

    val exposingList: ElmExposingList?
        get() = findChildByClass(ElmExposingList::class.java)


    val exposesAll: Boolean
        get() = exposingList?.doubleDot != null


    override val referenceNameElement: PsiElement
        get() = moduleQID

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() =
            object : ElmReferenceCached<ElmImportClause>(this) {

                override fun resolveInner(): ElmNamedElement? {
                    return getVariants().find { it.name == element.referenceName }
                }

                override fun getVariants(): Array<ElmNamedElement> {
                    val virtualFile = element.elmFile.virtualFile
                    val elmProject = element.project.elmWorkspace.findProjectForFile(virtualFile)

                    // The lightweight integration tests do not have an associated Elm Project,
                    // so we will just treat it as if all Elm files were in scope.
                    val allowAllProjects = if (elmProject == null) {
                        log.warn("Could not find Elm project containing ${virtualFile.path}")
                        true
                    } else {
                        false
                    }

                    return ElmModulesIndex.getAll(element.project)
                            .filter { allowAllProjects || elmProject!!.exposes(it) }
                            .toTypedArray()
                }
            }
}

/**
 * Returns true if [moduleDeclaration] is visible within the receiver [ElmProject].
 */
private fun ElmProject.exposes(moduleDeclaration: ElmModuleDeclaration): Boolean {
    // Since we do not fully support project-scope name resolution for Elm 0.18 projects,
    // we will treat all Elm modules as visible.
    when (this) {
        is ElmApplicationProject ->
            if (elmVersion == Version(0, 18, 0))
                return true

        is ElmPackageProject ->
            if (elmVersion.low == Version(0, 18, 0))
                return true
    }

    val elmModuleRelativePath = moduleDeclaration.name.replace('.', '/') + ".elm"

    // check if the module is reachable from the top-level of the containing Elm project
    val virtualDirs = sourceDirectories.map { projectDirPath.resolve(it) }
            .mapNotNull { LocalFileSystem.getInstance().findFileByPath(it) }
    if (virtualDirs.none { it.findFileByMaybeRelativePath(elmModuleRelativePath) != null })
        return false

    // TODO [kl] check if the module is reachable from a DIRECT dependency


    return true
}
