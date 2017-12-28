
Provides support for the [Elm](http://elm-lang.org) programming language in IntelliJ and WebStorm.


# Features 

* Code completion
* Go to declaration
* Go to symbol
* Find usages
* Rename refactoring
* Syntax highlighting
* Mark unresolvable references as errors
* 'Import' quick fix for unresolved references
* Structure view
* and more...


# Why Another Elm Plugin for IntelliJ

Prior to creating this plugin, I used to contribute to a very good [Elm plugin](https://github.com/durkiewicz/elm-plugin) for IntelliJ. But development of the plugin has stagnated, and it has some architectural problems. So I decided to create a new plugin based on the existing lexer and parser. My goals were the following:

* written in Kotlin instead of Java 8
* implement references the right way so that Find Usages can be enabled
* test-driven development of most plugin features
* hand-written Psi classes


# Development

There is still a lot of work left to do, but PRs are welcome. To build the plugin you will need to do the following:
- setup the IntelliJ Plugin SDK toolchain [link](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html)
- install the [GrammarKit plugin](https://github.com/JetBrains/Grammar-Kit)
- generate the lexer by pressing cmd-shift-g on `ElmLexer.flex`
- generate the parser by pressing cmd-shift-g on `ElmParser.bnf`


# Attribution

This project heavily leverages the open-source work of other IntelliJ plugin developers:

* Lexer and Parser from Kamil Durkiewicz's [elm-intellij-plugin](https://github.com/durkiewicz/elm-plugin).
* Offside-rule Lexer from Alexander Kiel's [idea-haskell](https://github.com/alexanderkiel/idea-haskell) plugin.
* Code organization and testing utils based on the [Rust plugin](https://github.com/intellij-rust/intellij-rust) for IntelliJ. 

Thank you Kamil, Alexander, and the Rust plugin developers.


# Donation

If this plugin improves your Elm development experience, and you want to give back, please consider making a [donation](http://www.brahmrishiyoga.org/donate) (tax-deductible in the US) to the [Narmada Vidya Peeth school](http://www.brahmrishiyoga.org/jabalpur_school/photo_gallery) in Jabalpur, India. This school provides a completely free education to 550 boys and girls, from ages 5 to 17. I have visited the school twice in the past 6 years, and I can vouch for the worthiness of the cause.

Alternatively consider making donations to your favorite open-source developers and projects.
