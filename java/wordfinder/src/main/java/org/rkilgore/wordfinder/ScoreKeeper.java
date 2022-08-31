package org.rkilgore.wordfinder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class ScoreKeeper {

  public static final ScoreKeeper empty = new ScoreKeeper(0, 0, 1);

  public final int score;
  public final int overUnderScore;
  public final int wordMult;

  public ScoreKeeper add(int scoreToAdd) {
    return this.toBuilder().score(this.score + scoreToAdd).build();
  }

  public ScoreKeeper addOverUnder(int scoreToAdd) {
    return this.toBuilder()
      .overUnderScore(this.overUnderScore + scoreToAdd)
      .build();
  }

  public ScoreKeeper mult(int wordMult) {
    return this.toBuilder().wordMult(this.wordMult * wordMult).build();
  }

  public int score() {
    return this.score * this.wordMult + this.overUnderScore;
  }
}
