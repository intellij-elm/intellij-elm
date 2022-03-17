type alias Person =
    { name : String
    , age : Int
    , occupation : (String, String)
    }

type alias Updater = Person -> Person
