## Technical Setup

The architecture is inspired by https://github.com/intellij-rust/intellij-rust/blob/master/ARCHITECTURE.md

Check out [the `intellij-rust` repo](https://github.com/intellij-rust/intellij-rust) in general for how the `intellij-elm` testing is set up and how the project is structured. Also good for examples of how to use the Intellij SDK. The people who built this originally are now JetBrains employees. There are also [some helpful blog posts](https://kobzol.github.io/) written by an intellij-rust maintainer.

The [Intellij SDK docs](https://www.jetbrains.org/intellij/sdk/docs/intro/welcome.html) are a great reference, both for getting started and for more advanced features.

Dillon Kearns and Aaron VonderHaar did a [live stream where they built an intellij-elm feature from scratch](https://www.youtube.com/watch?v=8ihh7HNXlaU), using Test-Driven Development.

## Testing
To keep this project maintainable, it's important to add test coverage for all new Intentions, Inspections, etc.
