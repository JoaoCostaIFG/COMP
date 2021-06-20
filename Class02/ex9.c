Start() {
  if (token == "") return true;

  while (token.next() && Decl()) {}
  while (token.next() && Attrib()) {}

  return token == NULL;
}

Decl() {
  if (token == INT) {
    token = token.next();
    if (token == IDENT) {
      token = token.next();
      while (token == VIRG && token.next() == IDENT) {
        token = token.next().next();
      }
      if (token == PVIRG) {
        token = token.next();
        return true;
      }
    }
  }

  return false;
}

Attrib() {
  if (token == IDENT) {
    token = token.next();
    if (token == IGUAL) {
      token = token.next();
      if (token == CONST) {
        token = token.next();
      }
      else if (!Expr()) {
        return false
      }

      if (token == PVIRG) {
        token = token.next();
        return true;
      }
    }
  }

  return false;
}

Expr() {
  if (token == IDENT) {
    token = token.next();
    if (token == MULT) {
      token = token.next();
      if (token == IDENT) {
        token = token.next();
        return true;
      }
    }
  }

  return false;
}
