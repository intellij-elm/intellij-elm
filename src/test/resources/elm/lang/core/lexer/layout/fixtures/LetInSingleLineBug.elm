-- ok
i = let x = 320 in x

-- invalid syntax, but still should not crash the layout lexer
-- https://github.com/klazuka/intellij-elm/issues/20#issuecomment-374843581
boom = [in, in, in]