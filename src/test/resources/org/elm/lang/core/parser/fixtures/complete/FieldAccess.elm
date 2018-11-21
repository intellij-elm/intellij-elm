type alias PersonExtra = { name: {first: String, last: String } }

person = PersonExtra { first = "George", last = "Harrison" }

f = person.name

g = person.name.last

h = .name person