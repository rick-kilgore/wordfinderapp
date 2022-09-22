package org.rkilgore.wordfinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import lombok.AllArgsConstructor;


@AllArgsConstructor
class OverUnderResult {
  public final boolean check;
  public final int scoreToAdd;
  public final boolean isLetter;
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
      this.isZeroLetter = false;
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
    public boolean isZeroLetter;
  }

  enum LetterPlacement {
    PREFIX,
    TEMPLATE,
    POSTFIX
  }

  private static Logger logger = Logger.getLogger("WordFinder");

  private void setupLetterScores(boolean wwf) {
    _letterScores.put('a', 1);
    _letterScores.put('b', wwf ? 4 : 3);
    _letterScores.put('c', wwf ? 4 : 3);
    _letterScores.put('d', 2);
    _letterScores.put('e', 1);
    _letterScores.put('f', 4);
    _letterScores.put('g', 3);
    _letterScores.put('h', wwf ? 3 : 4);
    _letterScores.put('i', 1);
    _letterScores.put('j', wwf ? 10 : 11);
    _letterScores.put('k', 5);
    _letterScores.put('l', wwf ? 2 : 1);
    _letterScores.put('m', wwf ? 4 : 3);
    _letterScores.put('n', 2);
    _letterScores.put('o', 1);
    _letterScores.put('p', wwf ? 4 : 3);
    _letterScores.put('q', wwf ? 10 : 11);
    _letterScores.put('r', 1);
    _letterScores.put('s', 1);
    _letterScores.put('t', 1);
    _letterScores.put('u', wwf ? 2 : 3);
    _letterScores.put('v', wwf ? 5 : 4);
    _letterScores.put('w', 4);
    _letterScores.put('x', 8);
    _letterScores.put('y', wwf ? 3 : 4);
    _letterScores.put('z', wwf ? 10 : 11);
  }


  public WordFinder(String dictfilename, boolean wwf) {
    this._dict = new TrieNode(dictfilename);
    this.setupLetterScores(wwf);
    this.debug = false;
  }

  public WordFinder(Scanner scanner, boolean wwf) {
    this._dict = new TrieNode(scanner);
    this.setupLetterScores(wwf);
    this.debug = false;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public Map<String, WordInfo> findWords(Mode mode, String letters, String template) {

    int maxPrefix = 7;
    int maxPostfix = 7;
    if (!template.isEmpty()) {
      if (Character.isDigit(template.charAt(0))) {
        maxPrefix = Character.digit(template.charAt(0), 10);
        template = template.substring(1);
      }
      int last = template.length() - 1;
      if (Character.isDigit(template.charAt(last))) {
        maxPostfix = Character.digit(template.charAt(last), 10);
        template = template.substring(0, last);
      }
    }

    List<Tile> tiles = new ArrayList<Tile>();
    boolean modifier = false;
    for (char ch : template.toCharArray()) {
      if (ch == ':') {
        modifier = true;
      } else if (modifier) {
        modifyLastTile(tiles, ch);
        modifier = false;
      } else if (ch == '.') {
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
      } else if (Character.isLetter(ch)) {
        if (mode == Mode.NORMAL) {
          tiles.add(Tile.forLetter(ch));
        } else {
          tiles.add(Tile.openTileWithLetter(Character.toLowerCase(ch)));
        }
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
              ScoreKeeper.empty, this._dict /* nodeSoFar */,
              letters, tiles, false /* templateStarted */,
              0 /* curPrefixLen */, 0 /* curPostfixLen */);
    } else {
      recurseOverUnder(0 /* depth */, OverUnder.empty, "" /* sofar */, "" /* dotsSoFar */,
              ScoreKeeper.empty, this._dict /* nodeSoFar */,
              letters, tiles, false /* templateStarted */,
              0 /* curPrefixLen */, 0 /* curPostfixLen */);
    }

    return this._words;
  }


  private void modifyLastTile(List<Tile> tiles, char modch) {
    if (tiles.isEmpty()) {
      throw new RuntimeException(String.format("No tile to modify with mod %c", modch));
    }
    Tile last = tiles.get(tiles.size() - 1);
    if (modch == '-') {
      last.letterMult = 2;
    } else if (modch == '+') {
      last.letterMult = 3;
    } else if (modch == '#') {
      last.wordMult = 2;
    } else if (modch == '!') {
      last.wordMult = 3;
    } else if (modch == '0') {
      last.isZeroLetter = true;
    } else if (modch == '@') {
      last.open = false;
    } else {
      throw new RuntimeException(String.format("Unrecognized mod character: '%c'", modch));
    }
  }


  private void recurseNormal(
      int depth,
      String sofar,
      String dotsSoFar,
      ScoreKeeper scoreSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      boolean templateStarted,
      int curPrefixLen,
      int curPostfixLen) {

    debugLog(String.format("%srecurseNormal sofar=%s dotsSoFar=%s letters=%s template=%s score=%s prefix=%d postfix=%d "
                           + "templStarted=%s",
                           forDepth(depth),
                           sofar, dotsSoFar, letters, template, scoreSoFar, curPrefixLen, curPostfixLen,
                           String.valueOf(templateStarted)));

    // ---> check for terminate recursion
    if (shouldTerminate(depth, sofar, letters, template, curPostfixLen)) {
      return;
    }

    // ---> try adding from letters to prefix before template
    tryAddToPrefix(depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar,
                   letters, template, templateStarted, curPrefixLen);


    // ---> open tile two types - empty space or preset letter from letters
    if (!template.isEmpty()) {
      addFromTemplateNormal(depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar,
                            letters, template, curPrefixLen);

    } else {
      addToPostfixOrTerminate(depth, null, sofar, dotsSoFar, scoreSoFar, nodeSoFar,
                              letters, curPrefixLen, curPostfixLen);
    }

  }

  private void recurseOverUnder(
      int depth,
      OverUnder overUnder,
      String sofar,
      String dotsSoFar,
      ScoreKeeper scoreSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      boolean templateStarted,
      int curPrefixLen,
      int curPostfixLen) {

    debugLog(String.format("%srecurseOverUnder overUnder=%s sofar=%s scoreSoFar=%s dotsSoFar=%s letters=%s template=%s prefix=%d postfix=%d "
                           + "templStarted=%s",
                           forDepth(depth),
                           overUnder.isEmpty() ? "" : overUnder.forWord(sofar, this._mode),
                           sofar, scoreSoFar, dotsSoFar, letters, template, curPrefixLen, curPostfixLen,
                           String.valueOf(templateStarted)));

    // ---> check for terminate recursion
    if (shouldTerminate(depth, sofar, letters, template, curPostfixLen)) {
      return;
    }

    // ---> try adding from letters to prefix before template
    tryAddToPrefix(depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar,
                   letters, template, templateStarted, curPrefixLen);


    if (!template.isEmpty()) {
        // ---> add letter from letters
        // debugLog(String.format("%s  template tile - add from letters: sofar=%s letters=%s templ=%s",
                               // forDepth(depth),
                               // sofar, letters, template));

        Tile nextTile = template.get(0);
        if (!nextTile.open) {
          // ---> <letter>:n means add this char from template as a normal template character - i.e. don't do over under
          addLetterFromTemplateAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar, overUnder,
            letters, template, curPrefixLen);
        } else {
          addLetterFromLettersAndRecurse(
              depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar, overUnder,
              letters, template, curPrefixLen, 0, LetterPlacement.TEMPLATE);
        }

    } else {
      addToPostfixOrTerminate(depth, overUnder, sofar, dotsSoFar, scoreSoFar, nodeSoFar,
                              letters, curPrefixLen, curPostfixLen);
    }

    // ---> if no word started and more template left, try removng one template letter (start after that point)
    if (sofar.length() == 0 && !template.isEmpty() && template.get(0).open) {
      List<Tile> newtemplate = template.subList(1, template.size());
      recurseOverUnder(depth, overUnder, sofar, dotsSoFar, scoreSoFar, nodeSoFar,
                       letters, newtemplate, true, 0, 0);
    }
  }


  private String forDepth(int depth) {
    return "    ".repeat(depth);
  }


  private boolean shouldTerminate(int depth, String sofar, String letters, List<Tile> template, int curPostfixLen) {
    boolean nextIsTemplateLetter = !template.isEmpty() && !template.get(0).open;
    boolean cantAddPostfix = curPostfixLen == this._maxPostfix;
    if ((letters.isEmpty() && !nextIsTemplateLetter) ||
        (template.isEmpty() && cantAddPostfix)) {
      debugLog(String.format("%s    terminate recursion: sofar=%s letters=%s templ=%s postfixLen=%d",
                             forDepth(depth), sofar, letters, template, curPostfixLen));
      return true;
    }
    return false;
  }


  private void tryAddToPrefix(
      int depth,
      String sofar,
      String dotsSoFar,
      ScoreKeeper scoreSoFar,
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
        // debugLog(String.format("%s  prefix add from letters: remainingNeeded=%d for sofar=%s letters=%s templ=%s",
                               // forDepth(depth),
                               // remainingLettersNeeded,
                               // sofar, letters, template));

        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar, OverUnder.empty,
            letters, template, curPrefixLen, 0, LetterPlacement.PREFIX);
      } else {
        debugLog(String.format("%s  no prefix add: remainingNeeded=%d letters=%s",
                               forDepth(depth),
                               remainingLettersNeeded, letters));
      }
    }
  }


  private void addFromTemplateNormal(
      int depth,
      String sofar,
      String dotsSoFar,
      ScoreKeeper scoreSoFar,
      TrieNode nodeSoFar,
      String letters,
      List<Tile> template,
      int curPrefixLen) {

    assert !template.isEmpty();

    Tile nextTile = template.get(0);
    if (nextTile.open && !nextTile.hasLetter()) {
        // ---> open empty tile ([.-+#!])
        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar, OverUnder.empty,
            letters, template, curPrefixLen, 0, LetterPlacement.TEMPLATE);

    } else {
      // ---> template letter tile - add letter from template
      assert nextTile.open || nextTile.hasLetter();
      debugLog(String.format("%s  add templ letter '%c': sofar=%s letters=%s templ=%s",
                             forDepth(depth),
                             template.get(0).letter,
                             sofar, letters, template));

      if (nextTile.open && nextTile.hasLetter()) {
        // ---> user has requested to put one of their letters in this spot
        char ch = nextTile.letter;
        if (letters.indexOf(ch) == -1) {
          // can't fulfill this request
          debugLog(String.format("%s  recursion stopped - cannot fill template letter tile '%c': sofar=%s letters=%s templ=%s",
                                 forDepth(depth),
                                 nextTile.letter, sofar, letters, template));
          return;
        }
        letters = letters.replaceFirst(String.valueOf(ch), "");
      }

      addLetterFromTemplateAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar, OverUnder.empty,
            letters, template, curPrefixLen);
    }
  }


  private void addToPostfixOrTerminate(
      int depth,
      OverUnder overUnder,
      String sofar,
      String dotsSoFar,
      ScoreKeeper scoreSoFar,
      TrieNode nodeSoFar,
      String letters,
      int curPrefixLen,
      int curPostfixLen) {

    if (curPostfixLen < this._maxPostfix) {
      // ---> add letter to the postfix
      debugLog(String.format("%s  postfix add from letters: sofar=%s letters=%s",
                             forDepth(depth),
                             sofar, letters));

      addLetterFromLettersAndRecurse(
          depth, sofar, dotsSoFar, scoreSoFar, nodeSoFar, overUnder,
          letters, Collections.emptyList(), curPrefixLen, curPostfixLen, LetterPlacement.POSTFIX);

    } else {
      debugLog(String.format("%s  terminate - template and postfix exhausted: sofar=%s letters=%s postfixLen=%d",
                             forDepth(depth),
                             letters.charAt(0),
                             sofar, letters, curPostfixLen));
    }
  }


  private void addLetterFromTemplateAndRecurse(
        int depth, String sofar, String dotsSoFar,
        ScoreKeeper scoreSoFar,
        TrieNode nodeSoFar, OverUnder overUnder,
        String letters, List<Tile> template,
        int curPrefixLen) {
    Tile nextTile = template.get(0);
    char ch = nextTile.letter;
    TrieNode nextNode = nodeSoFar.isPrefix(Character.toString(ch));
    boolean isOverUnder = this._mode != Mode.NORMAL;
    debugLog(String.format("%s  ADDING '%c' from template: sofar=%s letters=%s templ=%s",
                           forDepth(depth), ch,
                           sofar, letters, template));
    if (nextNode != null) {
      String nextsofar = sofar + ch;
      List<Tile> newtemplate = template.subList(1, template.size());
      int scoreAdd = nextTile.isZeroLetter ? 0 : _letterScores.get(ch) * template.get(0).letterMult;
      int wordMult = template.get(0).wordMult;
      // debugLog(String.format("%sadding to TEMPLATE: scoreAdd=%d wordMult=%d for %s + '%c' on %s space",
            // forDepth(depth), scoreAdd, wordMult, sofar, ch, template.get(0)));
      ScoreKeeper nextScore = scoreSoFar.add(scoreAdd).mult(wordMult);
      if (nextNode.isword
          && (newtemplate.isEmpty() || (isOverUnder && (newtemplate.isEmpty() || newtemplate.get(0).open)))
          && hasRequiredLetters(nextsofar)) {
        addWord(depth, nextsofar, nextScore, dotsSoFar, overUnder);
      }
      if (isOverUnder) {
        recurseOverUnder(depth+1, overUnder, nextsofar, dotsSoFar, nextScore, nextNode,
                         letters, newtemplate, true, curPrefixLen, 0);
      } else {
        recurseNormal(depth+1, nextsofar, dotsSoFar, nextScore, nextNode,
                      letters, newtemplate, true, curPrefixLen, 0);
      }
    }
  }

  private void addLetterFromLettersAndRecurse(
        int depth, String sofar, String dotsSoFar,
        ScoreKeeper scoreSoFar,
        TrieNode nodeSoFar, OverUnder overUnder,
        String letters, List<Tile> template,
        int curPrefixLen, int curPostfixLen, LetterPlacement placement) {

    for (char ch : rmDupes(letters).toCharArray()) {
      debugLog(String.format("%s  %s ADDING '%c' from letters: sofar=%s letters=%s templ=%s",
                             forDepth(depth),
                             placement, ch,
                             sofar, letters, template));

      boolean isDot = ch == '.';
      String newletters = letters.replaceFirst(isDot ? "\\." : String.valueOf(ch), "");
      List<Tile> newtemplate = placement == LetterPlacement.TEMPLATE ? template.subList(1, template.size()) : template;
      int nextPre = placement == LetterPlacement.PREFIX ? curPrefixLen + 1 : curPrefixLen;
      int nextPost = placement == LetterPlacement.POSTFIX ? curPostfixLen + 1 : curPostfixLen;
      boolean nextTemplateStarted = placement != LetterPlacement.PREFIX;
      boolean isOverUnder = this._mode != Mode.NORMAL;

      char[] searchChars = isDot ? "abcdefghijklmnopqrstuvwxyz".toCharArray() : new char[] { ch };
      for (char sch : searchChars) {
        String nextsofar = sofar + sch;
        TrieNode nextNode = nodeSoFar.isPrefix(Character.toString(sch));
        if (nextNode == null) {
          debugLog(String.format("%s    terminate recursion - %s is not a word prefix",
                                 forDepth(depth), nextsofar));
          continue;
        }
        OverUnderResult oures = maybeCheckOverUnder(depth, sch, template, placement);
        if (!isOverUnder || oures.check) {
          String nextDotsSoFar = dotsSoFar + (isDot ? String.valueOf(sch) : "");
          // FIXME: regarding OverUnderResult.scoreToAdd: this needs to be a supplemental add
          //        that does not later get multiplied by the word multipliers
          int letterMult = 
                (placement == LetterPlacement.TEMPLATE && template.size() > 0
                     ? template.get(0).letterMult
                     : 1);
          int letterAdd = isDot ? 0 : _letterScores.get(sch) * letterMult;
          int wordMult = 
              placement == LetterPlacement.TEMPLATE && template.size() > 0
                  ? template.get(0).wordMult
                  : 1;
          ScoreKeeper nextScore = scoreSoFar.add(letterAdd).mult(wordMult);
          // debugLog(String.format("%sadding to %s: scoreAdd=%d wordMult=%d for %s + '%c' on %s space",
                // forDepth(depth), placement, scoreAdd, wordMult, sofar, sch, placement == LetterPlacement.TEMPLATE ? template.get(0) : "PREFIX"));

          if (isOverUnder) {
            int ouScoreAdd = oures.isLetter ? (oures.scoreToAdd + letterAdd) * wordMult : 0;
            nextScore = nextScore.addOverUnder(ouScoreAdd);
            OverUnder nextOverUnder = template.size() < 1 || !template.get(0).hasLetter() || placement != LetterPlacement.TEMPLATE
                                     ? overUnder
                                     : overUnder.addOverUnderChar(template.get(0).letter, nextsofar.length() - 1);
            // boolean templateFirstLetterCovered = newtemplate.size() < this._fullTemplate.size() - this._templateFirstLetterIndex;
            boolean templateFirstLetterCovered = !nextOverUnder.isEmpty();
            debugLog(String.format("%s    after adding %c%s scoreAdd=%d ouScoreAdd=%d score=%s",
                                   forDepth(depth),
                                   sch,
                                   nextOverUnder.isEmpty() ? "" : " " + nextOverUnder.forWord(nextsofar, this._mode),
                                   letterAdd,
                                   ouScoreAdd,
                                   nextScore));
            if (nextNode.isword && templateFirstLetterCovered
                && (newtemplate.isEmpty() || newtemplate.get(0).open)
                /* && hasRequiredLetters(nextsofar) */) {
              addWord(depth, nextsofar, nextScore, nextDotsSoFar, nextOverUnder);
            }
            recurseOverUnder(depth+1, nextOverUnder, nextsofar, nextDotsSoFar, nextScore, nextNode,
                             newletters, newtemplate, nextTemplateStarted, nextPre, nextPost);

          } else {
            if (nextNode.isword && newtemplate.isEmpty() && hasRequiredLetters(nextsofar)) {
              addWord(depth, nextsofar, nextScore, nextDotsSoFar, null);
            }
            recurseNormal(depth+1, nextsofar, nextDotsSoFar, nextScore, nextNode,
                          newletters, newtemplate, nextTemplateStarted, nextPre, nextPost);
          }
        }
      }
    }
  }

  private OverUnderResult maybeCheckOverUnder(int depth, char ch, List<Tile> template, LetterPlacement placement) {
    if (this._mode == Mode.NORMAL || placement != LetterPlacement.TEMPLATE || template.size() < 1) {
      return new OverUnderResult(true, 0, false);
    }
    Tile tile = template.get(0);
    if (!tile.hasLetter()) {
      return new OverUnderResult(true, 0, false);
    }
    char tmpl_ch = tile.letter;

    char[] twoletters = this._mode == Mode.OVER ? new char[] {ch, tmpl_ch}
                                                : new char[] {tmpl_ch, ch};
    String overUnderWord = String.valueOf(twoletters);
    TrieNode node = this._dict.isPrefix(overUnderWord);
    if (node == null || !node.isword) {
      // debug stmt
      debugLog(String.format("%s    terminate recursion from overunder check: %s is not a word",
                             forDepth(depth), overUnderWord));
      return new OverUnderResult(false, 0, true);
    }
    int scoreToAdd = tile.isZeroLetter ? 0 : _letterScores.get(tmpl_ch);
    debugLog(String.format("%s    overunder score %d from %c", forDepth(depth), scoreToAdd, tmpl_ch));
    return new OverUnderResult(true, scoreToAdd, true);
  }

  private void debugLog(String msg) {
    if (this.debug) {
      logger.info(msg);
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

  private void addWord(int depth, String word, ScoreKeeper score, String dotVals, OverUnder overUnder) {
    debugLog(String.format("%s    addWord(%s, %s, %s=%d)", forDepth(depth), word, dotVals, score, score.score()));
    WordInfo prev = this._words.get(word);
    if (prev == null || prev.score.score() < score.score()) {
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

  static {
    try (InputStream is = WordFinder.class.getClassLoader().getResourceAsStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(is);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public static void main(String[] args) {
    int argc = 0;
    Mode mode = Mode.NORMAL;
    boolean wwf = false;
    boolean debug = false;
    while (argc < args.length && args[argc].startsWith("-")) {
      String val = nextArg(args, argc++);
      if ("-under".startsWith(val)) {
        mode = Mode.UNDER;
      } else if ("-over".startsWith(val)) {
        mode = Mode.OVER;
      } else if ("-debug".startsWith(val)) {
        debug = true;
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

    if (!validate(letters, template)) {
      return;
    }

    WordFinder.reportTime("loading dictionary...");
    WordFinder wf = new WordFinder(wwf ? "./wwf.txt" : "./scrabble_words.txt", wwf);
    wf.setDebug(debug);
    WordFinder.reportTime("loaded.");

    Map<String, WordInfo> map = wf.findWords(mode, letters, template);
    WordFinder.reportTime("findWords complete.");

    List<String> words = new ArrayList<>(map.keySet());
    words.sort((a, b) -> {
        WordInfo wia = map.get(a);
        WordInfo wib = map.get(b);
        if (wia.score.score() != wib.score.score()) {
          return wia.score.score() - wib.score.score();
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
                winfo.overUnder.isEmpty() ? "" : String.format(" %s", winfo.overUnder.forWord(word, mode)),
                winfo.score.score());
      }
    }
  }

  public static boolean validate(String letters, String template) {
    if (letters.length() < 1 || letters.matches(".*[^a-zA-Z\\.].*")) {
      System.out.println("invalid letters: "+letters);
      System.out.println("Letters can be a-z or '.' to represent a blank tile");
      return false;
    }
    if (template.matches(".*[^a-zA-Z0-9\\.\\-\\+#!:@].*")) {
      System.out.println("invalid template: "+template);
      System.out.print("The template can be a-z, '.' for an open space, ");
      System.out.println("[- + # !] to represent [DL TL DW TW], or ':' to start a mod");
      return false;
    }
    return true;
  }

  private boolean debug;
  private TrieNode _dict;
  private Map<String, WordInfo> _words;
  private Map<Character, Integer> _letterScores = new HashMap<>();
  private List<Tile> _fullTemplate;
  private int _templateFirstLetterIndex;
  private String _requiredLetters;
  private Mode _mode;
  private int _maxPrefix;
  private int _maxPostfix;
  private static long _lastTime = 0;
}
