type Foo = Foo
type alias Normal = ()
type   alias Spacey = ()

-- `alias` can be used in other contexts (https://github.com/intellij-elm/intellij-elm/issues/378)
x = { alias = "secret" }