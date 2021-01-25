/*!
 * jsPOS
 *
 * Copyright 2010, Percy Wegmann
 * Licensed under the GNU LGPLv3 license
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * https://code.google.com/archive/p/jspos/
 */

// eslint-disable-next-line max-classes-per-file
class LexerNode {
  /**
   * @param {string} string
   * @param {RegExp} regex
   * @param {RegExp[]} regexs
   */
  constructor(string, regex, regexs) {
    this.string = string;
    this.children = [];

    let childElements;
    if (string) {
      this.matches = string.match(regex);
      childElements = string.split(regex);
    }

    if (!this.matches) {
      this.matches = [];
      childElements = [string];
    }

    if (regexs.length > 0) {
      const nextRegex = regexs[0];
      const nextRegexes = regexs.slice(1);
      Array.from(childElements).forEach((element) => {
        this.children.push(new LexerNode(element, nextRegex, nextRegexes));
      });
    } else {
      this.children = childElements;
    }
  }

  fillArray(array) {
    Array.from(this.children).forEach((child, i) => {
      if (child.fillArray) {
        child.fillArray(array);
      } else if (/[^ \t\n\r]+/i.test(child)) {
        array.push(child);
      }

      if (i < this.matches.length) {
        const match = this.matches[i];
        if (/[^ \t\n\r]+/i.test(match)) {
          array.push(match);
        }
      }
    });
  }

  toString() {
    const array = [];
    this.fillArray(array);
    return array.toString();
  }
}

class Lexer {
  constructor() {
    // Split by numbers, then whitespace, then punctuation
    this.regexs = [/[0-9]*\.[0-9]+|[0-9]+/ig, /[ \t\n\r]+/ig, /[.,?!]/ig];
  }

  lex(string) {
    const array = [];
    const node = new LexerNode(string, this.regexs[0], this.regexs.slice(1));
    node.fillArray(array);
    return array;
  }
}

// var lexer = new Lexer();
// console.log(lexer.lex("I made $5.60 today in 1 hour of work.  The E.M.T.'s were on time, but only barely.").toString());
