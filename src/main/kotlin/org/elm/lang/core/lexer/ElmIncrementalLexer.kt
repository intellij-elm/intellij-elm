package org.elm.lang.core.lexer

import com.intellij.lexer.FlexAdapter

class ElmIncrementalLexer : FlexAdapter(_ElmLexer(null))
