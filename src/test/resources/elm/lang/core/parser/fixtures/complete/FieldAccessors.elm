f1 x = x.bar
f2 x = x.bar.baz
f3 x = Foo.bar.baz

p1 x = (foo x).bar
p2 x = (foo x).bar.baz

r1 = {x=2}.x
r2 = {x={y=2}}.x.y
