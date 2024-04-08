Elm Plugin for JetBrains IDEs
=============================

<!--
Fix these badges once the plugin in uploaded...

![Build](https://github.com/intellij-elm/intellij-elm/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
-->

<!-- Plugin description -->
Provides support for the Elm programming language.
<!-- Plugin description end -->

Should work on IDEA (Community and Ultimate), WebStorm, PyCharm, RubyMine and more. If not please raise an issue.


## Install

First make sure to uninstall all other Elm plugins you may have installed (this requires a restart of the IDE).
Some have reported that having two Elm plugins installed results in the IDE not starting but showing a seemingly unreleated error
(if you have this problem, there are [ways to fix it](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000524244-Disable-Uninstall-plugin-without-launching-Idea)).

From within a JetBrains IDE, go to `Settings` -> `Plugins` -> `Marketplace` and search for "Elm".
After installing the plugin, restart the IDE and then [open your existing Elm project](docs/existing-project.md) or [create a new project](docs/new-project.md).

Alternatively you can install it manually by downloading a [release](https://github.com/intellij-elm/intellij-elm/releases) (or downloading the source and building it yourself) and
installing it with `Settings` -> `Plugins` -> `⚙️ (gear icon)` -> `Install plugin from disk...`

**NOTE**: if you have [node](https://nodejs.org) installed using [nvm](https://github.com/nvm-sh/nvm), make sure to read [our NVM setup guide](docs/nvm.md).


## Features

* [Live error checking](docs/features/live-error-checking.md)
* [Generate JSON encoder and decoder functions](docs/features/generate-function-json.md)
* [Rename refactoring](docs/features/rename-refactoring.md)
* [Lamdera support](docs/features/lamdera.md)
* [Support for `elm-review`](docs/features/elm-review.md)
* [Type inference and type checking](docs/features/type-inference.md)
* [Find usages](docs/features/find-usages.md)
* [Run tests](docs/features/elm-test.md) (elm-test)
* [Reformat code using `elm-fomrat`](docs/features/elm-format.md) (elm-format)
* [Cleanup unused imports](docs/features/unused-imports.md)
* [Detect unused code](docs/features/unused-code.md)
* ['Add Import' quick fix for unresolved references](docs/features/add-imports.md)
* [Quick Docs](docs/features/quick-docs.md)
* [Structure view and quick navigation](docs/features/structure-view.md)
* [WebGL/GLSL support](docs/features/webgl.md)
* [Code folding](docs/features/code-folding.md)
* [Manage exposing lists](docs/features/exposure.md)
* Detect and remove unused imports
* Go to symbol
* Go to declaration
* Syntax highlighting
* Spell checking
* and more...

Want to see it in action? This [10 minute video](https://www.youtube.com/watch?v=CC2TdNuZztI) demonstrates many of the features and how they work together.


## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.


## License

MIT licensed.


## Contributing

Yes, please! See [our guide](/docs/contributing.md) on this topic. 
