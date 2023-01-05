package com.example.testapp;

import android.content.pm.LauncherActivityInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.testapp.databinding.ActivityMain2Binding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    private ActivityMain2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main2);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        TextView textView = new TextView(this);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

//    //add app to workspace
//    private void verifyApplications() {
//        final Contextcontext mApp.getContext();
//        ArrayList<Pair<ItemInfo, Object>> installQueue new ArrayList<>();
//        final List<UserHandle> profiles mUserManager.getUserProfiles();
//        for (UserHandle user profiles) {
//            final List<LauncherActivityInfo> apps mLauncherApps.getActivityList(null, user);
//            ArrayList<InstallShortcutReceiver.PendingInstallShortcutInfo> added new ArrayList< ~ > ();
//            synchronized (this) {
//                for (LauncherActivityInfo app apps) {
//                    InstallShortcutReceiver.PendingInstallShortcutInfo pendingInstallShortcutInfo
//                    new InstallShortcutReceiver.PendingInstallShortcutInfo(app, context);
//                    added.add(pendingInstallShortcutInfo);
//                    installQueue.add(pendingInstallShortcutInfo.getItemInfo());
//                    if (added.isEmpty()) {
//                        mApp.getModel().addAndBindAddedWorkspaceItems(installQueue);
//                    }
//                }
//            }
//        }
//    }

}