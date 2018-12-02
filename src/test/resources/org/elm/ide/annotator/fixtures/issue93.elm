import Issue93Module as I

{-
When importing a module using an alias, as we do here, the original module
name can no longer be used. And since this might otherwise be confusing
to the user, we show a nice error message.
-}

type alias Foo =
    { a : <error descr="Unresolved reference 'A'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.A</error>
    , b : <error descr="Unresolved reference 'B'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.B ()</error>
    }

f a b = <error descr="Unresolved reference 'x'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.x</error>

g = <error descr="Unresolved reference 'ConstructorA'. Module 'Issue93Module' is imported as 'I' and so you must use the alias here.">Issue93Module.ConstructorA</error>

ok = I.x

badRefRegardlessOfAlias = <error descr="Unresolved reference 'bogus'">Issue93Module.bogus</error>