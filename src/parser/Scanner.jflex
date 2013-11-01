package parser;


import java_cup.runtime.SymbolFactory;
%%
%cup
%class Scanner
%{
	public Scanner(java.io.InputStream r, SymbolFactory sf){
		this(r);
		this.sf=sf;
	}
	private SymbolFactory sf;
%}
%eofval{
    return sf.newSymbol("EOF",sym.EOF);
%eofval}

%%
";" { return sf.newSymbol("Semicolon",sym.SEMI, new String(yytext())); }
"A" { return sf.newSymbol("For All",sym.FORALL, new String(yytext())); }
"E" { return sf.newSymbol("Exist",sym.EXIST, new String(yytext())); }
"P" { return sf.newSymbol("Permission",sym.PERMISSION, new String(yytext()) ); }
"O" { return sf.newSymbol("Obligation",sym.OBLIGATION, new String(yytext())); }
"R" { return sf.newSymbol("Recovery",sym.RECOVERY, new String(yytext())); }
"(" { return sf.newSymbol("Left Bracket",sym.LPAREN, new String(yytext())); }
")" { return sf.newSymbol("Right Bracket",sym.RPAREN, new String(yytext())); }
[a-z][_a-z0-9]* { return sf.newSymbol("identifier",sym.ID, new String(yytext())); }
"X" { return sf.newSymbol("Right Bracket",sym.NEXT, new String(yytext())); }
"U" { return sf.newSymbol("Right Bracket",sym.UNTIL, new String(yytext()) ); }
"T" { return sf.newSymbol("Right Bracket",sym.TRUE, new String(yytext()) ); }
"!" { return sf.newSymbol("Right Bracket",sym.NEG, new String(yytext()) ); }
"->" { return sf.newSymbol("Right Bracket",sym.IMPLIES, new String(yytext())); }
"~>" { return sf.newSymbol("Right Bracket",sym.IMPLIESTEMP, new String(yytext())); }
[ \t\r\n\f] { /* ignore white space. */ }
. { System.err.println("Illegal character: "+yytext()); }

