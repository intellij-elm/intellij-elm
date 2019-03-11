package org.elm.workspace.compiler

// types for Elm's json report

data class Region(val end: End, val start: Start)

data class Start(val column: Int, val line: Int)

data class End(val column: Int, val line: Int)


// plugin's types

data class MessageAndRegion(val message: String, val region: Region, val title: String)

data class CompilerMessage(val name: String, val path: String, val messageWithRegion: MessageAndRegion)


