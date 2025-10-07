package com.correctsyntax.biblenotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Date;

public class SetAlarm {
  private static final String TAG = "SetAlarm";

  public static int hour = 12;
  public static int min = 0;

  public static void startAlarmBroadcastReceiver(
      Context context, SharedPreferences sharedPreferences) {

    // get time
    if (sharedPreferences.contains("SetTimeH")) {
      min = sharedPreferences.getInt("SetTimeM", 0);
      hour = sharedPreferences.getInt("SetTimeH", 0);
    }
    
    Log.i(TAG, "Setting alarm for " + hour + ":" + String.format("%02d", min));

    // Start Alarm
    Intent _intent = new Intent(context, AlarmBroadcastReceiver.class);
    PendingIntent pendingIntent;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      pendingIntent =
          PendingIntent.getBroadcast(
              context,
              0,
              _intent,
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    } else {
      pendingIntent =
          PendingIntent.getBroadcast(context, 0, _intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    alarmManager.cancel(pendingIntent);
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, min);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    // Check active profile and find next valid notification day
    ProfileManager profileManager = new ProfileManager(context);
    String activeProfileId = sharedPreferences.getString("currentActiveProfile", null);
    Calendar nextValidDay = getNextValidNotificationDay(calendar, profileManager, activeProfileId);
    
    Log.i(TAG, "Current time: " + Calendar.getInstance().getTime().toString());
    Log.i(TAG, "Next valid notification: " + nextValidDay.getTime().toString());

    boolean canScheduleExactAlarms;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      canScheduleExactAlarms = alarmManager.canScheduleExactAlarms();
    } else {
      canScheduleExactAlarms = true;
    }

    // SDK 18 and below
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      alarmManager.set(AlarmManager.RTC_WAKEUP, nextValidDay.getTimeInMillis(), pendingIntent);
    }
    // SDK 19 to 22
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextValidDay.getTimeInMillis(), pendingIntent);
    }
    // SDK 23 +
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!canScheduleExactAlarms) {
        Log.e(TAG, "Cannot schedule exact alarms - permission not granted");
        Toast.makeText(context, "Exact alarm permission not granted. Notifications may be delayed.", Toast.LENGTH_LONG).show();
        return;
      }
      alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP, nextValidDay.getTimeInMillis(), pendingIntent);
      Log.i(TAG, "Exact alarm set with setExactAndAllowWhileIdle");
    }
  }

  // Helper method to find next valid day for notification based on active profile
  private static Calendar getNextValidNotificationDay(Calendar baseTime, ProfileManager profileManager, String activeProfileId) {
    Calendar testCalendar = Calendar.getInstance();
    testCalendar.setTimeInMillis(baseTime.getTimeInMillis());
    Date now = new Date();
    
    // Get the active profile
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
    
    // If no active profile found, fallback to any enabled profile
    if (activeProfile == null) {
      java.util.List<QuoteProfile> enabledProfiles = profileManager.getEnabledProfiles();
      if (!enabledProfiles.isEmpty()) {
        activeProfile = enabledProfiles.get(0);
      }
    }
    
    // If still no profile, return original time
    if (activeProfile == null) {
      Log.w(TAG, "No active profile found, using original time");
      return baseTime;
    }
    
    Log.i(TAG, "Using profile for scheduling: " + activeProfile.name);
    
    // If time has passed today, start checking from tomorrow
    if (testCalendar.getTime().compareTo(now) <= 0) {
      testCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }
    
    // Check up to 7 days ahead to find a valid day for this specific profile
    for (int daysAhead = 0; daysAhead < 7; daysAhead++) {
      int dayOfWeek = testCalendar.get(Calendar.DAY_OF_WEEK);
      
      if (activeProfile.isEnabledForDay(dayOfWeek)) {
        Log.i(TAG, "Found valid notification day: " + testCalendar.getTime().toString() + " for profile: " + activeProfile.name);
        return testCalendar;
      }
      
      // Move to next day if profile doesn't allow notifications today
      testCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }
    
    // Fallback: if no valid day found, return the original time (shouldn't happen)
    Log.w(TAG, "No valid notification day found in next 7 days for profile " + activeProfile.name + ", using original time");
    return baseTime;
  }
}
