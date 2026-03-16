lexer grammar CombyNestedBlockLexer;

STRING_DQUOTE : '"'  (ESC | ~["\\\r\n])* '"'  ;
STRING_SQUOTE : '\'' (ESC | ~['\\\r\n])* '\'' ;
LINE_COMMENT  : '//' ~[\r\n]*                 ;
OPEN_BLOCK_COMMENT  : '/*' -> pushMode(BLOCK_COMMENT_MODE) ;
OPEN  : [({[] ;
CLOSE : [)}\]] ;
ANY   : .      ;

fragment ESC : '\\' . ;

mode BLOCK_COMMENT_MODE;
NBC_OPEN  : '/*' -> pushMode(BLOCK_COMMENT_MODE) ;
NBC_CLOSE : '*/' -> popMode                      ;
NBC_ANY   : .                                    ;
