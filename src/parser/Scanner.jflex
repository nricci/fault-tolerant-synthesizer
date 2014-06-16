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

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment}}

TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}

%eofval{
    return sf.newSymbol("EOF",sym.EOF);
%eofval}



%%
"--Interface" { return sf.newSymbol("Interface",sym.INTERFACE, new String(yytext())); }
"--InitialState" { return sf.newSymbol("INITIALSTATE",sym.INITIALSTATE, new String(yytext())); }
"--Specification" { return sf.newSymbol("Specification",sym.SPECIFICATION, new String(yytext())); }
"," { return sf.newSymbol("Semicolon",sym.COMMA, new String(yytext())); }
";" { return sf.newSymbol("Semicolon",sym.SEMI, new String(yytext())); }
"A" { return sf.newSymbol("For All",sym.FORALL, new String(yytext())); }
"E" { return sf.newSymbol("Exist",sym.EXIST, new String(yytext())); }
"P" { return sf.newSymbol("Permission",sym.PERMISSION, new String(yytext()) ); }
"O" { return sf.newSymbol("Obligation",sym.OBLIGATION, new String(yytext())); }
"F" { return sf.newSymbol("Future",sym.FUTURE, new String(yytext())); }
"G" { return sf.newSymbol("Globally",sym.GLOBALLY, new String(yytext())); }
"(" { return sf.newSymbol("Left Bracket",sym.LPAREN, new String(yytext())); }
")" { return sf.newSymbol("Right Bracket",sym.RPAREN, new String(yytext())); }
[a-z][_a-z0-9]* { return sf.newSymbol("identifier",sym.ID, new String(yytext())); }
"X" { return sf.newSymbol("Next",sym.NEXT, new String(yytext())); }
"U" { return sf.newSymbol("Until",sym.UNTIL, new String(yytext()) ); }
"W" { return sf.newSymbol("WeakUntil",sym.WEAKUNTIL, new String(yytext()) ); }
"T" { return sf.newSymbol("True Value",sym.TRUE, new String(yytext()) ); }
"!" { return sf.newSymbol("Negation",sym.NEG, new String(yytext()) ); }
"->" { return sf.newSymbol("Implication",sym.IMPLIES, new String(yytext())); }
"<->" { return sf.newSymbol("Equivalence",sym.EQUIVALENCE, new String(yytext())); }
"&&" { return sf.newSymbol("Logic AND",sym.AND, new String(yytext())); }
"||" { return sf.newSymbol("Logic OR",sym.OR, new String(yytext())); }
  /* comments */
{Comment}                      { /* ignore */ }
[ \t\r\n\f] { /* ignore white space. */ }
. { System.err.println("Illegal character: "+yytext()); }

/*"R" { return sf.newSymbol("Recovery",sym.RECOVERY, new String(yytext())); }*/
/*"~>" { return sf.newSymbol("Right Bracket",sym.IMPLIESTEMP, new String(yytext())); }*/
