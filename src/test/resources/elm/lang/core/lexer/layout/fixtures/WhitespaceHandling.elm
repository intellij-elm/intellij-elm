-- Virtual end decl tokens should be placed at the end of line, AFTER the newline.
-- This is important for ElmParameterInfoHandler and other things that need to look
-- at the context of the caret when the caret is on a space character.

x = f 
y = f   
z = f -- a comment that belongs to the value decl
-- this should stand on its own
a = f 0 
