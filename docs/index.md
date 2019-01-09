Elm language plugin for JetBrains IDEs (IntelliJ, WebStorm, PyCharm, etc.)

# Features 

* Code completion
* Go to declaration
* Go to symbol
* Find usages
* Type Inference and Type Checking
* Rename refactoring
* Cleanup unused imports
* Detect unused code
* Code folding
* Syntax highlighting
* Mark unresolvable references as errors
* 'Import' quick fix for unresolved references
* Structure view
* and more...


## In More Detail

IntelliJ is a powerful and free IDE built on a unique architecture. Rather than working with source code in its text form, IntelliJ parses the source code into an abstract syntax tree (AST). All functionality and refactorings are built on this AST, allowing for much more sophisticated features than normally possible in a text editor that relies on regex and find/replace.

Here are a few of the advanced features provided by [intellij-elm](https://github.com/klazuka/intellij-elm): 


### Add Import Quick Fix

While you write code, intellij-elm checks that the functions and types that your program uses can be resolved based on what's in scope. Errors are marked in red. If the error can be resolved by adding an import, intellij-elm will offer a quick fix action. Press Option-Enter (Alt-Enter on Windows) and then select "Import" to generate the appropriate `import` and add it to the top of your file. No more scrolling to the top and then back down again just to fiddle with imports!

<video controls>
    <source src="assets/import_quick_fix.mp4" type="video/mp4">
</video>
<br/>


### Cleanup Unused Imports

Elm code tends to accumulate imports over time, some of which may no longer be needed. Unused imports are detected by the plugin and shown in gray. The plugin also detects functions exposed in an import but the exposed name is unused. In both cases, the plugin offers a quick-fix to cleanup the imports. Put the cursor on an unused import, then press Option-Enter (Alt-Enter on Windows), and and then select "Optimize imports".

<video controls>
    <source src="assets/optimize_imports.mp4" type="video/mp4">
</video>
<br/>


### Rename Refactoring

Reliably rename types, functions, values, modules and files. Press Shift-F6 to rename the identifier under the cursor. All references will also be renamed, including `import` statements and `exposing` lists.

<video controls>
    <source src="assets/rename.mp4" type="video/mp4">
</video>
<br/>


### Find Usages
 
Find all usages of a type, function, value or module. Press Option-F7 (Ctrl-F7 on Windows) while the cursor is on an identifier to find all of its usages.

<video controls>
    <source src="assets/find_usages.mp4" type="video/mp4">
</video>
<br/>


### Detect Unused Code

intellij-elm automatically detects unused functions, parameters, and types. Unused code will be displayed in gray, indicating that it can safely be deleted.

<video controls>
    <source src="assets/unused_code.mp4" type="video/mp4">
</video>
<br/>


### Infer Type of Expression

Show the inferred type of any Elm expression. Press Ctrl-Shift-P to see the type of the expression under the cursor, or expand the selection to include parent expressions.

<video controls>
    <source src="assets/expr_type_inference.mp4" type="video/mp4">
</video>
<br/>


### Type Checking

Type mismatches are detected and shown directly within the editor. For instance, if you try to call a function that expects a `String` but you give it a `Char`, an error will be shown on the bad argument. The type checking is performed immediately within the plugin--no need to wait for the Elm compiler to be launched in an external process.

<video controls>
    <source src="assets/type_checking.mp4" type="video/mp4">
</video>
<br/>


### Quick Docs
 
Press Ctrl-J to display the documentation for a function, type, etc. It even works with doc comments within your own code.

<video controls>
    <source src="assets/quick_docs.mp4" type="video/mp4">
</video>
<br/>
 

### Structure View

Get a 30,000 ft view of your Elm file's types, functions and values.

<img src="assets/structure_view.jpg"/>

Press Cmd-F12 (Ctrl-F12 on Windows) to popup the quick structure navigator. Use your keyboard to jump between functions and types effortlessly.

<video controls>
    <source src="assets/quick_nav_structure.mp4" type="video/mp4">
</video>
<br/>


### Code Folding

By default, a module's `import`s are automatically collapsed using code folding. You can also collapse function and type declarations.

<video controls>
    <source src="assets/folding_imports.mp4" type="video/mp4">
</video>
<br/>


### Manage the Exposing List

Tired of jumping around to the top of an Elm file to add/remove something from the exposing list? intellij-elm makes it easy by providing an intention action directly on the function/type declaration. Just put the cursor on the name of the function/type, press Option-enter, and then select "Add to exposing list" (or, if it's already exposed, "Remove from exposing list").

A special icon will be displayed next to exposed functions/types in the gutter next to the line number. Click the icon to jump to where it's exposed.


<video controls>
    <source src="assets/edit_exposing_list.mp4" type="video/mp4">
</video>
<br/>


### Type-annotation-driven Development

When writing new code, it's often helpful to write the type annotations first. Once you've written a type annotation, press Option-Enter (Alt-Enter on Windows) and select "Create" from the quick fix menu. A function declaration will be generated for you based on your type annotation. 

<video controls>
    <source src="assets/generate_func_decl.mp4" type="video/mp4">
</video>


------------------------------------------------------------


# Installation

You can install the plugin from within the JetBrains IDE by going to `Settings -> Plugins` and then searching for "Elm". Then click the `Search in repositories` link.

If, however, you need to install a specific version of the plugin, you can get it from [the releases page](https://github.com/klazuka/intellij-elm/releases).


# Integrating with elm-format

See the elm-format [integration instructions](https://klazuka.github.io/intellij-elm/elm-format/setup.html).


# FAQ

- Is this free?
    - Yes, the plugin itself is free and it works with the free version of IntelliJ.
- What versions of IntelliJ does this work with?
    - IntelliJ IDEA Ultimate
    - IntelliJ IDEA Community Edition (**free!**)
    - PyCharm Professional
    - PyCharm Community (**free!**)
    - WebStorm
    - and others (untested)
