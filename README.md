
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


# Support for Elm 0.19

When Elm 0.19 is released, this plugin will drop support for 0.18 and switch entirely over to 0.19. If you need Elm 0.19
support in the meantime, get it from [this gist](https://gist.github.com/klazuka/82b0f5288fa9944dba8fa027c476dfb1).

One major difference versus Elm 0.18 is that you now need to tell the Elm IntelliJ plugin the location of your `elm.json`
file. The plugin will try to find it on its own, but you may need to manually attach it if the magic fails.


# Integrating with elm-format

See the elm-format [integration instructions](https://github.com/klazuka/intellij-elm/blob/master/docs/elm-format/setup.md).


# Development

There is still a lot of work left to do, but PRs are welcome. To build the plugin you will need to do the following:
- setup the IntelliJ Plugin SDK toolchain [link](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html)
- install the [GrammarKit plugin](https://github.com/JetBrains/Grammar-Kit)
- generate the lexer by pressing cmd-shift-g on `ElmLexer.flex`
- generate the parser by pressing cmd-shift-g on `ElmParser.bnf`


# Attribution

This project heavily leverages the open-source work of other IntelliJ plugin developers:

* Code organization and testing utils based on the [Rust plugin](https://github.com/intellij-rust/intellij-rust) for IntelliJ. 
* Lexer and Parser originally from Kamil Durkiewicz's [elm-intellij-plugin](https://github.com/durkiewicz/elm-plugin).
* Offside-rule Lexer originally based on code from Alexander Kiel's [idea-haskell](https://github.com/alexanderkiel/idea-haskell) plugin.

Thank you Kamil, Alexander, and the Rust plugin developers.


# Donation

If this plugin improves your Elm development experience, and you want to give back, please consider making a [donation](http://www.brahmrishiyoga.org/donate) (tax-deductible in the US) to the [Narmada Vidya Peeth school](http://www.brahmrishiyoga.org/jabalpur_school/photo_gallery) in Jabalpur, India. This school provides a completely free education to 550 boys and girls, from ages 5 to 17. I have visited the school three times since 2012, and I can vouch for the worthiness of the cause.

Alternatively consider making donations to your favorite open-source developers and projects.
