package org.rkilgore.wordfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import lombok.AllArgsConstructor;


@AllArgsConstructor
class OverUnderResult {
  public final boolean check;
  public final int scoreToAdd;
}


/**
 * This class is currently NOT thread-safe because of the following
 * instance variables used for a single search:
 *    _requiredLetters
 *    _maxPrefix
 *    _maxPostfix
 *    _words.
 */
public class WordFinder {

  static class Tile {
    public static Tile OPEN = new Tile("open");
    public static Tile DLETTER = new Tile("DL", 2, 1);
    public static Tile TLETTER = new Tile("TL", 3, 1);
    public static Tile DWORD = new Tile("DW", 1, 2);
    public static Tile TWORD = new Tile("TW", 1, 3);
    public static Tile forLetter(char letter) {
      return new Tile(letter);
    }
    public static Tile openTileWithLetter(char letter) {
      Tile tile = forLetter(letter);
      tile.open = true;
      return tile;
    }

    Tile(String name) {
      this.name = name;
      this.open = true;
      this.letter = (char) 0;
      this.letterMult = 1;
      this.wordMult = 1;
    }

    Tile(char letter) {
      this("board letter " + letter);
      this.open = false;
      this.letter = letter;
    }

    Tile(String name, int letterMult, int wordMult) {
      this(name);
      this.letterMult = letterMult;
      this.wordMult = wordMult;
    }

    public String toString() {
      return this.name;
    }

    public boolean hasLetter() {
      return this.letter != (char) 0;
    }

    public String name;
    public boolean open;
    public char letter;
    public int letterMult;
    public int wordMult;
  }

  enum LetterPlacement {
    PREFIX,
    TEMPLATE,
    POSTFIX
  }

  static Map<Character, Integer> letterScores = new HashMap<>();

  public static void setupLetterScores(boolean wwf) {
    letterScores.put('a', 1);
    letterScores.put('b', wwf ? 4 : 3);
    letterScores.put('c', wwf ? 4 : 3);
    letterScores.put('d', 2);
    letterScores.put('e', 1);
    letterScores.put('f', 4);
    letterScores.put('g', 3);
    letterScores.put('h', wwf ? 3 : 4);
    letterScores.put('i', 1);
    letterScores.put('j', wwf ? 10 : 11);
    letterScores.put('k', 5);
    letterScores.put('l', wwf ? 2 : 1);
    letterScores.put('m', wwf ? 4 : 3);
    letterScores.put('n', 2);
    letterScores.put('o', 1);
    letterScores.put('p', wwf ? 4 : 3);
    letterScores.put('q', wwf ? 10 : 11);
    letterScores.put('r', 1);
    letterScores.put('s', 1);
    letterScores.put('t', 1);
    letterScores.put('u', wwf ? 2 : 3);
    letterScores.put('v', wwf ? 5 : 4);
    letterScores.put('w', 4);
    letterScores.put('x', 8);
    letterScores.put('y', wwf ? 3 : 4);
    letterScores.put('z', wwf ? 10 : 11);
  }


  public WordFinder(String dictfilename) {
    this._dict = new TrieNode(dictfilename);
  }

  public WordFinder(Scanner scanner) {
    this._dict = new TrieNode(scanner);
  }


