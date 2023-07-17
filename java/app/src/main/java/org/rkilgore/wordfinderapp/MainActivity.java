package org.rkilgore.wordfinderapp;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.rkilgore.wordfinder.FindResult;
import org.rkilgore.wordfinder.Mode;
import org.rkilgore.wordfinder.ValidateResult;
import org.rkilgore.wordfinder.WordFinder;
import org.rkilgore.wordfinder.WordInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.LogManager;

public class MainActivity extends AppCompatActivity implements View.OnKeyListener {

    static {
      try (InputStream is = WordFinder.class.getClassLoader().getResourceAsStream("logging.properties")) {
        LogManager.getLogManager().readConfiguration(is);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private boolean debug;
    private WordFinder wf;
    private Mode mode;


    public MainActivity() {
      this.debug = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidLoggingHandler.reset(new AndroidLoggingHandler());

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        EditText lettersText = findViewById(R.id.lettersText);
        EditText patternText = findViewById(R.id.patternText);
        lettersText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        patternText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        lettersText.setOnKeyListener(this);
        patternText.setOnKeyListener(this);
        lettersText.requestFocus();

        try {
            this.wf = new WordFinder(new Scanner(getAssets().open("wwf.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onDebugClicked(View view) {
        final CheckBox cb = (CheckBox) view;
        this.debug = cb.isChecked();
    }

    public void onEdit(View view) {
      final EditText lettersText = findViewById(R.id.lettersText);
      lettersText.requestFocus();
      showKeyboard();
    }

    public void sendMessage(View view) {
        hideKeyboard();
        findWords(view);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        try { Thread.sleep(250); } catch(InterruptedException e) {
            // ignore
        }
    }

    private void showKeyboard() {
      View view = this.getCurrentFocus();
      if (view != null) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
      }
      try { Thread.sleep(250); } catch(InterruptedException e) {
          // ignore
      }
    }

    private void endFind(ProgressBar spinner, TextView output, StringBuilder sb) {
      runOnUiThread(() -> {
          spinner.setVisibility(View.GONE);
          output.setText(sb.toString());
      });
    }

    private void findWords(View view) {
        final EditText lettersText = findViewById(R.id.lettersText);
        final EditText patternText = findViewById(R.id.patternText);
        String letters = lettersText.getText().toString();
        String pattern = patternText.getText().toString();
        // System.out.println(String.format("rkilgore: letters=%s  pattern=%s", letters, pattern));
        final TextView output = findViewById(R.id.outputText);
        output.setText("");
        output.invalidate();
        output.requestFocus();

        ValidateResult res = WordFinder.validate(letters, pattern);
        if (!res.valid) {
          output.setText(res.errmsg);
          output.invalidate();
          return;
        }

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.setVisibility(View.VISIBLE);
        this.wf.setDebug(this.debug);

        Thread thread = new Thread() {
          public void run() {
            FindResult res = wf.findWords(letters, pattern);
            StringBuilder sb = new StringBuilder();
            if (!res.ok) {
              sb.append(res.errmsg);
              endFind(spinner, output, sb);
              return;
            }
            Map<String, WordInfo> map = res.words;

            List<String> words = new ArrayList<>(map.keySet());
            if (words.size() < 1) {
              sb.append("no words found");
              endFind(spinner, output, sb);
              return;
            }

            Collections.sort(words, (String a, String b) -> {
                  WordInfo ainf = map.get(a);
                  WordInfo binf = map.get(b);
                  assert ainf != null;
                  assert binf != null;
                  if (ainf.score.score() != binf.score.score()) {
                      return binf.score.score - ainf.score.score;
                  }
                  if (a.length() != b.length()) {
                      return b.length() - a.length();
                  }
                  if (ainf.dotVals.length() != binf.dotVals.length()) {
                      return ainf.dotVals.length() - binf.dotVals.length();
                  }
                  if (!ainf.dotVals.equals(binf.dotVals)) {
                      return ainf.dotVals.compareTo(binf.dotVals);
                  }
                  return a.compareTo(b);
            });

            System.out.println("printing results: mode = " + wf.getMode());
            for (String word : words) {
                WordInfo winfo = map.get(word);
                sb.append(String.format("%s%s%s score:%d%n",
                        winfo.dotVals.isEmpty() ? "" : winfo.dotVals + ": ",
                        word,
                        winfo.overUnder.isEmpty() ? "" : String.format(" %s", winfo.overUnder.forWord(word, wf.getMode())),
                        winfo.score.score()));
            }
            endFind(spinner, output, sb);
          }
        };
        thread.start();
    }

    public boolean onKey(View view, int keyCode, KeyEvent event) {
      if (Arrays.asList(IME_ACTION_DONE, IME_ACTION_GO, KeyEvent.KEYCODE_ENTER).contains(keyCode)) {
          hideKeyboard();
          findWords(view);
      }
      return true;
    }
}
