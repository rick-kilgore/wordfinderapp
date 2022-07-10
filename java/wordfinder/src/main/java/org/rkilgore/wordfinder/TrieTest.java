package org.rkilgore.wordfinder;

public class TrieTest {

  public static void main(String[] args) {
      TrieNode trie = new TrieNode('*');
      for (String word : new String[] {"hello", "string", "strings"}) {
        System.out.printf("insert('%s')\n", word);
        trie.insert(word);
      }
      for (String candidate : new String[] {"hello", "hell", "str", "string"}) {
        System.out.printf("isword('%s') = %s\n", candidate, trie.isPrefix(candidate));
      }
      System.out.println("\ntrie.insert('HELL')");
      trie.insert("HELL");

      for (String candidate : new String[] { "hello", "heLL", "STR", "string" }) {
        System.out.printf("isword('%s') = %s\n", candidate, trie.isPrefix(candidate));
      }
  }
}
