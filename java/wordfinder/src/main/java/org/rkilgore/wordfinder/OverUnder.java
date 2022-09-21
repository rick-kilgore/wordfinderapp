package org.rkilgore.wordfinder;

public class OverUnder {

    static {
      empty = new OverUnder(null, -1);
    }

    public final String chars;
    public final int startpos;
    public static OverUnder empty;

    public OverUnder(String chars, int startpos) {
        this.chars = chars;
        this.startpos = startpos;
    }

    public String toString() {
      return String.format("[%d overunder=%s]", this.chars == null ? 0 : this.chars.length(), this.chars);
    }

    public String forWord(String word, Mode mode) {
      StringBuilder sb = new StringBuilder();
      if (this.startpos == -1) {
        sb.append("[empty]");
        return sb.toString();
      }
      boolean isOver = mode == Mode.OVER;
      sb.append("[");  // append(isOver ? "over" : "under").append(": ");
      sb.append(word.substring(this.startpos, this.startpos + this.chars.length()));
      sb.append(isOver ? " over " : " under ");
      sb.append(this.chars);
      sb.append("]");
      return sb.toString();
    }

    public OverUnder addOverUnderChar(char ch, int pos) {
        if (this.chars == null) {
          return new OverUnder(String.valueOf(ch), pos);
        }
        return new OverUnder(this.chars + ch, this.startpos);
    }
    
    public boolean isEmpty() {
        return this.chars == null;
    }
}
