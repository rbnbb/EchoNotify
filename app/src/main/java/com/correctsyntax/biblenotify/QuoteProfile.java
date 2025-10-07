package com.correctsyntax.biblenotify;

import org.json.JSONException;
import org.json.JSONObject;

public class QuoteProfile {
    public String id;           // Unique identifier
    public String name;         // Display name like "Morning Motivation" 
    public String category;     // Category for organizing quotes
    public int hour;           // Notification hour (24-hour format)
    public int minute;         // Notification minute
    public boolean enabled;    // Whether notifications are enabled
    public String quotesFile;  // Filename containing quotes for this profile
    public String originalFileName; // Original imported file name (for display)
    public boolean[] selectedDays; // Days of week: [Sun, Mon, Tue, Wed, Thu, Fri, Sat]
    
    public QuoteProfile() {
        // Default constructor
    }
    
    public QuoteProfile(String id, String name, String category, int hour, int minute) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.hour = hour;
        this.minute = minute;
        this.enabled = true;
        this.quotesFile = id + "_quotes.json";
        // Default: all days enabled
        this.selectedDays = new boolean[]{true, true, true, true, true, true, true};
    }
    
    // Convert to JSON for storage
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("category", category);
        json.put("hour", hour);
        json.put("minute", minute);
        json.put("enabled", enabled);
        json.put("quotesFile", quotesFile);
        if (originalFileName != null) {
            json.put("originalFileName", originalFileName);
        }
        // Store selectedDays as JSON array
        if (selectedDays != null) {
            org.json.JSONArray daysArray = new org.json.JSONArray();
            for (boolean day : selectedDays) {
                daysArray.put(day);
            }
            json.put("selectedDays", daysArray);
        }
        return json;
    }
    
    // Create from JSON
    public static QuoteProfile fromJSON(JSONObject json) throws JSONException {
        QuoteProfile profile = new QuoteProfile();
        profile.id = json.getString("id");
        profile.name = json.getString("name");
        profile.category = json.getString("category");
        profile.hour = json.getInt("hour");
        profile.minute = json.getInt("minute");
        profile.enabled = json.getBoolean("enabled");
        profile.quotesFile = json.getString("quotesFile");
        if (json.has("originalFileName")) {
            profile.originalFileName = json.getString("originalFileName");
        }
        // Load selectedDays from JSON array
        if (json.has("selectedDays")) {
            org.json.JSONArray daysArray = json.getJSONArray("selectedDays");
            profile.selectedDays = new boolean[7];
            for (int i = 0; i < 7 && i < daysArray.length(); i++) {
                profile.selectedDays[i] = daysArray.getBoolean(i);
            }
        } else {
            // Default: all days enabled for backwards compatibility
            profile.selectedDays = new boolean[]{true, true, true, true, true, true, true};
        }
        return profile;
    }
    
    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }
    
    // Helper method to get readable days string
    public String getDaysString() {
        if (selectedDays == null) {
            return "Every day";
        }
        
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        java.util.ArrayList<String> enabledDays = new java.util.ArrayList<>();
        
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                enabledDays.add(dayNames[i]);
            }
        }
        
        if (enabledDays.size() == 7) {
            return "Every day";
        } else if (enabledDays.size() == 0) {
            return "No days selected";
        } else {
            return String.join(", ", enabledDays);
        }
    }
    
    // Check if profile should run today
    public boolean isEnabledForDay(int dayOfWeek) {
        if (selectedDays == null) {
            return true; // Default to every day
        }
        // dayOfWeek: Calendar.SUNDAY = 1, Calendar.MONDAY = 2, etc.
        // selectedDays: [Sun=0, Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6]
        int index = dayOfWeek - 1; // Convert Calendar day to array index
        return index >= 0 && index < selectedDays.length && selectedDays[index];
    }
}