package org.elm.lang.core.resolve.scope

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import org.elm.lang.core.modulePathToFile
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration

/**
 * A scope that allows exposed values and types from the module named [elmFile]
 * to be imported. You can think of it as the view of the module from the outside.
 *
 * @see ModuleScope for the view of the module from inside
 */
class ImportScope(val elmFile: ElmFile) {

    companion object {

        /**
         * Returns an [ImportScope] for the module which is being imported by [importDecl].
         */
        fun fromImportDecl(importDecl: ElmImportClause): ImportScope? {
            val moduleName = importDecl.moduleQID.upperCaseIdentifierList.joinToString(".") { it.text }
            return modulePathToFile(moduleName, importDecl.moduleQID.project)?.let { ImportScope(it) }
        }

        fun allElmFiles(project: Project): List<ElmFile> {
            return FilenameIndex.getAllFilesByExt(project, "elm")
                    .mapNotNull { PsiManager.getInstance(project).findFile(it) as? ElmFile }
        }

        /**
         * Returns an [ImportScope] for the module named by [qualifierPrefix] reachable
         * via [elmFile], either via import declarations or implicit imports from Elm's
         * Core standard library.
         */
        fun fromQualifierPrefixInModule(qualifierPrefix: String, elmFile: ElmFile): ImportScope? {
            // handle implicit imports from Core
            val targetFile = modulePathToFile(qualifierPrefix, elmFile.project)
            if (targetFile?.isCore() == true)
                return ImportScope(targetFile)

            // handle explicit import from within this module
            val importDecl = ModuleScope(elmFile).importDeclForQualifierPrefix(qualifierPrefix)
            return importDecl?.let { ImportScope.fromImportDecl(it) }
        }
    }


    fun getExposedValues(): List<ElmNamedElement> {
        val moduleDecl = elmFile.findChildByClass(ElmModuleDeclaration::class.java)
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope(elmFile).getDeclaredValues()

        return moduleDecl.exposingList.exposedValueList
                .mapNotNull { it.reference.resolve() as? ElmNamedElement }
    }

    fun getExposedTypes(): List<ElmNamedElement> {
        val moduleDecl = elmFile.findChildByClass(ElmModuleDeclaration::class.java)
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope(elmFile).getDeclaredTypes()

        return moduleDecl.exposingList.exposedTypeList
                .mapNotNull { it.reference.resolve() as? ElmNamedElement }
    }

    fun getExposedUnionOrRecordConstructors(): List<ElmNamedElement> {
        val moduleDecl = elmFile.findChildByClass(ElmModuleDeclaration::class.java)
                ?: return emptyList()

        if (moduleDecl.exposesAll)
            return ModuleScope(elmFile).getDeclaredUnionOrRecordConstructors()

        return moduleDecl.exposingList.exposedTypeList
                .flatMap {
                    val ctors = it.exposedUnionConstructors
                    when {
                        it.exposesAll ->
                            // It's a union type that exposes all of its constructors
                            (it.reference.resolve() as? ElmTypeDeclaration)?.unionMemberList ?: emptyList()

                        ctors != null ->
                            // It's a union type that exposes one or more constructors
                            ctors.exposedUnionConstructors.mapNotNull { it.reference.resolve() as? ElmNamedElement }

                        else -> {
                            // It's either a record type or a union type without any exposed constructors
                            val targetType = it.reference.resolve() as? ElmTypeAliasDeclaration
                            if (targetType != null && targetType.typeRef.isRecord)
                                listOf(targetType)
                            else
                                emptyList<ElmNamedElement>()
                        }
                    }
                }
    }
}
