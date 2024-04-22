How to help out with this project?
==================================

Recommended reading:

- The [IntelliJ SDK docs](https://www.jetbrains.org/intellij/sdk/docs/intro/welcome.html) for a broad understanding of the concepts involved in IntelliJ plugin building (including an [FAQ](https://plugins.jetbrains.com/docs/intellij/faq.html)).
- The Rust plugin's [architecture doc](https://github.com/intellij-rust/intellij-rust/blob/master/ARCHITECTURE.md). This project was heavily influenced by how the Rust plugin team structured their project, especially their integration test base classes.
- A [series of blog posts](https://kobzol.github.io/) about a newcomer's experience contributing features to the Rust plugin.  

Prefer watching videos? Dillon Kearns and Aaron VonderHaar did a [live stream where they built an intellij-elm feature from scratch](https://www.youtube.com/watch?v=8ihh7HNXlaU) using Test-Driven Development.


## Getting help

Much of what is done in this Elm plugin is already done in other plugins.

The Rust plugin has served as inspiration, but since JetBrains now sells a Rust specific IDE, this plugin is not getting as much love as it once did.
We are looking for good plugin projects to learn from besides the Rust plugin!

Much of the work maintaining this Elm plugin is related to staying current with the latest IntelliJ Platform versions as JetBrains is not afraid to break backward compatibility, deprecate APIs or mark API as internal.
To find out how to fix this it helps to look at the [IntelliJ Community](https://github.com/JetBrains/intellij-community) codebase, which is fully open source.
Alternatively we also get inspiration from other plugin projects' fixes by searching through their codebases.

JetBrains responds quite well when being reached out to, over on:
* their [Platform Slack for Plugin Developers](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360006494439--ANN-JetBrains-Platform-Slack-for-Plugin-Developers), and
* their [Plugin Development support forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development).


## The Psi Tree

IntelliJ maintains a bidirectional link between the Elm source text on disk and a parsed syntax tree representation. We define the Elm syntax rules in [ElmParser.bnf](../src/main/grammars/ElmParser.bnf), and we use [GrammarKit](https://github.com/JetBrains/Grammar-Kit) to generate a parser from the `.bnf` file.
For each public rule in the `.bnf`, there is a corresponding class by the same name with an `Elm` prefix. For instance, the syntax rule `ImportClause ::= IMPORT UpperCaseQID [AsClause] [ExposingList]` corresponds to the [ElmImportClause](../src/main/kotlin/org/elm/lang/core/psi/elements/ElmImportClause.kt) class.
Normally GrammarKit generates these classes along with the parser, but for this project, we wrote and maintain those classes by hand.  

If you are curious about how the syntax tree is structured, I recommend playing around with the [PsiViewer](https://plugins.jetbrains.com/plugin/227-psiviewer) plugin on your Elm code.


## Testing discipline

To keep this project maintainable, it's important to add test coverage for each new feature. There are over a thousand tests in the project right now, so you should be able to find a similar example to base your tests on.
If there is no comparable feature, check to see if [intellij-rust](https://github.com/intellij-rust/intellij-rust) has implemented a similar feature. Their plugin is very mature, has a lot of features, and has excellent tests.
  
The only place where it is acceptable not to have a test is for UI code (although if someone has a good pattern for doing that, too, I would be interested).

**TIP**:

You can speed up your development/test workflow by ensuring that IntelliJ runs the tests directly without invoking Gradle.
By default, IntelliJ will delegate the running of tests to Gradle. This is more correct, but unnecessary for typical development.
Over at:
 
`File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle`

There is a dropdown box labeled "Run tests using" and change it from "Gradle" to "IntelliJ IDEA".
 
The only time when you would want to have Gradle run the tests is if you change the parser or lexer definition, in which case we would need Gradle to re-build the parser and lexer prior to running the tests.

To run the tests the `elm`, `elm-format` and `elm-review` commands need to be on your `$PATH`.
Lamdera also needs to be installed, but I managed to simply link `elm` to `lamdera`, like with:

```bash
sudo ln -s /usr/local/bin/elm /usr/local/bin/lamdera
```


## Contributing to the project

How to make a PR to contribute to the code of this project is explained quite well by GitHub:

https://docs.github.com/en/get-started/exploring-projects-on-github/contributing-to-a-project


## Questions

Have a question? Try reaching out to us on [Elm Slack](https://elmlang.herokuapp.com/) or create a GitHub issue. 
