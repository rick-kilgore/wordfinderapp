package org.rkilgore.wordfinder;


public class WordInfo {
  public WordInfo(int score, String dotVals, OverUnder overUnder) {
    this.score = score;
    this.dotVals = dotVals;
    this.overUnder = overUnder != null ? overUnder : OverUnder.empty;
  }

  public int score;
  public String dotVals;
  public OverUnder overUnder;
}
