infix right 5 (**) = power

power a b = List.product (List.repeat b a)

f1 = 2 ** 3
f2 = (**) 2 3

g1 = -f1
g2 = 8 - f1
g3 = (-f1 + 7)

h1 = "hello" :: ["world"]
