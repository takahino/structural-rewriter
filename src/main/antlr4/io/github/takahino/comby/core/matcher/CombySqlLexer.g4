lexer grammar CombySqlLexer;

STRING_SQUOTE : '\'' (~['\r\n] | '\'\'')* '\'' ;
STRING_DQUOTE : '"'  (~["\r\n] | '""')*  '"'  ;
LINE_COMMENT  : '--' ~[\r\n]*                  ;
BLOCK_COMMENT : '/*' .*? '*/'                  ;
OPEN  : [({[] ;
CLOSE : [)}\]] ;
ANY   : .      ;
