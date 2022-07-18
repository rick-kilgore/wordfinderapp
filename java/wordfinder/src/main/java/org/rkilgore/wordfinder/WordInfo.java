package org.rkilgore.wordfinder;


public class WordInfo {
  public WordInfo(int score, String dotVals) {
    this(score, dotVals, "");
  }

  public WordInfo(int score, String dotVals, String overUnder) {
    this.score = score;
    this.dotVals = dotVals;
    this.overUnder = overUnder;
  }

  public int score;
  public String dotVals;
  public String overUnder;
}
