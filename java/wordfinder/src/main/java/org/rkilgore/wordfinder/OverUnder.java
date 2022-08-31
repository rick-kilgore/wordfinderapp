package org.rkilgore.wordfinder;

public class OverUnder {

    static {
      empty = new OverUnder();
    }

    public String chars;
    public int startpos;
    public static OverUnder empty;

    public OverUnder() {
        chars = null;
        startpos = -1;
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
        OverUnder newone = new OverUnder();
        if (this.chars == null) {
            newone.chars = String.valueOf(ch);
            newone.startpos = pos;
        } else {
            newone.chars = this.chars + ch;
            newone.startpos = this.startpos;
        }
        return newone;
    }
    
    public boolean isEmpty() {
        return this.chars == null;
    }
}
