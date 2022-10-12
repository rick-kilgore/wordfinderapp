package org.rkilgore.wordfinder;

import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FindResult {
  public final Map<String, WordInfo> words;
  public final boolean ok;
  public final String errmsg;
}
