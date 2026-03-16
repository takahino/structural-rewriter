lexer grammar CombyPythonLexer;

// トリプルクォートを先に定義することで単一クォートより必ず優先される（最長一致）
STRING_TRIPLE_DQ : '"""' .*? '"""' ;
STRING_TRIPLE_SQ : '\'\'\'' .*? '\'\'\'' ;
STRING_DQUOTE    : '"'  (ESC | ~["\\\r\n])* '"'  ;
STRING_SQUOTE    : '\'' (ESC | ~['\\\r\n])* '\'' ;
LINE_COMMENT     : '#' ~[\r\n]*                  ;
OPEN  : [({[] ;
CLOSE : [)}\]] ;
ANY   : .      ;

fragment ESC : '\\' . ;
