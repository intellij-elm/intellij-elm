(**) : number -> number -> number
(**) = (^)

infixl 0 **

f1 = 2 ** 3
f2 = (**) 2 3

g1 = -f1
g2 = 8 - f1

h1 = "hello" :: ["world"]
