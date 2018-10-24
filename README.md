[![CircleCI](https://circleci.com/gh/klazuka/intellij-elm.svg?style=svg)](https://circleci.com/gh/klazuka/intellij-elm)

Provides support for the [Elm](http://elm-lang.org) programming language in IntelliJ and WebStorm.


# Features 

* Code completion
* Go to declaration
* Go to symbol
* Find usages
* Rename refactoring
* Code folding
* Syntax highlighting
* Mark unresolvable references as errors
* 'Import' quick fix for unresolved references
* Structure view
* and more...

Watch the [feature demo videos](https://klazuka.github.io/intellij-elm/) to learn more.


# Installation

You can install the plugin from within the JetBrains IDE by going to `Settings -> Plugins` and then searching for "Elm". Then click the `Search in repositories` link.

If, however, you need to install a specific version of the plugin, you can get it from [the releases page](https://github.com/klazuka/intellij-elm/releases).


# Attaching Elm JSON files

One major change since earlier versions of this plugin is that you must now attach your Elm project's JSON manifest
file (either `elm.json` for Elm 0.19 or `elm-package.json` for 0.18). The plugin will try to find this JSON file 
on its own, but you may need to manually attach it if the magic fails.


# Integrating with elm-format

See the elm-format [integration instructions](https://github.com/klazuka/intellij-elm/blob/master/docs/elm-format/setup.md).


# Creating a new Elm project

The plugin does not currently provide any new-project templates. Until that work is done, the easiest way to create a new
Elm project is to do everything from the command line using the standard Elm tools. If you're using Elm 0.19, this means:

1. create a new directory for your project
1. run `elm init`
1. open that directory in IntelliJ (or WebStorm, etc.)
1. select the `src` directory in the IntelliJ project window
1. select `File -> New -> Elm Module`, call the new file `Main`
1. follow the prompt to setup the Elm toolchain by specifying the path to the directory which contains the Elm compiler
1. follow the prompt to attach your project's `elm.json` file.
1. you're all set to write some code

Please note that the IntelliJ plugin does not install any of your project dependencies for you. It assumes that you have
already installed them via the standard Elm command line tools.


# Attribution

This project heavily leverages the open-source work of other IntelliJ plugin developers:

* Code organization and testing utils based on the [Rust plugin](https://github.com/intellij-rust/intellij-rust) for IntelliJ. 
* Lexer and Parser originally from Kamil Durkiewicz's [elm-intellij-plugin](https://github.com/durkiewicz/elm-plugin).
* Offside-rule Lexer originally based on code from Alexander Kiel's [idea-haskell](https://github.com/alexanderkiel/idea-haskell) plugin.

Thank you Kamil, Alexander, and the Rust plugin developers.


# Donation

If this plugin improves your Elm development experience, and you want to give back, please consider making a [donation](http://www.brahmrishiyoga.org/donate) (tax-deductible in the US) to the [Narmada Vidya Peeth school](http://www.brahmrishiyoga.org/jabalpur_school/photo_gallery) in Jabalpur, India. This school provides a completely free education to 550 boys and girls, from ages 5 to 17. I have visited the school three times since 2012, and I can vouch for the worthiness of the cause.

Alternatively consider making donations to your favorite open-source developers and projects.
