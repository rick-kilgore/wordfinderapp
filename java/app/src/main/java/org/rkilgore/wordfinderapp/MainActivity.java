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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.rkilgore.wordfinder.WordFinder;
import org.rkilgore.wordfinder.Mode;
import org.rkilgore.wordfinder.WordInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.LogManager;

public class MainActivity extends AppCompatActivity implements View.OnKeyListener, AdapterView.OnItemSelectedListener {

  static {
    try (InputStream is = WordFinder.class.getClassLoader().getResourceAsStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(is);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

    private boolean useWwf;
    private boolean debug;
    private WordFinder wf;
    private WordFinder wwf;
    private Mode mode;


    public MainActivity() {
      this.useWwf = false;
      this.debug = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidLoggingHandler.reset(new AndroidLoggingHandler());

        final Spinner modes = findViewById(R.id.modes_pulldown);
        ArrayAdapter<CharSequence> items
                = ArrayAdapter.createFromResource(this, R.array.modes, android.R.layout.simple_spinner_item);
        items.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modes.setAdapter(items);
        modes.setOnItemSelectedListener(this);

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        EditText lettersText = findViewById(R.id.lettersText);
        EditText patternText = findViewById(R.id.patternText);
        lettersText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        patternText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        lettersText.setOnKeyListener(this);
        patternText.setOnKeyListener(this);
        lettersText.requestFocus();

        Scanner wordsFile = null;
        try {
            this.wf = new WordFinder(new Scanner(getAssets().open("scrabble_words.txt")), false);
            this.wwf = new WordFinder(new Scanner(getAssets().open("wwf.txt")), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onWwfClicked(View view) {
      final CheckBox cb = (CheckBox) view;
      this.useWwf = cb.isChecked();
    }

    public void onDebugClicked(View view) {
      final CheckBox cb = (CheckBox) view;
      this.debug = cb.isChecked();
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

        if (!WordFinder.validate(letters, pattern)) {
          output.setText("validation failed");
          output.invalidate();
          return;
        }

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.setVisibility(View.VISIBLE);
        final WordFinder wf = this.useWwf ? this.wwf : this.wf;
        wf.setDebug(this.debug);

        Thread thread = new Thread() {
          public void run() {
            Map<String, WordInfo> map = wf.findWords(MainActivity.this.mode, letters, pattern);

            List<String> words = new ArrayList<>(map.keySet());
            Collections.sort(words, (String a, String b) -> {
                  WordInfo ainf = map.get(a);
                  WordInfo binf = map.get(b);
                  assert ainf != null;
                  assert binf != null;
                  if (ainf.score != binf.score) {
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

            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                WordInfo winfo = map.get(word);
                sb.append(String.format("%s%s%s score:%d%n",
                        winfo.dotVals.isEmpty() ? "" : winfo.dotVals + ": ",
                        word,
                        winfo.overUnder.isEmpty() ? "" : String.format(" %s", winfo.overUnder.forWord(word, mode)),
                        winfo.score.score()));
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String selected = ((String) adapterView.getItemAtPosition(i)).toLowerCase(Locale.ROOT);
        this.mode = Mode.NORMAL;
        for (Mode mode : Mode.values()) {
            if (selected.equals(mode.name().toLowerCase(Locale.ROOT))) {
                this.mode = mode;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        this.mode = Mode.NORMAL;
    }
}
