[![CircleCI](https://circleci.com/gh/klazuka/intellij-elm.svg?style=svg)](https://circleci.com/gh/klazuka/intellij-elm)

Provides support for the [Elm](http://elm-lang.org) programming language in IntelliJ and WebStorm.


# Features 

* Code completion
* Go to declaration
* Go to symbol
* Find usages
* Type Inference and Type Checking (see below for more info)
* Rename refactoring
* Graphical UI for running elm-test
* Reformat code using elm-format
* Cleanup unused imports
* Detect unused code
* Code folding
* Syntax highlighting
* Mark unresolvable references as errors
* 'Import' quick fix for unresolved references
* Structure view
* WebGL/GLSL support
* and more...

Watch the [feature demo videos](https://klazuka.github.io/intellij-elm/) to learn more.


# Getting Started

## Installation

You can install the plugin from within the JetBrains IDE by going to `Settings -> Plugins` and then searching for "Elm". Then click the `Search in repositories` link.

If, however, you need to install a specific version of the plugin, you can get it from [the releases page](https://github.com/klazuka/intellij-elm/releases).


## Creating a new Elm project

Ready to start a new Elm project in IntelliJ? First, make sure that you have the Elm plugin installed (see above). When you open IntelliJ, you'll see a launch screen.

1. Click 'Create New Project'
2. Select 'Elm' from the panel on the left.
3. Click the 'Next' button
4. Give your project a name and a location on disk


## Opening an existing Elm project

Already have some Elm code that you want to edit in IntelliJ? First, make sure that you have the Elm plugin installed (see above). When you open IntelliJ, you'll see a launch screen.

You might be tempted to click "Import Project" but there's a simpler way. Instead, click the "Open" button and select your existing project directory.

Once your project loads and you open a `.elm` file, you will be prompted to setup the toolchain (paths to things like the Elm compiler as well as elm-format and elm-test). After that's configured, you'll be prompted to attach an `elm.json` file. Use the file picker to locate the `elm.json` file for your project.


# Integrations

## elm-format

With a tiny bit of configuration, the Elm plugin can re-format your code using [elm-format](https://github.com/avh4/elm-format). You can choose whether you want it to run when a keyboard shortcut is invoked or run automatically whenever an Elm file is saved.

Once elm-format has been installed, configure intellij-elm as follows:

1. Open IntelliJ settings.
2. Select 'Languages & Frameworks' from the left-side pane
3. Select 'Elm'
4. Fill out the section titled 'elm-format' (use the 'Auto Discover' button to search for elm-format in common locations) 


## elm-test

[elm-test](https://github.com/elm-explorations/test) provides a way to write tests and run them from the command line. The Elm plugin for IntelliJ takes that a step further. You can run your tests directly from within IntelliJ and the test results will be shown with green and red lights for each test, indicating success and failure respectively.

Once elm-test has been installed, configure intellij-elm as follows:

1. Open IntelliJ settings.
2. Select 'Languages & Frameworks' from the left-side pane
3. Select 'Elm'
4. Fill out the section titled 'elm-test' (use the 'Auto Discover' button to search for elm-test in common locations) 


## WebGL/GLSL

Does your project use WebGL/GLSL? If you install the [GLSL language plugin](https://plugins.jetbrains.com/plugin/6993-glsl-support), all of its features will be available in GLSL code blocks in your Elm files. This includes syntax highlighting, code completion and rename support.



# Type Inference and Type Checking

The plugin can perform type inference on arbitrary expressions in your program. Press `ctrl-shift-p` to display the inferred type of the expression under the cursor/caret.

The plugin also performs type checking, marking incompatible types in red.

The current implementation of the type system does not infer the types of parameters of unannotated
functions, but this limitation will be removed in the future.


# Attribution

This project heavily leverages the open-source work of other IntelliJ plugin developers:

* Code organization and testing utils based on the [Rust plugin](https://github.com/intellij-rust/intellij-rust) for IntelliJ. 
* Lexer and Parser originally from Kamil Durkiewicz's [elm-intellij-plugin](https://github.com/durkiewicz/elm-plugin).
* Offside-rule Lexer originally based on code from Alexander Kiel's [idea-haskell](https://github.com/alexanderkiel/idea-haskell) plugin.

Thank you Kamil, Alexander, and the Rust plugin developers.


# Donation

If this plugin improves your Elm development experience, and you want to give back, please consider making a [donation](http://www.brahmrishiyoga.org/donate) (tax-deductible in the US) to the [Narmada Vidya Peeth school](http://www.brahmrishiyoga.org/jabalpur_school/photo_gallery) in Jabalpur, India. This school provides a completely free education to 550 boys and girls, from ages 5 to 17. I have visited the school three times since 2012, and I can vouch for the worthiness of the cause.

Alternatively consider making donations to your favorite open-source developers and projects.
