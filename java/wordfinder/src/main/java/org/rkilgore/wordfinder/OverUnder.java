package org.rkilgore.wordfinder;

public class OverUnder {
    public String chars;
    public int startpos;

    public OverUnder() {
        chars = null;
        startpos = -1;
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
