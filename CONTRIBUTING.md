## Technical Setup

Recommended reading:
- The [Intellij SDK docs](https://www.jetbrains.org/intellij/sdk/docs/intro/welcome.html)
- The Rust plugin's [architecture doc](https://github.com/intellij-rust/intellij-rust/blob/master/ARCHITECTURE.md). I was heavily influenced by how the Rust plugin team structured their project, especially their integration test base classes.
- A [series of blog posts](https://kobzol.github.io/) about a newcomer's experience contributing features to the Rust plugin.  

Prefer watching videos? Dillon Kearns and Aaron VonderHaar did a [live stream where they built an intellij-elm feature from scratch](https://www.youtube.com/watch?v=8ihh7HNXlaU) using Test-Driven Development.

## The Psi Tree

IntelliJ maintains a bi-directional link between the Elm source text on disk and a parsed syntax tree representation. We define the Elm syntax rules in [ElmParser.bnf](src/main/grammars/ElmParser.bnf), and we use [GrammarKit](https://github.com/JetBrains/Grammar-Kit) to generate a parser from the `.bnf` file. For each public rule in the `.bnf`, there is a corresponding class by the same name with an `Elm` prefix. For instance, the syntax rule `ImportClause ::= IMPORT UpperCaseQID [AsClause] [ExposingList]` corresponds to the [ElmImportClause](src/main/kotlin/org/elm/lang/core/psi/elements/ElmImportClause.kt) class. Normally GrammarKit generates these classes along with the parser, but for this project, we wrote and maintain those classes by hand.  

If you are curious about how the syntax tree is structured, I recommend playing around with the [PsiViewer](https://plugins.jetbrains.com/plugin/227-psiviewer) plugin on your Elm code.

## Testing

To keep this project maintainable, it's important to add test coverage for each new feature. There are over a thousand tests in the project right now, so you should be able to find a similar example to base your tests on. If there is no comparable feature, check to see if [intellij-rust](https://github.com/intellij-rust/intellij-rust) has implemented a similar feature. Their plugin is very mature, has a lot of features, and has excellent tests.
  
The only place where it is acceptable not to have a test is for UI code (although if someone has a good pattern for doing that, too, I would be interested).

#### Test Workflow Tip

You can speed-up your development/test workflow by ensuring that IntelliJ runs the tests directly without invoking Gradle. By default, IntelliJ will delegate the running of tests to Gradle. This is more correct, but unnecessary for typical development. Fix it by doing the following:
 
 1. Open IntelliJ Settings
 2. Navigate to Build, Execution, Deployment -> Build Tools -> Gradle
 3. Find the dropdown box labeled "Run tests using: Gradle" and change it to "IntelliJ IDEA"
 
(The only time where you would want to have Gradle run the tests is if you change the parser or lexer definition, in which case we would need Gradle to re-build the parser and lexer prior to running the tests.)

## Questions

Have a question? Contact me (`klazuka`) on the [Elm Slack](https://elmlang.herokuapp.com/) or create a Github issue. 
