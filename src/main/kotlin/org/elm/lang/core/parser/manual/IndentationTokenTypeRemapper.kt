package org.elm.lang.core.parser.manual

import com.intellij.lang.ITokenTypeRemapper
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

import java.util.*

import org.elm.lang.core.psi.ElmTypes

class IndentationTokenTypeRemapper private constructor() : ITokenTypeRemapper {

    private var indentations: Stack<Int>? = null

    init {
        this.reset()
    }

    fun pushIndentation(indentation: Int) {
        if (indentation > 0) {
            this.indentations!!.push(indentation)
        }
    }

    override fun filter(type: IElementType, start: Int, end: Int, text: CharSequence): IElementType {
        if (!this.indentations!!.empty() && TokenType.WHITE_SPACE == type) {
            val i = IndentationHelper.getIndentation(text, start, end)
            if (i > 0) {
                if (this.indentations!!.search(i) > 0) {
                    return ElmTypes.SEPARATION_BY_INDENTATION
                }
            }
        } else if (ElmTypes.FRESH_LINE == type || end == text.length) {
            this.reset()
        }
        return type
    }

    fun reset() {
        if (this.indentations == null || !this.indentations!!.empty()) {
            this.indentations = Stack()
        }
    }

    companion object {
        private val instances = Collections.synchronizedMap(HashMap<Long, IndentationTokenTypeRemapper>())

        fun <T> use(callback: (IndentationTokenTypeRemapper,T)->T, input: T): T {
            val instance = getInstance()
            val indentationsBackup = instance.indentations
            @Suppress("UNCHECKED_CAST")
            instance.indentations = instance.indentations!!.clone() as Stack<Int>
            val inputAfterCallback = callback(instance, input)
            instance.indentations = indentationsBackup
            return inputAfterCallback
        }

        private fun getInstance(): IndentationTokenTypeRemapper {
            val key = Thread.currentThread().id
            return instances.getOrPut(key, { IndentationTokenTypeRemapper() })
        }
    }
}
