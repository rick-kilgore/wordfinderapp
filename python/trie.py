from typing import List, Optional


class TrieNode:
  def __init__(self, ch: chr, isword: bool = False):
    self.ch: chr = ch
    self.children: List[TrieNode] = []
    self.isword: bool = isword

  def __str__(self) -> str:
    return self.strhelper(0).rstrip()

  def strhelper(self, depth: int) -> str:
    prefix = "  " * depth
    s = ""
    if self.children:
      for child in self.children:
        s += f"{prefix}{child.ch}: isword = {child.isword}\n"
        childstr = child.strhelper(depth+1)
        if len(childstr) > 0:
          s += childstr
    return s

  def short_str(self) -> str:
    return str(self.ch) + f": isword={self.isword}"

  def add(self, ch):
    if not self.children:
      child = TrieNode(ch)
      self.children = [child]
      return child

    for child in self.children:
      if child.ch == ch:
        return child

    child = TrieNode(ch)
    self.children.append(child)
    return child

  def isprefix(self, word) -> Optional:
    node = self
    for ch in word:
      if not node.children:
        return None

      found = False
      for child in node.children:
        if child.ch == ch:
          node = child
          found = True
          break

      if not found:
        return None

    return node


class Trie:
  def __init__(self):
    self.trie = TrieNode('*')

  def __str__(self):
    return str(self.trie)

  def insert(self, word):
    node = self.trie
    for ch in word:
      node = node.add(ch)
    node.isword = True

  def isprefix(self, word) -> Optional[TrieNode]:
    return self.trie.isprefix(word)

