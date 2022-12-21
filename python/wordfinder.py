#!/Users/rkilgore/.venv/bin/python

import os
import pickle
import sys
import time
from functools import reduce
from typing import Any, Dict, List, Optional, Set

DEBUG = False

from trie import Trie, TrieNode


class StaticData:
  def __init__(
      self, dictwords: Any,
      necessary_letters: str,
      maxprefix: int, maxpostfix: int,
      use_trie: bool,
  ):
    self.dictwords = dictwords
    self.necessary_letters = necessary_letters
    self.maxprefix = maxprefix
    self.maxpostfix = maxpostfix
    self.use_trie = use_trie


def debug_log(log: str) -> None:
  global DEBUG
  if DEBUG:
    print(log)

def to_search(letters: str) -> str:
  to_srch = ""
  for ch in letters:
    if ch not in to_srch:
      to_srch += ch
  return to_srch

def has_necessary(word: str, static: StaticData) -> bool:
  for ch in static.necessary_letters:
    if ch not in word:
        return False
  return True


def add_word(word: str, dot_vals: str, words: Dict[str, str], label: str = "") -> None:
  debug_log(f"{label}add_word({word}, {dot_vals})")
  if word not in words or len(words[word]) > len(dot_vals):
    words[word] = dot_vals


def recurse(
    depth: int,
    static: StaticData,
    sofar: str, curnode: Optional[TrieNode],
    cur_prefix_len: int, cur_postfix_len: int,
    letters: str, template: str, templstarted: bool,
    in_dot_vals: str = "",
) -> Dict[str, str]:

  log_indent = '    ' * depth
  debug_log(
      f"{log_indent}recurse sofar={sofar} prefix={cur_prefix_len} postfix={cur_postfix_len} "
      f"letters='{letters}' template='{template}'")
  noLetters = len(letters) == 0
  emptyTemplate = len(template) == 0
  nextTemplateIsLetter = not emptyTemplate and template[0] != '.'
  cantAddPostfix = cur_postfix_len == static.maxpostfix
  if (
      (noLetters and not nextTemplateIsLetter) or
      (emptyTemplate and cantAddPostfix)
  ):
    debug_log(f"{log_indent}  short return: sofar={sofar} letters={letters} templ={template}")
    return {}

  words = {}

  if len(template) > 0:
    # try adding ch from letters to prefix before template
    remaining_letters_required = reduce(lambda tot, ch: tot + (1 if ch == '.' or ch.isupper() else 0), [0] + list(template))
    if not templstarted and cur_prefix_len < static.maxprefix and len(letters) > remaining_letters_required:
      debug_log(f"{log_indent}  prefix: remaining = {remaining_letters_required} for sofar={sofar} letters='{letters}' templ='{template}'")
      for ch in to_search(letters):
          newletters = letters.replace(ch, "", 1)
          is_dot = ch == '.'
          chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
          for xch in chars:
            nextsofar = sofar + xch
            next_dot_vals = in_dot_vals + (xch if is_dot else "")
            nextnode = isprefix(nextsofar, static.dictwords, curnode, static.use_trie)
            debug_log(f"{log_indent}  isprefix({nextsofar}, {curnode.short_str() if curnode else None}) -> {nextnode.short_str() if nextnode else None}")
            if nextnode is not None:
              if nextnode.isword and len(template) == 0 and has_necessary(nextsofar, static):
                add_word(nextsofar, next_dot_vals, words, f"{log_indent}  {xch} -> prefix:") 

              rwords = recurse(depth+1, static, nextsofar, nextnode, cur_prefix_len + 1, cur_postfix_len,
                               newletters, template, False, next_dot_vals)
              for word, dot_vals in rwords.items():
                add_word(word, dot_vals, words, f"{log_indent}  from recurse after {xch} -> prefix:")

  # template letter = '.' - try each letter from letters as match
  if len(template) > 0 and template[0] == '.':
    for ch in to_search(letters):
      newletters = letters.replace(ch, "", 1)
      newtempl = template[1:]
      is_dot = ch == '.'
      chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
      for xch in chars:
        debug_log(f"{log_indent}  use letter '{xch}' from: sofar={sofar} letters='{letters}' templ='{template}'")
        nextsofar = sofar + xch
        next_dot_vals = in_dot_vals + (xch if is_dot else "")
        nextnode = isprefix(nextsofar, static.dictwords, curnode, static.use_trie)
        debug_log(f"{log_indent}  isprefix({nextsofar}, {curnode.short_str() if curnode else None}) -> {nextnode.short_str() if nextnode else None}")
        if nextnode is not None:
          if nextnode.isword and len(newtempl) == 0 and has_necessary(nextsofar, static):
            add_word(nextsofar, next_dot_vals, words, f"{log_indent}  template dot {xch}:")

          rwords = recurse(depth+1, static, nextsofar, nextnode, cur_prefix_len, cur_postfix_len,
                           newletters, newtempl, True, next_dot_vals)
          for word, dot_vals in rwords.items():
            add_word(word, dot_vals, words, f"{log_indent}  from recurse after template dot {xch}:")

  # template letter ch != '.' - add ch.lower() and remove ch.lower() from input letters if ch.isupper()
  elif len(template) > 0:
    ch = template[0]
    debug_log(f"{log_indent}  take template '{ch}' from: sofar={sofar} letters='{letters}' templ='{template}'")
    if ch.isupper():
      ch = ch.lower()
      if not ch in letters:
        return words
      letters = letters.replace(ch, "", 1)
    nextsofar = sofar + ch
    newtempl = template[1:]
    nextnode = isprefix(nextsofar, static.dictwords, curnode, static.use_trie)
    debug_log(f"{log_indent}  isprefix({nextsofar}, {curnode.short_str() if curnode else None}) -> {nextnode.short_str() if nextnode else None}")
    if nextnode is not None:
      if nextnode.isword and len(newtempl) == 0 and has_necessary(nextsofar, static):
        add_word(nextsofar, in_dot_vals, words, f"{log_indent}  template letter {ch}:")

      rwords = recurse(depth+1, static, nextsofar, nextnode, cur_prefix_len, cur_postfix_len,
                       letters, newtempl, True, in_dot_vals)
      for word, dot_vals in rwords.items():
        add_word(word, dot_vals, words, f"{log_indent}  from recurse after template letter {ch}:")


  # at end, add letters for the postfix
  elif cur_postfix_len < static.maxpostfix:
    debug_log(f"{log_indent}  postfix: sofar={sofar} letters='{letters}' templ='{template}'")
    for ch in to_search(letters):
      newletters = letters.replace(ch, "", 1)
      is_dot = ch == '.'
      chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
      for xch in chars:
        nextsofar = sofar + xch
        next_dot_vals = in_dot_vals + (xch if is_dot else "")
        nextnode = isprefix(nextsofar, static.dictwords, curnode, static.use_trie)
        debug_log(f"{log_indent}  isprefix({nextsofar}, {curnode.short_str() if curnode else None}) -> {nextnode.short_str() if nextnode else None}")
        if nextnode is not None:
          if nextnode.isword and has_necessary(nextsofar, static):
            add_word(nextsofar, next_dot_vals, words, f"{log_indent}  {xch} -> postfix:")

          rwords = recurse(depth+1, static, nextsofar, nextnode, cur_prefix_len, cur_postfix_len + 1,
                           newletters, template, templstarted, next_dot_vals)
          for word, dot_vals in rwords.items():
            add_word(word, dot_vals, words, f"{log_indent}  from recurse after {xch} -> postfix:")

  return words


