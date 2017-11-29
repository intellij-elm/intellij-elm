package org.elm.lang.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
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
            return ElmModuleIndex.getFilesByModuleName(moduleName, project).firstOrNull()
        }

        fun getFilesByModuleName(moduleName: String, project: Project): List<ElmFile> {
            return getFilesByModuleName(moduleName, project, GlobalSearchScope.projectScope(project))
        }

        fun getAllModuleNames(project: Project): Collection<String> {
            return FileBasedIndex.getInstance().getAllKeys(ELM_MODULE_INDEX, project)
        }

        private fun getFilesByModuleName(moduleName: String, project: Project, searchScope: GlobalSearchScope): List<ElmFile> {
            val psiManager = PsiManager.getInstance(project)
            val virtualFiles = getVirtualFilesByModuleName(moduleName, searchScope)
            return virtualFiles.mapNotNull {
                val psiFile = psiManager.findFile(it)
                if (psiFile is ElmFile) psiFile else null
            }
        }

        private fun getVirtualFilesByModuleName(moduleName: String, searchScope: GlobalSearchScope): Collection<VirtualFile> {
            return FileBasedIndex.getInstance().getContainingFiles(ELM_MODULE_INDEX, moduleName, searchScope)
        }
    }
}