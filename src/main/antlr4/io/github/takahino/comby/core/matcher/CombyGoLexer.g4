lexer grammar CombyGoLexer;

STRING_BACKTICK : '`'  ~[`]*  '`'            ;
STRING_DQUOTE   : '"'  (ESC | ~["\\\r\n])* '"'  ;
STRING_SQUOTE   : '\'' (ESC | ~['\\\r\n])* '\'' ;
LINE_COMMENT    : '//' ~[\r\n]*               ;
BLOCK_COMMENT   : '/*' .*? '*/'               ;
OPEN  : [({[] ;
CLOSE : [)}\]] ;
ANY   : .      ;

fragment ESC : '\\' . ;