  public Map<String, WordInfo> findWords(Mode mode, String letters, String template) {

    int maxPrefix = 7;
    int maxPostfix = 7;
    if (!template.isEmpty()) {
      if (Character.isDigit(template.charAt(0))) {
        maxPrefix = Character.digit(template.charAt(0), 10);
        System.out.println("maxPrefix = " + maxPrefix);
        template = template.substring(1);
      }
      int last = template.length() - 1;
      if (Character.isDigit(template.charAt(last))) {
        maxPostfix = Character.digit(template.charAt(last), 10);
        System.out.println("maxPostfix = " + maxPostfix);
        template = template.substring(0, last);
      }
    }

    List<Tile> tiles = new ArrayList<Tile>();
    for (char ch : template.toCharArray()) {
      if (ch == '.') {
        tiles.add(Tile.OPEN);
      } else if (ch == '-') {
        tiles.add(Tile.DLETTER);
      } else if (ch == '+') {
        tiles.add(Tile.TLETTER);
      } else if (ch == '#') {
        tiles.add(Tile.DWORD);
      } else if (ch == '!') {
        tiles.add(Tile.TWORD);
      } else if (Character.isUpperCase(ch)) {
        tiles.add(Tile.openTileWithLetter(Character.toLowerCase(ch)));
      } else if (Character.isLetter(ch) && mode == Mode.NORMAL) {
        tiles.add(Tile.forLetter(ch));
      } else if (Character.isLetter(ch) /* && mode == Mode.NORMAL */) {
        tiles.add(Tile.openTileWithLetter(Character.toLowerCase(ch)));
      } else {
        throw new RuntimeException(String.format("Unrecognized character in template: '%c'", ch));
      }
    }

    String requiredLetters = calcRequiredLetters(letters);
    letters = letters.toLowerCase();
    this._requiredLetters = requiredLetters;
    this._maxPrefix = maxPrefix;
    this._maxPostfix = maxPostfix;
    this._words = new HashMap<String, WordInfo>();
    this._mode = mode;
    this._fullTemplate = tiles;
    int charsBeforeFirstLetter = 0;
    while (tiles.size() > charsBeforeFirstLetter && !tiles.get(charsBeforeFirstLetter).hasLetter()) ++charsBeforeFirstLetter;
    this._templateFirstLetterIndex = charsBeforeFirstLetter;
    if (mode == Mode.NORMAL) {
      recurseNormal(0 /* depth */, "" /* sofar */, "" /* dotsSoFar */,
              0 /* scoreSoFar */, 1 /* wordMultSoFar */, this._dict /* nodeSoFar */,
              letters, tiles, false /* templateStarted */,
              0 /* curPrefixLen */, 0 /* curPostfixLen */);
    } else {
      recurseOverUnder(0 /* depth */, new OverUnder(), "" /* sofar */, "" /* dotsSoFar */,
              0 /* scoreSoFar */, 1 /* wordMultSoFar */, this._dict /* nodeSoFar */,
              letters, tiles, false /* templateStarted */,
              0 /* curPrefixLen */, 0 /* curPostfixLen */);
    }

    return this._words;
  }


  private void recurseNormal(
      int depth,
      String sofar,
      String dotsSoFar,
      int scoreSoFar,
      int wordMultSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      boolean templateStarted,
      int curPrefixLen,
      int curPostfixLen) {

    debugLog(String.format("%srecurseNormal sofar=%s dotsSoFar=%s letters=%s template=%s score=%d mult=%d prefix=%d postfix=%d "
                           + "templStarted=%s",
                           "  ".repeat(depth),
                           sofar, dotsSoFar, letters, template, scoreSoFar, wordMultSoFar, curPrefixLen, curPostfixLen,
                           String.valueOf(templateStarted)));

    // ---> check for terminate recursion
    if (shouldTerminate(depth, sofar, letters, template, curPostfixLen)) {
      return;
    }

    // ---> try adding from letters to prefix before template
    tryAddToPrefix(depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
                   letters, template, templateStarted, curPrefixLen);


    // ---> open tile two types - empty space or preset letter from letters
    if (!template.isEmpty()) {
      addFromTemplateNormal(depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
                            letters, template, curPrefixLen);

    } else {
      addToPostfixOrTerminate(depth, null, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
                              letters, curPrefixLen, curPostfixLen);
    }

  }

