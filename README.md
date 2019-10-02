# Elm Plugin for IntelliJ IDEs (WebStorm, IDEA, PyCharm, etc.)

[![CircleCI](https://circleci.com/gh/klazuka/intellij-elm.svg?style=svg)](https://circleci.com/gh/klazuka/intellij-elm)

## Install

You can install the plugin from within the JetBrains IDE by going to **Settings -> Plugins** and then searching for "Elm". After installing the plugin, restart the IDE and then [open your existing Elm project](#open-project) or [create a new project](#create-project).


## Highlighted Features

- [Live Error Checking](docs/features/live-error-checking.md)
- [Generate JSON Decoders & Encoders](docs/features/generate-function-json.md)
- [Rename refactoring](docs/features/rename-refactoring.md)


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
