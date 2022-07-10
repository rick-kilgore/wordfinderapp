package org.rkilgore.wordfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TrieNode {
  public TrieNode(char ch) {
    this.ch = ch;
    this.isword = false;
    this._children = new HashMap<Character, TrieNode>();
  }

  public TrieNode(Scanner dictFileScanner) {
    this('*');
    this.readFromFile(dictFileScanner);
  }

  public TrieNode(String dictFilename) {
    this('*');
    try {
      Scanner scanner = new Scanner(new File(dictFilename));
      this.readFromFile(scanner);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public TrieNode isPrefix(String prefix) {
    TrieNode node = this;
    char[] chars = prefix.toLowerCase().toCharArray();
    for (char ch : chars) {
      node = node._children.get(ch);
      if (node == null) {
        return null;
      }
    }
    return node;
  }

  private void readFromFile(Scanner scanner) {
    try {
      while (scanner.hasNextLine()) {
        String word = scanner.nextLine();
        this.insert(word);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void insert(String word) {
    TrieNode node = this;
    char[] chars = word.toLowerCase().toCharArray();
    for (char ch : chars) {
      node = node.getOrCreateChild(ch);
    }
    node.isword = true;
  }

  private TrieNode getOrCreateChild(char ch) {
    TrieNode child = this._children.get(ch);
    if (child == null) {
      child = new TrieNode(ch);
      this._children.put(ch, child);
    }
    return child;
  }

  private char ch;
  public boolean isword;
  private Map<Character, TrieNode> _children;
}
