package com.correctsyntax.biblenotify;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProfileManagementActivity extends AppCompatActivity {

  private RecyclerView profilesRecyclerView;
  private Button addProfileButton, backButton;
  private ProfileManager profileManager;
  private ProfileAdapter profileAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.profile_management_activity);

    ConstraintLayout topbar = findViewById(R.id.profile_top_bar);

    ViewCompat.setOnApplyWindowInsetsListener(
        topbar,
        (v, windowInsets) -> {
          Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
          ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
          mlp.topMargin = insets.top;
          mlp.leftMargin = insets.left;
          mlp.bottomMargin = insets.bottom;
          mlp.rightMargin = insets.right;
          v.setLayoutParams(mlp);
          return WindowInsetsCompat.CONSUMED;
        });

    profileManager = new ProfileManager(this);
    
    profilesRecyclerView = findViewById(R.id.profiles_recycler_view);
    addProfileButton = findViewById(R.id.add_profile_button);
    backButton = findViewById(R.id.back_button);

    setupRecyclerView();
    loadProfiles();

    addProfileButton.setOnClickListener(v -> showAddProfileDialog());
    backButton.setOnClickListener(v -> finish());
  }

  private void setupRecyclerView() {
    profilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    profileAdapter = new ProfileAdapter();
    profilesRecyclerView.setAdapter(profileAdapter);
  }

  private void loadProfiles() {
    List<QuoteProfile> profiles = profileManager.getAllProfiles();
    profileAdapter.updateProfiles(profiles);
  }

  private void showAddProfileDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Add New Profile");
    
    View dialogView = LayoutInflater.from(this).inflate(R.layout.add_profile_dialog, null);
    builder.setView(dialogView);
    
    android.widget.EditText nameInput = dialogView.findViewById(R.id.profile_name_input);
    android.widget.EditText categoryInput = dialogView.findViewById(R.id.profile_category_input);
    android.widget.TimePicker timePicker = dialogView.findViewById(R.id.profile_time_picker);
    
    builder.setPositiveButton("Create", (dialog, which) -> {
      String name = nameInput.getText().toString().trim();
      String category = categoryInput.getText().toString().trim();
      
      if (name.isEmpty()) {
        name = "Custom Profile";
      }
      if (category.isEmpty()) {
        category = "General";
      }
      
      int hour, minute;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        hour = timePicker.getHour();
        minute = timePicker.getMinute();
      } else {
        hour = timePicker.getCurrentHour();
        minute = timePicker.getCurrentMinute();
      }
      
      profileManager.addProfile(name, category, hour, minute);
      loadProfiles();
    });
    
    builder.setNegativeButton("Cancel", null);
    builder.show();
  }

  private class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {
    private List<QuoteProfile> profiles;

    public void updateProfiles(List<QuoteProfile> newProfiles) {
      this.profiles = newProfiles;
      notifyDataSetChanged();
    }

    @Override
    public ProfileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.profile_item, parent, false);
      return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProfileViewHolder holder, int position) {
      QuoteProfile profile = profiles.get(position);
      holder.bind(profile);
    }

    @Override
    public int getItemCount() {
      return profiles != null ? profiles.size() : 0;
    }

    class ProfileViewHolder extends RecyclerView.ViewHolder {
      TextView nameText, timeText, categoryText;
      Button editButton, deleteButton;

      public ProfileViewHolder(View itemView) {
        super(itemView);
        nameText = itemView.findViewById(R.id.profile_name);
        timeText = itemView.findViewById(R.id.profile_time);
        categoryText = itemView.findViewById(R.id.profile_category);
        editButton = itemView.findViewById(R.id.edit_profile_button);
        deleteButton = itemView.findViewById(R.id.delete_profile_button);
      }

      public void bind(QuoteProfile profile) {
        nameText.setText(profile.name);
        timeText.setText(profile.getTimeString());
        categoryText.setText(profile.category);
        
        editButton.setOnClickListener(v -> editProfile(profile));
        deleteButton.setOnClickListener(v -> deleteProfile(profile));
      }
    }
  }

  private void editProfile(QuoteProfile profile) {
    // TODO: Implement edit profile dialog
  }

  private void deleteProfile(QuoteProfile profile) {
    new AlertDialog.Builder(this)
        .setTitle("Delete Profile")
        .setMessage("Are you sure you want to delete \"" + profile.name + "\"?")
        .setPositiveButton("Delete", (dialog, which) -> {
          profileManager.deleteProfile(profile.id);
          loadProfiles();
        })
        .setNegativeButton("Cancel", null)
        .show();
  }
}