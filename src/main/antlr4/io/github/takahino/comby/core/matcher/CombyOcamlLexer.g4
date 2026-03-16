lexer grammar CombyOcamlLexer;

STRING_DQUOTE : '"' (ESC | ~["\\\r\n])* '"' ;
OPEN_COMMENT  : '(*' -> pushMode(COMMENT_MODE) ;
OPEN  : [({[] ;
CLOSE : [)}\]] ;
ANY   : .      ;

fragment ESC : '\\' . ;

mode COMMENT_MODE;
NC_OPEN  : '(*' -> pushMode(COMMENT_MODE) ;
NC_CLOSE : '*)' -> popMode               ;
NC_ANY   : .                             ;
