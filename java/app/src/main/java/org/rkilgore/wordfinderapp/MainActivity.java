package org.rkilgore.wordfinderapp;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.rkilgore.wordfinder.WordFinder;
import org.rkilgore.wordfinder.WordInfo;

import java.io.IOException;
import java.util.*;

public class MainActivity extends AppCompatActivity implements View.OnKeyListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        EditText lettersText = findViewById(R.id.lettersText);
        EditText patternText = findViewById(R.id.patternText);
        lettersText.setOnKeyListener(this);
        patternText.setOnKeyListener(this);
        lettersText.requestFocus();

        Scanner wordsFile = null;
        try {
            wordsFile = new Scanner(getAssets().open("scrabble_words.txt"));
            this.wf = new WordFinder(wordsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.setVisibility(View.VISIBLE);

        Thread thread = new Thread() {
          @SuppressLint("DefaultLocale")
          public void run() {
            Map<String, WordInfo> map = MainActivity.this.wf.findWords(letters, pattern, 7, 7);

            List<String> words = new ArrayList<>(map.keySet());
            Collections.sort(words, (String a, String b) -> {
                  WordInfo ainf = map.get(a);
                  WordInfo binf = map.get(b);
                  assert ainf != null;
                  assert binf != null;
                  if (ainf.score != binf.score) {
                      return binf.score - ainf.score;
                  }
                  if (a.length() != b.length()) {
                      return b.length() - a.length();
                  }
                  if (ainf.dotVals.length() != ainf.dotVals.length()) {
                      return ainf.dotVals.length() - binf.dotVals.length();
                  }
                  if (!ainf.dotVals.equals(binf.dotVals)) {
                      return ainf.dotVals.compareTo(binf.dotVals);
                  }
                  return a.compareTo(b);
            });

            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                WordInfo winf = map.get(word);
                String dotVals = winf.dotVals;
                String dots =  dotVals.length() > 0 ? dotVals + ": " : "";
                sb.append(String.format("%s%s score: %d\n", dots, word, winf.score));
            }
            runOnUiThread(() -> {
                spinner.setVisibility(View.GONE);
                output.setText(sb.toString());
            });
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
    
    private WordFinder wf;
}
