from trie import (
  TrieNode,
  Trie,
)

"""
node = TrieNode()
node.create('a')
node.at('a').create('t')

node.create('o')
node.at('o').create('k')
print(f"node:\n{str(node)}\n")
"""

trie = Trie()
for word in ["hello", "string", "strings"]:
  print(f"insert('{word}')")
  trie.insert(word)

print(f"\n{str(trie)}\n")

for candidate in [ 'hello', 'hell', 'str', 'string' ]:
  print(f"isword('{candidate}') = {trie.isword(candidate)}")

print("\ntrie.insert('hell')")
trie.insert("hell")

print(f"\n{str(trie)}\n")
for candidate in [ 'hello', 'hell', 'str', 'string' ]:
  print(f"isword('{candidate}') = {trie.isword(candidate)}")

