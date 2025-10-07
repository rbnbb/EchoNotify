package com.correctsyntax.biblenotify;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QuoteParser {
  private static final String TAG = "QuoteParser";

  public static class Quote {
    public String text;
    public String category;

    public Quote(String text, String category) {
      this.text = text;
      this.category = category != null ? category : "General";
    }
  }

  public static List<Quote> parseTextFile(String filePath, String category) {
    List<Quote> quotes = new ArrayList<>();
    
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty()) {
          String cleanedQuote = cleanQuoteText(line);
          if (!cleanedQuote.isEmpty()) {
            quotes.add(new Quote(cleanedQuote, category));
          }
        }
      }
    } catch (IOException e) {
      Log.e(TAG, "Error parsing text file: " + e.getMessage());
    }

    return quotes;
  }

  private static String cleanQuoteText(String text) {
    text = text.trim();
    text = text.replaceAll("^\\d+\\.\\s*", "");
    text = text.replaceAll("^\\d+\\s*", "");
    text = text.replaceAll("^-\\s*", "");
    text = text.replaceAll("^•\\s*", "");
    return text.trim();
  }

  public static boolean generateQuoteJSON(Context context, List<Quote> quotes, String filename) {
    try {
      JSONObject root = new JSONObject();
      JSONArray allQuotes = new JSONArray();

      for (int i = 0; i < quotes.size(); i++) {
        Quote quote = quotes.get(i);
        JSONObject quoteObj = new JSONObject();
        quoteObj.put("verse", quote.text);
        quoteObj.put("place", quote.category);
        quoteObj.put("data", "custom/" + i);
        allQuotes.put(quoteObj);
      }

      root.put("all", allQuotes);

      File assetsDir = new File(context.getFilesDir(), "assets/quotes");
      if (!assetsDir.exists()) {
        assetsDir.mkdirs();
      }

      File jsonFile = new File(assetsDir, filename);
      try (FileOutputStream fos = new FileOutputStream(jsonFile)) {
        fos.write(root.toString(2).getBytes());
        Log.i(TAG, "Generated JSON with " + quotes.size() + " quotes");
        return true;
      }

    } catch (JSONException | IOException e) {
      Log.e(TAG, "Error generating JSON: " + e.getMessage());
      return false;
    }
  }

  public static String loadQuotesJSON(Context context, String filename) {
    try {
      File quotesDir = new File(context.getFilesDir(), "assets/quotes");
      File jsonFile = new File(quotesDir, filename);
      if (!jsonFile.exists()) {
        Log.i(TAG, "Custom quotes file not found, using default");
        return loadDefaultJSON(context);
      }

      StringBuilder json = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          json.append(line);
        }
      }
      return json.toString();

    } catch (IOException e) {
      Log.e(TAG, "Error loading quotes JSON: " + e.getMessage());
      return loadDefaultJSON(context);
    }
  }

  private static String loadDefaultJSON(Context context) {
    try {
      InputStream is = context.getAssets().open("bible/en/Verses/bible_verses.json");
      byte[] buffer = new byte[is.available()];
      is.read(buffer);
      is.close();
      return new String(buffer);
    } catch (IOException e) {
      Log.e(TAG, "Error loading default JSON: " + e.getMessage());
      return "{}";
    }
  }

  public static int getQuoteCount(String json) {
    try {
      JSONObject obj = new JSONObject(json);
      JSONArray quotes = obj.getJSONArray("all");
      return quotes.length();
    } catch (JSONException e) {
      return 158;
    }
  }
}