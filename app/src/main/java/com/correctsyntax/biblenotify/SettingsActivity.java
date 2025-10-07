package com.correctsyntax.biblenotify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

  TimePicker timePicker;
  ImageButton help;
  Button returnButton, saveButton, importButton, testButton, manageProfilesButton;
  Spinner profileSpinner;
  TextView currentFileName, daysToggle;
  LinearLayout daysContainer;
  CheckBox[] dayCheckboxes;
  ProfileManager profileManager;
  QuoteProfile currentProfile;
  private boolean isImportingFile = false;
  private boolean daysExpanded = false;

  public static int hour = 12;
  public static int min = 0;

  public static int hourToSet = 12;
  public static int minToSet = 0;

  private ActivityResultLauncher<Intent> filePickerLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);

    ConstraintLayout topbar = findViewById(R.id.settings_top_bar);

    ViewCompat.setOnApplyWindowInsetsListener(
        topbar,
        (v, windowInsets) -> {
          Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
          // Apply the insets as a margin to the view. This solution sets only the
          // bottom, left, and right dimensions, but you can apply whichever insets are
          // appropriate to your layout. You can also update the view padding if that's
          // more appropriate.
          ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
          mlp.topMargin = insets.top;
          mlp.leftMargin = insets.left;
          mlp.bottomMargin = insets.bottom;
          mlp.rightMargin = insets.right;
          v.setLayoutParams(mlp);

          // Return CONSUMED if you don't want want the window insets to keep passing
          // down to descendant views.
          return WindowInsetsCompat.CONSUMED;
        });

    timePicker = findViewById(R.id.time_picker);
    saveButton = findViewById(R.id.set_time_button);
    returnButton = findViewById(R.id.return_button);
    help = findViewById(R.id.set_help_button);
    importButton = findViewById(R.id.import_quotes_button);
    testButton = findViewById(R.id.test_notification_button);
    manageProfilesButton = findViewById(R.id.manage_profiles_button);
    profileSpinner = findViewById(R.id.profile_spinner);
    currentFileName = findViewById(R.id.current_file_name);
    daysToggle = findViewById(R.id.days_toggle);
    daysContainer = findViewById(R.id.days_container);
    
    // Initialize day checkboxes array
    dayCheckboxes = new CheckBox[]{
        findViewById(R.id.cb_sunday),
        findViewById(R.id.cb_monday),
        findViewById(R.id.cb_tuesday),
        findViewById(R.id.cb_wednesday),
        findViewById(R.id.cb_thursday),
        findViewById(R.id.cb_friday),
        findViewById(R.id.cb_saturday)
    };
    
    profileManager = new ProfileManager(this);

    setupFilePickerLauncher();
    setupProfileSpinner();

    final SharedPreferences sharedPreferences =
        getApplicationContext().getSharedPreferences("bibleNotify", MODE_PRIVATE);

    // Initialize with current profile's time
    loadCurrentProfileSettings();

    // Time picker
    timePicker.setOnTimeChangedListener(
        (timePicker, h, m) -> {
          hour = h;
          min = m;
        });

    // Save Button - now saves to current profile
    saveButton.setOnClickListener(
        v -> {
          if (currentProfile != null) {
            // Update current profile's time
            currentProfile.hour = hour;
            currentProfile.minute = min;
            
            // Save day selections to profile
            for (int i = 0; i < dayCheckboxes.length && i < currentProfile.selectedDays.length; i++) {
              currentProfile.selectedDays[i] = dayCheckboxes[i].isChecked();
            }
            
            profileManager.updateProfile(currentProfile);
            
            // Also update SharedPreferences for alarm system
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("SetTimeH", hour);
            editor.putInt("SetTimeM", min);
            editor.apply();

            // Pass the current profile to the alarm system
            editor.putString("currentActiveProfile", currentProfile.id);
            editor.apply();
            
            SetAlarm.startAlarmBroadcastReceiver(SettingsActivity.this, sharedPreferences);

            Toast.makeText(getApplicationContext(), R.string.time_saved_toast, Toast.LENGTH_SHORT)
                .show();
            // Don't call finish() - keep settings open
          } else {
            Toast.makeText(this, "Please select a profile first", Toast.LENGTH_SHORT).show();
          }
        });

    // Return Button - only this button closes settings
    returnButton.setOnClickListener(v -> finish());

    // Help
    help.setOnClickListener(
        v -> {
          Intent help_Intent = new Intent(SettingsActivity.this, HelpActivity.class);
          startActivity(help_Intent);
        });

    // Import Quotes Button
    importButton.setOnClickListener(v -> openFilePicker());
    
    // Test Notification Button
    testButton.setOnClickListener(v -> testNotificationNow());
    
    // Manage Profiles Button
    manageProfilesButton.setOnClickListener(v -> {
      Intent profileIntent = new Intent(SettingsActivity.this, ProfileManagementActivity.class);
      startActivity(profileIntent);
    });
    
    // Days toggle functionality
    daysToggle.setOnClickListener(v -> toggleDaysSection());
    
    // Set up day checkbox listeners
    for (int i = 0; i < dayCheckboxes.length; i++) {
      final int dayIndex = i;
      dayCheckboxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (currentProfile != null) {
          currentProfile.selectedDays[dayIndex] = isChecked;
          updateDaysToggleText();
        }
      });
    }
  }

  private void setupFilePickerLauncher() {
    filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
              importQuotesFromFile(uri);
            }
          }
        });
  }

  private void openFilePicker() {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("text/plain");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    filePickerLauncher.launch(Intent.createChooser(intent, "Select quotes text file"));
  }
  
  private String getFileName(Uri uri) {
    String result = null;
    if (uri.getScheme().equals("content")) {
      try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
          int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
          if (nameIndex != -1) {
            result = cursor.getString(nameIndex);
          }
        }
      }
    }
    if (result == null) {
      result = uri.getPath();
      int cut = result.lastIndexOf('/');
      if (cut != -1) {
        result = result.substring(cut + 1);
      }
    }
    return result;
  }

  private void importQuotesFromFile(Uri uri) {
    if (currentProfile == null) {
      Toast.makeText(this, "Please select a profile first", Toast.LENGTH_SHORT).show();
      return;
    }
    
    isImportingFile = true;
    try {
      InputStream inputStream = getContentResolver().openInputStream(uri);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\n");
      }
      reader.close();

      // Use current profile's category
      List<QuoteParser.Quote> quotes = parseQuotesFromText(content.toString(), currentProfile.category);
      android.util.Log.i("SettingsActivity", "Parsed " + quotes.size() + " quotes from file");
      
      if (!quotes.isEmpty()) {
        String fileName = "quotes_" + currentProfile.id + ".json";
        boolean success = QuoteParser.generateQuoteJSON(this, quotes, fileName);
        android.util.Log.i("SettingsActivity", "JSON generation success: " + success);
        
        if (success) {
          // Save file names to profile
          String originalName = getFileName(uri);
          currentProfile.quotesFile = fileName;
          currentProfile.originalFileName = originalName;
          profileManager.updateProfile(currentProfile);
          
          // Update global preferences for notification system
          SharedPreferences sharedPreferences = getSharedPreferences("bibleNotify", MODE_PRIVATE);
          SharedPreferences.Editor editor = sharedPreferences.edit();
          editor.putString("currentQuoteFile", fileName);
          editor.putInt("totalQuotes", quotes.size());
          editor.putInt("currentVerseNumber", 0); // Reset counter for new quotes
          editor.apply();
          
          // Update file name display
          updateFileNameDisplay();
          
          android.util.Log.i("SettingsActivity", "Saved " + quotes.size() + " quotes to profile " + currentProfile.name);
          Toast.makeText(this, "Imported " + quotes.size() + " quotes to \"" + currentProfile.name + "\" successfully!", 
                        Toast.LENGTH_LONG).show();
          isImportingFile = false;
        } else {
          Toast.makeText(this, "Failed to import quotes", Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, "No valid quotes found in file", Toast.LENGTH_SHORT).show();
      }
      
    } catch (Exception e) {
      Toast.makeText(this, "Error importing file: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
      isImportingFile = false;
    }
  }

  private List<QuoteParser.Quote> parseQuotesFromText(String text, String category) {
    List<QuoteParser.Quote> quotes = new java.util.ArrayList<>();
    String[] lines = text.split("\n");
    
    for (String line : lines) {
      line = line.trim();
      if (!line.isEmpty()) {
        String cleanedQuote = cleanQuoteText(line);
        if (!cleanedQuote.isEmpty()) {
          quotes.add(new QuoteParser.Quote(cleanedQuote, category));
        }
      }
    }
    
    return quotes;
  }

  private String cleanQuoteText(String text) {
    text = text.trim();
    text = text.replaceAll("^\\d+\\.\\s*", "");
    text = text.replaceAll("^\\d+\\s*", "");
    text = text.replaceAll("^-\\s*", "");
    text = text.replaceAll("^•\\s*", "");
    return text.trim();
  }

  private void testNotificationNow() {
    // Show current time for debugging
    java.util.Calendar now = java.util.Calendar.getInstance();
    String currentTime = String.format("Current time: %02d:%02d", 
        now.get(java.util.Calendar.HOUR_OF_DAY), 
        now.get(java.util.Calendar.MINUTE));
    
    // Create and send a test notification immediately
    AlarmBroadcastReceiver receiver = new AlarmBroadcastReceiver();
    receiver.onReceive(this, null);
    Toast.makeText(this, "Test notification sent! " + currentTime, 
                  Toast.LENGTH_LONG).show();
  }
  
  private void setupProfileSpinner() {
    List<QuoteProfile> profiles = profileManager.getAllProfiles();
    
    // Create default profile if none exist
    if (profiles.isEmpty()) {
      profileManager.addProfile("Default", "General", 8, 0);
      profiles = profileManager.getAllProfiles();
    }
    
    // Make profiles effectively final for inner class access
    final List<QuoteProfile> finalProfiles = profiles;
    
    // Create adapter with profile names
    java.util.ArrayList<String> profileNames = new java.util.ArrayList<>();
    for (QuoteProfile profile : finalProfiles) {
      profileNames.add(profile.name);
    }
    
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
        android.R.layout.simple_spinner_item, profileNames);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    profileSpinner.setAdapter(adapter);
    
    // Set selection change listener
    profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
        if (position < finalProfiles.size()) {
          currentProfile = finalProfiles.get(position);
          loadProfileSettings();
        }
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
      }
    });
    
    // Select first profile by default
    if (!finalProfiles.isEmpty()) {
      currentProfile = finalProfiles.get(0);
      profileSpinner.setSelection(0);
    }
  }
  
  private void loadCurrentProfileSettings() {
    if (currentProfile == null) {
      setupProfileSpinner(); // This will set currentProfile
    }
    
    if (currentProfile != null) {
      loadProfileSettings();
    }
  }
  
  private void loadProfileSettings() {
    if (currentProfile == null) return;
    
    // Load time from profile
    hourToSet = currentProfile.hour;
    minToSet = currentProfile.minute;
    hour = hourToSet;
    min = minToSet;
    
    // Update time picker
    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
      timePicker.setCurrentHour(hourToSet);
      timePicker.setCurrentMinute(minToSet);
    } else {
      timePicker.setHour(hourToSet);
      timePicker.setMinute(minToSet);
    }
    
    // Update file name display
    updateFileNameDisplay();
    
    // Update day checkboxes
    updateDaysCheckboxes();
    
    // Update global settings for notification system
    SharedPreferences sharedPreferences = getSharedPreferences("bibleNotify", MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString("currentQuoteFile", currentProfile.quotesFile);
    editor.apply();
  }
  
  private void updateFileNameDisplay() {
    if (currentProfile != null && currentProfile.originalFileName != null && !currentProfile.originalFileName.isEmpty()) {
      currentFileName.setText("File: " + currentProfile.originalFileName);
      currentFileName.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    } else {
      currentFileName.setText("No custom file imported");
      currentFileName.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    // Don't reset profile during file import
    if (isImportingFile) {
      return;
    }
    
    // Refresh profile list in case profiles were added/removed, but preserve selection
    String selectedProfileName = null;
    if (currentProfile != null) {
      selectedProfileName = currentProfile.name;
    }
    
    setupProfileSpinner();
    
    // Restore selection if possible
    if (selectedProfileName != null) {
      ArrayAdapter<String> adapter = (ArrayAdapter<String>) profileSpinner.getAdapter();
      for (int i = 0; i < adapter.getCount(); i++) {
        if (adapter.getItem(i).equals(selectedProfileName)) {
          profileSpinner.setSelection(i);
          break;
        }
      }
    }
  }
  
  private void toggleDaysSection() {
    daysExpanded = !daysExpanded;
    if (daysExpanded) {
      daysContainer.setVisibility(View.VISIBLE);
      daysToggle.setText("▲ Days (Optional)");
    } else {
      daysContainer.setVisibility(View.GONE);
      daysToggle.setText("▼ Days (Optional)");
    }
  }
  
  private void updateDaysCheckboxes() {
    if (currentProfile != null && currentProfile.selectedDays != null && dayCheckboxes != null) {
      for (int i = 0; i < dayCheckboxes.length && i < currentProfile.selectedDays.length; i++) {
        dayCheckboxes[i].setChecked(currentProfile.selectedDays[i]);
      }
    }
    updateDaysToggleText();
  }
  
  private void updateDaysToggleText() {
    if (currentProfile != null) {
      String daysText = currentProfile.getDaysString();
      if (daysExpanded) {
        daysToggle.setText("▲ Days: " + daysText);
      } else {
        daysToggle.setText("▼ Days: " + daysText);
      }
    }
  }
}
