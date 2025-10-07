package com.correctsyntax.biblenotify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
  // Will be updated dynamically based on current quote file
  int NUM_OF_VERSES = 158;

  // Notification
  String CHANNEL_ID = "echoNotify";
  NotificationChannel notificationChannel;
  CharSequence name = "EchoNotify";
  // make a random number
  Random rand = new Random();
  int randomNum = 0;

  String languagePath = "en";

  @Override
  public void onReceive(Context context, Intent intent) {
    // build and show notification
    Log.i("AlarmBroadcastReceiver", "=== onReceive STARTED ===");
    Log.i("AlarmBroadcastReceiver", "onReceive triggered at " + new java.util.Date().toString());

    final SharedPreferences sharedPreferences = context.getSharedPreferences("bibleNotify", 0);
    
    // Check if today is a valid day for the active profile
    ProfileManager profileManager = new ProfileManager(context);
    java.util.Calendar now = java.util.Calendar.getInstance();
    int currentDayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK);
    
    String activeProfileId = sharedPreferences.getString("currentActiveProfile", null);
    QuoteProfile activeProfile = null;
    
    if (activeProfileId != null) {
      java.util.List<QuoteProfile> allProfiles = profileManager.getAllProfiles();
      for (QuoteProfile profile : allProfiles) {
        if (profile.id.equals(activeProfileId)) {
          activeProfile = profile;
          break;
        }
      }
    }
    
    // Fallback to any enabled profile if no active profile set
    if (activeProfile == null) {
      java.util.List<QuoteProfile> enabledProfiles = profileManager.getEnabledProfiles();
      if (!enabledProfiles.isEmpty()) {
        activeProfile = enabledProfiles.get(0);
      }
    }
    
    boolean shouldShowNotification = false;
    if (activeProfile != null && activeProfile.isEnabledForDay(currentDayOfWeek)) {
      shouldShowNotification = true;
      Log.i("AlarmBroadcastReceiver", "Notification allowed for active profile: " + activeProfile.name + " on day " + currentDayOfWeek);
    }
    
    if (!shouldShowNotification) {
      Log.i("AlarmBroadcastReceiver", "Active profile " + (activeProfile != null ? activeProfile.name : "none") + " does not allow notifications today (day " + currentDayOfWeek + "), skipping notification");
      // Still reschedule for next valid day
      android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
      handler.postDelayed(() -> {
        Log.i("AlarmBroadcastReceiver", "Rescheduling for next valid day");
        SetAlarm.startAlarmBroadcastReceiver(context, sharedPreferences);
      }, 2000);
      return;
    }

    // Get the actual quote count from the JSON file
    NUM_OF_VERSES = getActualQuoteCount(context);
    Log.i("AlarmBroadcastReceiver", "Total quotes available: " + NUM_OF_VERSES);

    // Random verse algorithm
    if (NUM_OF_VERSES <= 0) {
      Log.e("AlarmBroadcastReceiver", "No quotes available!");
      return;
    }
    
    if (rand.nextInt(3) < 1) {
      // Sequential algorithm - cycle through quotes
      SharedPreferences.Editor editor = sharedPreferences.edit();

      // lang
      languagePath = sharedPreferences.getString("languagePath", "en");

      int verseNumber = 0;
      if (sharedPreferences.contains("currentVerseNumber")) {
        verseNumber = sharedPreferences.getInt("currentVerseNumber", 0);
      }
      
      // Keep verseNumber within bounds
      if (verseNumber >= NUM_OF_VERSES) {
        verseNumber = 0; // Reset to beginning
      }
      
      editor.putInt("currentVerseNumber", verseNumber + 1);
      editor.apply();

      randomNum = verseNumber;

    } else {
      // Random algorithm - pick any quote
      randomNum = rand.nextInt(NUM_OF_VERSES);
    }
    
    Log.i("AlarmBroadcastReceiver", "Selected quote index: " + randomNum + " out of " + NUM_OF_VERSES);

    try {
      Log.i("AlarmBroadcastReceiver", "Getting all quote data for index: " + randomNum);
      
      // Load JSON once and get all data atomically
      String json = loadJSONFromAsset(context);
      JSONObject obj = new JSONObject(json);
      JSONArray userArray = obj.getJSONArray("all");
      
      // Final safety check
      if (randomNum >= userArray.length()) {
        Log.w("AlarmBroadcastReceiver", "Index " + randomNum + " out of bounds for array size " + userArray.length() + ". Using index 0.");
        randomNum = 0;
      }
      
      JSONObject quoteDetail = userArray.getJSONObject(randomNum);
      String verse = quoteDetail.getString("verse");
      String place = quoteDetail.getString("place");  
      String data = quoteDetail.getString("data");
      
      Log.i("AlarmBroadcastReceiver", "Got verse: " + verse);
      Log.i("AlarmBroadcastReceiver", "Got place: " + place);
      Log.i("AlarmBroadcastReceiver", "Got data: " + data);
      
      // Determine notification title based on current profile or category  
      String notificationTitle;
      if (place.equals("Custom")) {
        // Try to get profile information for better title
        List<QuoteProfile> profiles = profileManager.getEnabledProfiles();
        if (!profiles.isEmpty()) {
          // For now, use the first enabled profile's name
          notificationTitle = profiles.get(0).name;
        } else {
          notificationTitle = "Daily Inspiration";
        }
      } else {
        notificationTitle = place;
      }
      
      showNotification(context, verse, place, data, notificationTitle);

    } catch (Exception e) {
      Log.e("AlarmBroadcastReceiver", "Exception in notification process: " + e.toString());
      e.printStackTrace();
      Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
    }

    // Start a new alarm after a brief delay to avoid timing conflicts
    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    handler.postDelayed(() -> {
      Log.i("AlarmBroadcastReceiver", "Rescheduling next alarm");
      SetAlarm.startAlarmBroadcastReceiver(context, sharedPreferences);
    }, 2000); // 2 second delay
  }

  // build Notification  
  public void showNotification(Context context, String bibleText, String bibleVerse, String data) {
    this.showNotification(context, bibleText, bibleVerse, data, "Daily Inspiration");
  }
  
  // Enhanced notification with custom title
  public void showNotification(Context context, String bibleText, String bibleVerse, String data, String customTitle) {
    Intent notificationIntent = new Intent(context, BibleReader.class);
    Bundle bundle = new Bundle();
    notificationIntent.putExtras(bundle);
    notificationIntent.setFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

    PendingIntent contentIntent;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      contentIntent =
          PendingIntent.getActivity(
              context,
              0,
              notificationIntent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    } else {
      contentIntent =
          PendingIntent.getActivity(
              context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationChannel =
          new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
    }

    NotificationManager mNotificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mNotificationManager.createNotificationChannel(notificationChannel);
    }

    /* save verse data(date) so we know what
    to show when user opens reader  */
    final SharedPreferences sharedPreferences = context.getSharedPreferences("bibleNotify", 0);

    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString("readerData", data);
    
    // Handle both Bible verse format (Book Chapter:Verse) and custom format (Category)
    String verseNumber = "";
    if (bibleVerse.contains(":")) {
      String[] parts = bibleVerse.split(":");
      if (parts.length > 1) {
        verseNumber = parts[1].replace(" (story)", "");
      }
    } else {
      // For custom quotes without verse numbers, use empty string
      verseNumber = "";
    }
    editor.putString("readerDataVerseNumber", verseNumber);
    editor.apply();

    // More for Notification
    Notification.BigTextStyle bigText = new Notification.BigTextStyle();
    bigText.bigText(bibleText);
    bigText.setSummaryText(bibleVerse);

    Notification.Builder NotificationBuilder;

    // check Android API and do as needed

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationBuilder = new Notification.Builder(context, CHANNEL_ID);
    } else {
      NotificationBuilder = new Notification.Builder(context);
    }
    Notification.Builder mBuilder = NotificationBuilder;

    mBuilder.setSmallIcon(R.drawable.nicon);
    mBuilder.setContentTitle(customTitle);  // Use custom title instead of fixed string
    mBuilder.setContentText(bibleText);     // Show quote text instead of category
    mBuilder.setStyle(bigText);
    mBuilder.setAutoCancel(true);
    mBuilder.setContentIntent(contentIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mBuilder.setChannelId(CHANNEL_ID);
    }

    mNotificationManager.notify(1, mBuilder.build());
  }

  //  Parse Json file to get verse data
  public String pickBibleVerse(Context context, String whichPart) {
    String name;

    try {
      // get JSONObject from JSON file
      JSONObject obj = new JSONObject(loadJSONFromAsset(context));

      // fetch JSONArray named users
      JSONArray userArray = obj.getJSONArray("all");
      
      // Safety check: ensure randomNum is within actual array bounds
      int actualArraySize = userArray.length();
      Log.i("AlarmBroadcastReceiver", "JSON array actual size: " + actualArraySize + ", trying to access index: " + randomNum);
      
      if (randomNum >= actualArraySize) {
        Log.w("AlarmBroadcastReceiver", "Index out of bounds! Using index 0 instead");
        randomNum = 0;
      }

      try {
        JSONObject userDetail = userArray.getJSONObject(randomNum);
        name = userDetail.getString(whichPart);

      } catch (JSONException e) {
        Log.e("AlarmBroadcastReceiver", "Error accessing quote at index " + randomNum + ": " + e);
        return "ERROR: " + e;
      }

    } catch (JSONException e) {
      Log.e("AlarmBroadcastReceiver", "Error parsing JSON: " + e);
      return "ERROR: " + e;
    }

    return name;
  }

  // load json file from App Asset or custom quotes
  public String loadJSONFromAsset(Context context) {
    SharedPreferences prefs = context.getSharedPreferences("bibleNotify", 0);
    String customQuoteFile = prefs.getString("currentQuoteFile", null);
    
    // Try to load custom quotes first
    if (customQuoteFile != null) {
      Log.i("AlarmBroadcastReceiver", "Attempting to load custom quotes from: " + customQuoteFile);
      String customJson = QuoteParser.loadQuotesJSON(context, customQuoteFile);
      if (customJson != null && !customJson.equals("{}")) {
        Log.i("AlarmBroadcastReceiver", "Successfully loaded custom quotes JSON");
        Log.i("AlarmBroadcastReceiver", "JSON content: " + customJson.substring(0, Math.min(200, customJson.length())) + "...");
        return customJson;
      } else {
        Log.w("AlarmBroadcastReceiver", "Custom quotes file empty or null, falling back to default");
      }
    }
    
    // Fallback to default bible verses
    String json;
    try {
      InputStream is =
          context.getAssets().open("bible/" + languagePath + "/Verses/bible_verses.json");
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      json = new String(buffer, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      Log.d("ERROR", String.valueOf(ex));
      Toast.makeText(context, "Quote files not found: " + ex, Toast.LENGTH_SHORT).show();
      return null;
    }
    return json;
  }

  // Get the actual number of quotes from the JSON file
  private int getActualQuoteCount(Context context) {
    try {
      String json = loadJSONFromAsset(context);
      if (json == null || json.equals("{}")) {
        return 0;
      }
      
      JSONObject obj = new JSONObject(json);
      JSONArray quotesArray = obj.getJSONArray("all");
      int count = quotesArray.length();
      Log.i("AlarmBroadcastReceiver", "Actual quote count from JSON: " + count);
      return count;
      
    } catch (JSONException e) {
      Log.e("AlarmBroadcastReceiver", "Error counting quotes: " + e);
      return 158; // Default fallback
    }
  }
}