last_time = -1.0
def report_time(msg: str) -> float:
  now = time.process_time()
  global last_time
  if last_time >= 0.0:
    print(f"{msg}: elapsed = {now - last_time}")
  else:
    print(f"{msg}: time = {now}")
  last_time = now


def add_dictword(word: str, dictwords: Any, use_trie: bool) -> None:
  if use_trie:
    dictwords.insert(word)
  else:
    dictwords[word] = 1

def isprefix(prefix: str, dictwords: Any, node: Optional[TrieNode], use_trie: bool) -> Optional[TrieNode]:
  if use_trie:
    return node.isprefix(prefix[-1]) if node else dictwords.isprefix(prefix)
  else:
    isword = prefix in dictwords
    return TrieNode(prefix[-1], isword)

def necessary_chars(letters: str) -> str:
  necessary = ""
  for ch in letters:
    if ch != '.' and ch.isupper():
      necessary += ch.lower()
  return necessary


def load_dict(use_trie: bool) -> Any:
  bn = os.path.basename(sys.argv[0])
  pickle_filename = f"{os.environ.get('HOME')}/git/wordfinder/{bn}_words.pickle" 
  if use_trie and os.path.exists(pickle_filename):
    with open(pickle_filename, "rb") as picklefile:
      return pickle.load(picklefile)

  else:
    with open(f"{os.environ.get('HOME')}/git/wordfinder/scrabble_words.txt") as dictfile:
      lines = dictfile.readlines()
      if use_trie:
        dictwords = Trie()
      else:
        dictwords = {}

      for line in lines:
        w = line.lower().strip()
        if len(w) > 0:
          add_dictword(w, dictwords, use_trie)

      if use_trie:
        report_time(f"writing dictionary to {pickle_filename}")
        with open(pickle_filename, "wb") as picklefile:
          pickle.dump(dictwords, picklefile)
      else:
        report_time(f"dictwords contains {len(dictwords)} words")

      return dictwords


