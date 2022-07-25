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
      return String.format("[chars=%s startpos=%d]", this.chars, this.startpos);
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