  private void recurseOverUnder(
      int depth,
      OverUnder overUnder,
      String sofar,
      String dotsSoFar,
      int scoreSoFar,
      int wordMultSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      boolean templateStarted,
      int curPrefixLen,
      int curPostfixLen) {

    debugLog(String.format("%srecurseOverUnder overUnder=%s sofar=%s dotsSoFar=%s letters=%s template=%s prefix=%d postfix=%d "
                           + "templStarted=%s",
                           "  ".repeat(depth),
                           overUnder, sofar, dotsSoFar, letters, template, curPrefixLen, curPostfixLen,
                           String.valueOf(templateStarted)));

    // ---> check for terminate recursion
    if (shouldTerminate(depth, sofar, letters, template, curPostfixLen)) {
      return;
    }

    // ---> try adding from letters to prefix before template
    tryAddToPrefix(depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
                   letters, template, templateStarted, curPrefixLen);


    if (!template.isEmpty()) {
        // ---> add letter from letters
        debugLog(String.format("%s  template tile - add from letters: sofar=%s letters=%s templ=%s",
                               "  ".repeat(depth),
                               sofar, letters, template));

        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar, overUnder,
            letters, template, curPrefixLen, curPostfixLen, LetterPlacement.TEMPLATE);

    } else {
      addToPostfixOrTerminate(depth, overUnder, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
                              letters, curPrefixLen, curPostfixLen);
    }
  }


  private boolean shouldTerminate(int depth, String sofar, String letters, List<Tile> template, int curPostfixLen) {
    boolean nextIsTemplateLetter = !template.isEmpty() && !template.get(0).open;
    boolean cantAddPostfix = curPostfixLen == this._maxPostfix;
    if ((letters.isEmpty() && !nextIsTemplateLetter) ||
        (template.isEmpty() && cantAddPostfix)) {
      debugLog(String.format("%s  terminate recursion: sofar=%s letters=%s templ=%s postfixLen=%d",
                             "  ".repeat(depth), sofar, letters, template, curPostfixLen));
      return true;
    }
    return false;
  }


  private void tryAddToPrefix(
      int depth,
      String sofar,
      String dotsSoFar,
      int scoreSoFar,
      int wordMultSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      boolean templateStarted,
      int curPrefixLen) {

    if (!template.isEmpty() && !templateStarted && curPrefixLen < this._maxPrefix) {
      int remainingLettersNeeded = this._mode == Mode.NORMAL
          ?  (int)template.stream().filter(tile -> tile.open).count()
          : this._templateFirstLetterIndex + 1;

      if (letters.length() > remainingLettersNeeded) {
        debugLog(String.format("%s  prefix add from letters: remainingNeeded=%d for sofar=%s letters=%s templ=%s",
                               "  ".repeat(depth),
                               remainingLettersNeeded,
                               sofar, letters, template));

        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar, OverUnder.empty,
            letters, template, curPrefixLen, 0, LetterPlacement.PREFIX);
      } else {
        debugLog(String.format("%s  no prefix add: remainingNeeded=%d letters=%s",
                               "  ".repeat(depth),
                               remainingLettersNeeded, letters));
      }
    }
  }


  private void addFromTemplateNormal(
      int depth,
      String sofar,
      String dotsSoFar,
      int scoreSoFar,
      int wordMultSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      int curPrefixLen) {

    assert !template.isEmpty();

    Tile nextTile = template.get(0);
    if (!nextTile.open) {
      // ---> template letter tile - add letter from template
      assert nextTile.hasLetter();
      debugLog(String.format("%s  add templ letter '%c': sofar=%s letters=%s templ=%s",
                             "  ".repeat(depth),
                             template.get(0).letter,
                             sofar, letters, template));

      addLetterFromTemplateAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
            letters, template, curPrefixLen, 0, false);

    } else if (nextTile.hasLetter()) {
      // ---> tile for which user has requested to put one of their letters in this spot
      debugLog(String.format("%s  designated letter tile - take '%c' from letters: sofar=%s letters=%s templ=%s",
                             "  ".repeat(depth),
                             nextTile.letter, sofar, letters, template));
      char ch = nextTile.letter;
      if (letters.indexOf(ch) == -1) {
        // can't fulfill this request
        debugLog(String.format("%s  recursion stopped - cannot fill template letter tile '%c': sofar=%s letters=%s templ=%s",
                               "  ".repeat(depth),
                               nextTile.letter, sofar, letters, template));
        return;
      }
      letters = letters.replaceFirst(String.valueOf(ch), "");
      addLetterFromTemplateAndRecurse(
          depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar,
          letters, template, curPrefixLen, 0, true);

    } else {
        // ---> open empty tile ([.-+#!])
        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar, OverUnder.empty,
            letters, template, curPrefixLen, 0, LetterPlacement.TEMPLATE);
    }
  }


  private void addToPostfixOrTerminate(
      int depth,
      OverUnder overUnder,
      String sofar,
      String dotsSoFar,
      int scoreSoFar,
      int wordMultSoFar,
      TrieNode nodeSoFar,
      String letters,
      int curPrefixLen,
      int curPostfixLen) {

    if (curPostfixLen < this._maxPostfix) {
      // ---> add letter to the postfix
      debugLog(String.format("%s  postfix add from letters: sofar=%s letters=%s",
                             "  ".repeat(depth),
                             sofar, letters));

      addLetterFromLettersAndRecurse(
          depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar, nodeSoFar, overUnder,
          letters, Collections.emptyList(), curPrefixLen, curPostfixLen, LetterPlacement.POSTFIX);

    } else {
      debugLog(String.format("%s  terminate - template and postfix exhausted: sofar=%s letters=%s postfixLen=%d",
                             "  ".repeat(depth),
                             letters.charAt(0),
                             sofar, letters, curPostfixLen));
    }
  }


  private void addLetterFromTemplateAndRecurse(
        int depth, String sofar, String dotsSoFar,
        int scoreSoFar, int wordMultSoFar,
        TrieNode nodeSoFar,
        String letters, List<Tile> template,
        int curPrefixLen, int curPostfixLen, boolean usingLetter) {
    Tile nextTile = template.get(0);
    char ch = nextTile.letter;
    TrieNode nextNode = nodeSoFar.isPrefix(Character.toString(ch));
    if (nextNode != null) {
      String nextsofar = sofar + ch;
      List<Tile> newtemplate = template.subList(1, template.size());
      int scoreAdd = usingLetter ? (letterScores.get(ch) * template.get(0).letterMult) : letterScores.get(ch);
      int multMult = usingLetter ? template.get(0).wordMult : 1;
      // debugLog(String.format("%sadding to TEMPLATE: scoreAdd=%d multMult=%d for %s + '%c' on %s space",
            // "  ".repeat(depth), scoreAdd, multMult, sofar, ch, template.get(0)));
      int nextScore = scoreSoFar + scoreAdd;
      int nextWordMult = wordMultSoFar * multMult;
      if (nextNode.isword && (newtemplate.isEmpty() || this._mode != Mode.NORMAL) && hasRequiredLetters(nextsofar)) {
        addWord(nextsofar, nextScore * nextWordMult, dotsSoFar, null);
      }
      recurseNormal(depth+1, nextsofar, dotsSoFar, nextScore, nextWordMult, nextNode,
                    letters, newtemplate, true, curPrefixLen, curPostfixLen);
    }
  }

  private void addLetterFromLettersAndRecurse(
        int depth, String sofar, String dotsSoFar,
        int scoreSoFar, int wordMultSoFar,
        TrieNode nodeSoFar, OverUnder overUnder,
        String letters, List<Tile> template,
        int curPrefixLen, int curPostfixLen, LetterPlacement placement) {

    for (char ch : rmDupes(letters).toCharArray()) {
      debugLog(String.format("%s  ADDING '%c' from letters - placement=%s: sofar=%s letters=%s templ=%s",
                             "  ".repeat(depth), ch,
                             placement,
                             sofar, letters, template));

      boolean isDot = ch == '.';
      String newletters = letters.replaceFirst(isDot ? "\\." : String.valueOf(ch), "");
      List<Tile> newtemplate = placement == LetterPlacement.TEMPLATE ? template.subList(1, template.size()) : template;
      int nextPre = placement == LetterPlacement.PREFIX ? curPrefixLen + 1 : curPrefixLen;
      int nextPost = placement == LetterPlacement.POSTFIX ? curPostfixLen + 1 : curPostfixLen;
      boolean nextTemplateStarted = placement != LetterPlacement.PREFIX;

      char[] searchChars = isDot ? "abcdefghijklmnopqrstuvwxyz".toCharArray() : new char[] { ch };
      for (char sch : searchChars) {
        String nextsofar = sofar + sch;
        TrieNode nextNode = nodeSoFar.isPrefix(Character.toString(sch));
        if (nextNode == null) {
          debugLog(String.format("%s  terminate recursion - %s is not a word prefix",
                                 "  ".repeat(depth), nextsofar));
        }
        OverUnderResult oures = maybeCheckOverUnder(depth, sch, template, placement);
        if (nextNode != null && oures.check) {
          String nextDotsSoFar = dotsSoFar + (isDot ? String.valueOf(sch) : "");
          // FIXME: regarding OverUnderResult.scoreToAdd: this needs to be a supplemental add
          //        that does not later get multiplied by the word multipliers
          int scoreAdd =
              (letterScores.get(sch) + oures.scoreToAdd) *
                (placement == LetterPlacement.TEMPLATE && template.size() > 0
                     ? template.get(0).letterMult
                     : 1);
          if (isDot) {
            scoreAdd = 0;
          }
          int wordMult =
              placement == LetterPlacement.TEMPLATE && template.size() > 0
                  ? template.get(0).wordMult
                  : 1;
          // debugLog(String.format("%sadding to %s: scoreAdd=%d wordMult=%d for %s + '%c' on %s space",
                // "  ".repeat(depth), placement, scoreAdd, wordMult, sofar, sch, placement == LetterPlacement.TEMPLATE ? template.get(0) : "PREFIX"));
          int nextScore = scoreSoFar + scoreAdd;
          int nextWordMult = wordMultSoFar * wordMult;

          if (this._mode == Mode.NORMAL) {
            if (nextNode.isword && newtemplate.isEmpty() && hasRequiredLetters(nextsofar)) {
              addWord(nextsofar, nextScore * nextWordMult, nextDotsSoFar, null);
            }
            recurseNormal(depth+1, nextsofar, nextDotsSoFar, nextScore, nextWordMult, nextNode,
                          newletters, newtemplate, nextTemplateStarted, nextPre, nextPost);

          } else {
            OverUnder nextOverUnder = template.size() < 1 || !template.get(0).hasLetter() || placement != LetterPlacement.TEMPLATE
                                     ? overUnder
                                     : overUnder.addOverUnderChar(template.get(0).letter, nextsofar.length() - 1);
            boolean templateFirstLetterCovered = newtemplate.size() < this._fullTemplate.size() - this._templateFirstLetterIndex;
            if (nextNode.isword && templateFirstLetterCovered /* && hasRequiredLetters(nextsofar) */) {
              addWord(nextsofar, nextScore * nextWordMult, nextDotsSoFar, nextOverUnder);
            }
            recurseOverUnder(depth+1, nextOverUnder, nextsofar, nextDotsSoFar, nextScore, nextWordMult, nextNode,
                             newletters, newtemplate, nextTemplateStarted, nextPre, nextPost);
          }
        }
      }
    }
  }

  private OverUnderResult maybeCheckOverUnder(int depth, char ch, List<Tile> template, LetterPlacement placement) {
    if (this._mode == Mode.NORMAL || placement != LetterPlacement.TEMPLATE || template.size() < 1) {
      return new OverUnderResult(true, 0);
    }
    Tile tile = template.get(0);
    if (!tile.hasLetter()) {
      return new OverUnderResult(true, 0);
    }
    char tmpl_ch = tile.letter;

    char[] twoletters = this._mode == Mode.OVER ? new char[] {ch, tmpl_ch}
                                                : new char[] {tmpl_ch, ch};
    String overUnderWord = String.valueOf(twoletters);
    TrieNode node = this._dict.isPrefix(overUnderWord);
    if (node == null || !node.isword) {
      // debug stmt
      debugLog(String.format("%s  terminate recursion from overunder check: %s is not a word",
                             "  ".repeat(depth), overUnderWord));
      return new OverUnderResult(false, 0);
    }
    int scoreToAdd = letterScores.get(tmpl_ch) * tile.wordMult;
    debugLog(String.format("%s    overunder score %d from %c", "  ".repeat(depth), scoreToAdd, tmpl_ch));
    return new OverUnderResult(true, scoreToAdd);
  }

  private void debugLog(String msg) {
    if (DEBUG) {
      System.out.println(msg);
    }
  }

  private String rmDupes(String letters) {
    StringBuilder sb = new StringBuilder();
    char[] chars = letters.toCharArray();
    for (char ch : chars) {
      if (sb.indexOf(String.valueOf(ch)) == -1) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }


  private String calcRequiredLetters(String letters) {
    StringBuilder sb = new StringBuilder();
    char[] chars = letters.toCharArray();
    for (char ch : chars) {
      if (ch != '.' && Character.isUpperCase(ch)) {
        sb.append(Character.toLowerCase(ch));
      }
    }
    return sb.toString();
  }


  private boolean hasRequiredLetters(String word) {
    char[] chars = this._requiredLetters.toCharArray();
    for (char ch : chars) {
      if (word.indexOf(ch) == -1) {
        return false;
      }
    }
    return true;
  }

  private void addWord(String word, int score, String dotVals, OverUnder overUnder) {
    debugLog(String.format("        add_word(%s, %s)", word, dotVals));
    WordInfo prev = this._words.get(word);
    if (prev == null || prev.score < score) {
      this._words.put(word, new WordInfo(score, dotVals, overUnder));
    } else if (prev == null || dotVals.length() < prev.dotVals.length()) {
      this._words.put(word, new WordInfo(score, dotVals, overUnder));
    }
  }


  private static void reportTime(String msg) {
    long now = System.currentTimeMillis();
    if (WordFinder._lastTime > 0) {
      System.out.printf("%s: elapsed = %.3f\n", msg, 1.0 * (now - WordFinder._lastTime) / 1000.0);
    } else {
      System.out.printf("%s: time = 0\n", msg);
    }
    WordFinder._lastTime = now;
  }


  private static String nextArg(String[] args, int argn) {
    return nextArg(args, argn, "");
  }

  private static String nextArg(String[] args, int argn, String defval) {
    if (argn < args.length) {
      return args[argn];
    }
    return defval;
  }


  public static enum Mode {
    NORMAL,
    UNDER,
    OVER
  }


  public static void main(String[] args) {
    int argc = 0;
    Mode mode = Mode.NORMAL;
    boolean wwf = false;
    while (argc < args.length && args[argc].startsWith("-")) {
      String val = nextArg(args, argc++);
      if ("-under".startsWith(val)) {
        mode = Mode.UNDER;
      } else if ("-over".startsWith(val)) {
        mode = Mode.OVER;
      } else if ("-debug".startsWith(val)) {
        DEBUG = true;
      } else if ("-wwf".startsWith(val)) {
        wwf = true;
      } else {
        System.out.println("unrecognized option: "+val);
        System.exit(1);
      }
    }
    System.out.println("mode = " + mode);
    String letters = nextArg(args, argc++);
    String template = nextArg(args, argc++);
    setupLetterScores(wwf);

    if (!validate(letters, template)) {
      return;
    }

    WordFinder.reportTime("loading dictionary...");
    WordFinder wf = new WordFinder(wwf ? "./wwf.txt" : "./scrabble_words.txt");
    WordFinder.reportTime("loaded.");

    Map<String, WordInfo> map = wf.findWords(mode, letters, template);
    WordFinder.reportTime("findWords complete.");

    List<String> words = new ArrayList<>(map.keySet());
    words.sort((a, b) -> {
        WordInfo wia = map.get(a);
        WordInfo wib = map.get(b);
        if (wia.score != wib.score) {
          return wia.score - wib.score;
        }
        if (a.length() != b.length()) {
          return a.length() - b.length();
        }
        if (wia.dotVals.length() != wib.dotVals.length()) {
          return wia.dotVals.length() - wib.dotVals.length();
        }
        return a.compareTo(b);
    });
    for (String word : words) {
      if (!word.equals(template.toLowerCase().replaceAll("\\d", ""))) {
        WordInfo winfo = map.get(word);
        System.out.printf(Locale.ROOT, "%s%s%s score:%d%n",
                winfo.dotVals.isEmpty() ? "" : winfo.dotVals + ": ",
                word,
                winfo.overUnder.isEmpty() ? "" : String.format(" (%s)", winfo.overUnder),
                winfo.score);
      }
    }
  }

  private static boolean validate(String letters, String template) {
    if (letters.length() < 1 || letters.matches(".*[^a-zA-Z\\.].*")) {
      System.out.println("invalid letters: "+letters);
      System.out.println("Letters can be a-z or '.' to represent a blank tile");
      return false;
    }
    if (template.matches(".*[^a-zA-Z0-9\\.\\-\\+#!].*")) {
      System.out.println("invalid template: "+template);
      System.out.println("The template can be a-z, or '.' for an open space,");
      System.out.println("or [- + # !] to represent [DL TL DW TW]");
      return false;
    }
    return true;
  }

  private static boolean DEBUG = false;
  private TrieNode _dict;
  private Map<String, WordInfo> _words;
  private List<Tile> _fullTemplate;
  private int _templateFirstLetterIndex;
  private String _requiredLetters;
  private Mode _mode;
  private int _maxPrefix;
  private int _maxPostfix;
  private static long _lastTime = 0;
}