def find_words(static: StaticData, letters: str, template: str) -> None:
  words: Dict[str, str] = recurse(0, static, "", None, 0, 0, letters, template, False)

  ordered_keys = list(words.keys())
  ordered_keys.sort(key=lambda w: [len(w), len(words[w]), w])
  for word in ordered_keys:
    if word != template.lower():
      dot_vals: str = words[word]
      print((f"{dot_vals}: " if len(dot_vals) > 0 else "") + f"{word} {len(word)}")


# main
def main():
  argn = 1
  use_trie = False
  override_map = False
  while sys.argv[argn].startswith("-"):
    if sys.argv[argn] == "-t":
      use_trie = True
    elif sys.argv[argn] == "-d":
      global DEBUG
      DEBUG = True
    elif sys.argv[argn] == "-m":
      override_map = True
    argn += 1

  letters: str = sys.argv[argn]; argn += 1
  template: str = sys.argv[argn] if len(sys.argv) > argn else ""; argn += 1
  maxprefix: int = int(sys.argv[argn]) if len(sys.argv) > argn else 7; argn += 1
  maxpostfix: int = int(sys.argv[argn]) if len(sys.argv) > argn else 7; argn += 1

  # template can specify max prefix/postfix
  if len(template) > 0:
    if template[0].isnumeric():
      maxprefix = int(template[0])
      template = template[1:]
    if template[-1].isnumeric():
      maxpostfix = int(template[-1])
      template = template[:-1]

  necessary_letters = necessary_chars(letters)
  letters = letters.lower()

  if not override_map:
    maxdots = 3
    for i, arg in enumerate([letters, template]):
      dotcount = 0
      for ch in arg:
        if ch == '.':
          dotcount += 1
        elif not ch.isalpha():
          print(f"string args must be all letters and '.' chars: {arg}")
          sys.exit(1)
      if i == 0 and dotcount > maxdots:
        print(f"using trie because {dotcount} dots (>{maxdots}) in letters")
        use_trie = True

  report_time("starting")
  dictwords = load_dict(use_trie)

  static: StaticData = StaticData(dictwords, necessary_letters, maxprefix, maxpostfix, use_trie)
  report_time("dictwords loaded")
  find_words(static, letters, template)
  dictwords = None
  report_time("done")


main()
