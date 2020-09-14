package org.elm.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.elm.lang.core.ElmFileType
import org.elm.lang.core.resolve.reference.ElmReference
import org.elm.workspace.ElmProject
import org.elm.workspace.elmWorkspace


/**
 * Base interface for all Elm Psi elements
 */
interface ElmPsiElement : PsiElement {
    /**
     * Get the file containing this element as an [ElmFile]
     */
    val elmFile: ElmFile

    /**
     * Get the Elm project which this element's file belongs to.
     *
     * Returns null if the containing Elm project's manifest (`elm.json`) has not
     * yet been attached to the workspace.
     */
    val elmProject: ElmProject?
}

/**
 * Base class for normal Elm Psi elements
 */
abstract class ElmPsiElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), ElmPsiElement {

    override val elmFile: ElmFile
        get() = containingFile as ElmFile

    override val elmProject: ElmProject?
        get() = elmFile.elmProject

    // Make the type-system happy by using our reference interface instead of PsiReference
    override fun getReferences(): Array<ElmReference> {
        val ref = getReference() as? ElmReference ?: return EMPTY_REFERENCE_ARRAY
        return arrayOf(ref)
    }
}

/**
 * Base class for Elm Psi elements which can be stubbed
 */
abstract class ElmStubbedElement<StubT : StubElement<*>>
    : StubBasedPsiElementBase<StubT>, StubBasedPsiElement<StubT>, ElmPsiElement {

    constructor(node: ASTNode)
            : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>)
            : super(stub, nodeType)

    override val elmFile: ElmFile
        get() = containingFile as ElmFile

    override val elmProject: ElmProject?
        get() = project.elmWorkspace.findProjectForFile(elmFile.virtualFile)

    // Make the type-system happy by using our reference interface instead of PsiReference
    override fun getReferences(): Array<ElmReference> {
        val ref = getReference() as? ElmReference ?: return EMPTY_REFERENCE_ARRAY
        return arrayOf(ref)
    }

    override fun getUseScope(): SearchScope {
        // Restrict find-usages to only look at `*.elm` files in the current IntelliJ project.
        val baseScope = GlobalSearchScope.projectScope(project)
        return GlobalSearchScope.getScopeRestrictedByFileTypes(baseScope, ElmFileType)
    }

    // this is needed to match how [ASTWrapperPsiElement] implements `toString()`
    override fun toString(): String =
            "${javaClass.simpleName}($elementType)"
}

private val EMPTY_REFERENCE_ARRAY = emptyArray<ElmReference>()