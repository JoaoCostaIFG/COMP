f = open("input") // opens 'input' file for reading

List output;
int par = 0;
bool invar = false;
bool innum = false;
char c;
while ((c = f.getChar()) != EOF) { // gets the next char of the file
  if (invar && c not match [0-9A-Za-z]) {
    output.append(VAR);
    invar = false;
  }
  else if (innum && c not match [0-9]) {
    output.append(INT);
    innum = false;
  }

  switch (c) {
    case '(':
      ++par;
      output.append(LPAR);
      break;
    case ')':
      if (par <= 0) die("unmatched RPAR");
      --par;
      output.append(RPAR);
      break;
    case '*':
      output.append(MUL);
      break;
    case '\':
      output.append(DIV);
      break;
    case '+':
      output.append(PLUS);
      break;
    case '-':
      output.append(SUB);
      break;
    case '=':
      output.append(EQ);
      break;
    case ';':
      output.append(SMICOLON);
      break;
    case [0-9]:
      if (!invar) {
        innum = true;
      }
      break;
    case [A-Za-z]:
      invar = true;
      break;
    case [\n\r\s\t]:
      // skip the symbol: "\n", "\r", " ", "\t"
      break;
    default:
      die("unkown token");
      break;
  }
}

if (invar) {
  output.append(VAR);
}
else if (innum) {
  output.append(INT);
}

if (par > 0) die("unmatched LPAR");

print(output); // show output on screen
