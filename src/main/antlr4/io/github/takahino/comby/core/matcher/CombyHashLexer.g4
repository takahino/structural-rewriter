lexer grammar CombyHashLexer;

STRING_DQUOTE : '"'  (ESC | ~["\\\r\n])* '"'  ;
STRING_SQUOTE : '\'' (ESC | ~['\\\r\n])* '\'' ;
LINE_COMMENT  : '#' ~[\r\n]*                  ;
OPEN  : [({[] ;
CLOSE : [)}\]] ;
ANY   : .      ;

fragment ESC : '\\' . ;
