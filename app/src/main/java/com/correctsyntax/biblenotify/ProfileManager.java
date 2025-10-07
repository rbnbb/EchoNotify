package com.correctsyntax.biblenotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {
    private static final String TAG = "ProfileManager";
    private static final String PREFS_KEY = "quote_profiles";
    private static final String PROFILES_JSON_KEY = "profiles_json";
    
    private Context context;
    private SharedPreferences prefs;
    
    public ProfileManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("bibleNotify", Context.MODE_PRIVATE);
    }
    
    public List<QuoteProfile> getAllProfiles() {
        List<QuoteProfile> profiles = new ArrayList<>();
        String profilesJson = prefs.getString(PROFILES_JSON_KEY, "");
        
        if (profilesJson.isEmpty()) {
            // Create default profile if none exist
            profiles.add(createDefaultProfile());
            saveProfiles(profiles);
            return profiles;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(profilesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject profileJson = jsonArray.getJSONObject(i);
                profiles.add(QuoteProfile.fromJSON(profileJson));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing profiles JSON: " + e);
            profiles.add(createDefaultProfile());
        }
        
        return profiles;
    }
    
    public void saveProfiles(List<QuoteProfile> profiles) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (QuoteProfile profile : profiles) {
                jsonArray.put(profile.toJSON());
            }
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROFILES_JSON_KEY, jsonArray.toString());
            editor.apply();
            
            Log.i(TAG, "Saved " + profiles.size() + " profiles");
        } catch (JSONException e) {
            Log.e(TAG, "Error saving profiles: " + e);
        }
    }
    
    public QuoteProfile addProfile(String name, String category, int hour, int minute) {
        List<QuoteProfile> profiles = getAllProfiles();
        
        // Generate unique ID
        String id = "profile_" + System.currentTimeMillis();
        
        QuoteProfile newProfile = new QuoteProfile(id, name, category, hour, minute);
        profiles.add(newProfile);
        
        saveProfiles(profiles);
        Log.i(TAG, "Added new profile: " + name);
        
        return newProfile;
    }
    
    public void deleteProfile(String profileId) {
        List<QuoteProfile> profiles = getAllProfiles();
        profiles.removeIf(profile -> profile.id.equals(profileId));
        saveProfiles(profiles);
        Log.i(TAG, "Deleted profile: " + profileId);
    }
    
    public void updateProfile(QuoteProfile updatedProfile) {
        List<QuoteProfile> profiles = getAllProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id.equals(updatedProfile.id)) {
                profiles.set(i, updatedProfile);
                break;
            }
        }
        saveProfiles(profiles);
        Log.i(TAG, "Updated profile: " + updatedProfile.name);
    }
    
    public List<QuoteProfile> getEnabledProfiles() {
        List<QuoteProfile> allProfiles = getAllProfiles();
        List<QuoteProfile> enabledProfiles = new ArrayList<>();
        
        for (QuoteProfile profile : allProfiles) {
            if (profile.enabled) {
                enabledProfiles.add(profile);
            }
        }
        
        return enabledProfiles;
    }
    
    private QuoteProfile createDefaultProfile() {
        return new QuoteProfile("default", "Daily Inspiration", "General", 9, 0);
    }
    
    // Get profile for a specific time (for scheduling)
    public QuoteProfile getProfileForTime(int hour, int minute) {
        List<QuoteProfile> profiles = getEnabledProfiles();
        
        for (QuoteProfile profile : profiles) {
            if (profile.hour == hour && profile.minute == minute) {
                return profile;
            }
        }
        
        return null;
    }
}