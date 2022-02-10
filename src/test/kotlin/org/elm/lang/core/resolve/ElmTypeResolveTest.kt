package org.elm.lang.core.resolve

import org.junit.Test


/**
 * Tests related to the namespace of Types
 */
class ElmTypeResolveTest: ElmResolveTestBase() {


    @Test
    fun `test union type ref`() = checkByCode(
"""
type Page = Home
     --X

title : Page -> String
        --^
""")


    @Test
    fun `test union type ref from module exposing list`() = checkByCode(
"""
module Main exposing (Page)
                      --^

type Page = Home
     --X
""")


    @Test
    fun `test union constructor ref`() = checkByCode(
"""
type Page = Home
            --X

defaultPage = Home
              --^
""")


    @Test
    fun `test union constructor pattern matching`() = checkByCode(
"""
type Page = Home
            --X

title page =
    case page of
        Home -> "home"
        --^
""")


    @Test
    fun `test type alias ref from module exposing list`() = checkByCode(
"""
module Main exposing (Person)
                      --^
type alias Person = { name : String, age: Int }
           --X
""")


    @Test
    fun `test type alias ref in type annotation`() = checkByCode(
"""
type alias Person = { name : String, age: Int }
           --X

personToString : Person -> String
                 --^
""")


    @Test
    fun `test type alias record constructor ref`() = checkByCode(
"""
type alias Person = { name : String, age: Int }
           --X

defaultPerson = Person "George" 42
                --^
""")


    @Test
    fun `test parametric union type ref `() = checkByCode(
"""
type Page a = Home a
     --X

title : Page a -> String
        --^
""")


    @Test
    fun `test parametric type alias ref `() = checkByCode(
"""
type alias Person a = { name : String, extra : a }
           --X

title : Person a -> String
        --^
""")



    @Test
    fun `test union constructor ref should not resolve to a record constructor`() = checkByCode(
"""
type alias User = { name : String, age : Int }

foo user =
    case user of
        User -> "foo"
        --^unresolved
""")


    @Test
    fun `test variable in union type`() = checkByCode(
            """
type Page a = Home a
        --X      --^
""")


    @Test
    fun `test variable in a record type alias`() = checkByCode(
            """
type alias User details = { name : String, extra : details }
                --X                                --^
""")
}