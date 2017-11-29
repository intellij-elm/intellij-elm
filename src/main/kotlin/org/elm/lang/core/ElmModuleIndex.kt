package org.elm.lang.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.elm.lang.core.psi.ElmFile


class ElmModuleIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void?> {
        return ELM_MODULE_INDEX
    }

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return INDEXER
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return KEY_DESCRIPTOR
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return INPUT_FILTER
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }

    override fun getVersion(): Int {
        return 0
    }

    companion object {
        private val ELM_MODULE_INDEX = ID.create<String, Void?>("ElmModuleIndex")
        private val PLATFORM_PREFIX = "Platform."

        private val KEY_DESCRIPTOR = EnumeratorStringDescriptor()

        private val INDEXER = object : DataIndexer<String, Void?, FileContent> {
            override fun map(inputData: FileContent): Map<String, Void?> {
                val elmFile = inputData.getPsiFile() as? ElmFile
                val moduleName = elmFile?.getModuleName()

                return if (moduleName == null) {
                    emptyMap()
                } else if (moduleName.startsWith(PLATFORM_PREFIX)) {
                    // The Elm compiler implicitly imports `Platform.Cmd` and `Platform.Sub`
                    // using the aliases `Cmd` and `Sub` respectively. So we index them
                    // under both the full module name as well as the alias.
                    mapOf(
                            moduleName to null,
                            moduleName.substring(PLATFORM_PREFIX.length) to null
                    )
                } else {
                    mapOf(moduleName to null)
                }
            }
        }

        private val INPUT_FILTER: FileBasedIndex.InputFilter = object : FileBasedIndex.InputFilter {
            override fun acceptInput(file: VirtualFile) =
                    file.getFileType() === ElmFileType && file.isInLocalFileSystem()
        }

        fun getFileByModuleName(moduleName: String, project: Project): ElmFile? {
            // TODO [kl] log a warning if there is more than one result?
            return getFilesByModuleName(moduleName, project)
                    .firstOrNull()
        }

        fun getFilesByModuleName(moduleName: String, project: Project): List<ElmFile> {
            val virtualFiles = getVirtualFilesByModuleName(moduleName, GlobalSearchScope.projectScope(project))
            return virtualFiles.mapNotNull {
                PsiManager.getInstance(project).findFile(it) as? ElmFile
            }
        }

        fun getAllModuleNames(project: Project): Set<String> {
            return FileBasedIndex.getInstance().getAllKeys(ELM_MODULE_INDEX, project).toSet()
        }

        fun getAllModules(project: Project): List<ElmFile> {
            // NOTE: we are using the IntelliJ file index here--not the index by module name.
            // The index by module name doesn't provide any advantage in this case since
            // we are getting ALL modules.
            return FilenameIndex.getAllFilesByExt(project, "elm")
                    .sortedWith(elmAppVsLibraryComparator)
                    .mapNotNull { PsiManager.getInstance(project).findFile(it) as? ElmFile }
        }

        /**
         * The core function for looking up a module by name.
         */
        private fun getVirtualFilesByModuleName(moduleName: String, searchScope: GlobalSearchScope): List<VirtualFile> {
            return FileBasedIndex.getInstance()
                    .getContainingFiles(ELM_MODULE_INDEX, moduleName, searchScope)
                    .sortedWith(elmAppVsLibraryComparator)
        }
    }
}

// There should be only a single file in project scope for a given module name,
// but until we start loading the `elm-package.json` manifest file to determine
// source roots and library roots, we will need to deal with indexed Elm files
// that do not truly belong to THIS project. To mitigate the problem, we will
// de-prioritize any matched files that are in the `elm-stuff` directory by
// sorting them so that they occur last when ordered in a list.
private val elmAppVsLibraryComparator =
        compareBy<VirtualFile> {
            if (it.path.contains("/elm-stuff/")) 1 else 0
        }
