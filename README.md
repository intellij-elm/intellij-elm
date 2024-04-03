# Elm Plugin for IntelliJ IDEs (WebStorm, IDEA, PyCharm, etc.)

## Install

You can install the plugin from within the JetBrains IDE by going to **Settings -> Plugins** and then searching for "Elm". After installing the plugin, restart the IDE and then [open your existing Elm project](docs/existing-project.md) or [create a new project](docs/new-project.md).


## Highlighted Features

- [Live Error Checking](docs/features/live-error-checking.md)
- [Generate JSON Decoders & Encoders](docs/features/generate-function-json.md)
- [Rename refactoring](docs/features/rename-refactoring.md)
- [Lamdera support](docs/features/lamdera.md)
- [elm-review support](docs/features/elm-review.md)


## Additional Features 

* [Type Inference](docs/features/type-inference.md)
* [Find Usages](docs/features/find-usages.md)
* [Run Tests](docs/features/elm-test.md) (elm-test)
* [Reformat File](docs/features/elm-format.md) (elm-format)
* [Cleanup unused imports](docs/features/unused-imports.md)
* [Detect unused code](docs/features/unused-code.md)
* [Add Imports](docs/features/add-imports.md)
* [Quick Docs](docs/features/quick-docs.md)
* [Structure View & Quick Nav](docs/features/structure-view.md)
* [WebGL/GLSL support](docs/features/webgl.md)
* [Code Folding](docs/features/code-folding.md)
* [Manage the Exposing List](docs/features/exposure.md)
* Plus the usual IDE stuff: code completion, go-to-declaration, spell-checking, etc.

Want to see it in action? This [10 minute video](https://www.youtube.com/watch?v=CC2TdNuZztI) demonstrates many of the features and how they work together.

_Note_: if you have [node](https://nodejs.org) installed using [nvm](https://github.com/nvm-sh/nvm), please see the
related information [here](docs/nvm.md).

## FAQ

- Is this free?
    - Yes, the plugin itself is free and it works with the free version of IntelliJ.
- What versions of IntelliJ does this work with?
    - IntelliJ IDEA Ultimate
    - IntelliJ IDEA Community Edition (**free!**)
    - PyCharm Professional
    - PyCharm Community (**free!**)
    - WebStorm (amazing JS and CSS support)
    - and others (untested)

# Elm IntelliJ Plugin

![Build](https://github.com/intellij-elm/intellij-elm/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
Provides support for the Elm programming language
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Elm"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/intellij-elm/intellij-elm/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
