package gsoc.google.com.physicalweblgpoireader.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;


import gsoc.google.com.physicalweblgpoireader.R;
import gsoc.google.com.physicalweblgpoireader.utils.Constants;
import gsoc.google.com.physicalweblgpoireader.utils.FragmentStackManager;

/**
 * Created by lgwork on 20/06/16.
 */
public class SettingsFragment extends Fragment {

    protected FragmentStackManager fragmentStackManager;
    private EditText adminPasswordInput1;
    private EditText adminPasswordInput2;
    private EditText lgIp;
    private EditText lgUser;
    private EditText lgPassword;
    private EditText lgSSH;
    private EditText lgKMLName;
    private EditText defaultVisitPoiDuration;

    private TextInputLayout password1;
    private TextInputLayout password2;

    Button saveChanges;
    Button cancelChanges;


    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem itemAbout = menu.findItem(R.id.action_about);
        itemAbout.setVisible(false);
        MenuItem itemSettings = menu.findItem(R.id.action_lg_settings);
        itemSettings.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentStackManager = FragmentStackManager.getInstance(getActivity());

        View rootView = inflater.inflate(R.layout.settings_layout, container, false);

        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
        String adminPassword = prefs.getString("password", "");
        String ipPrefs = prefs.getString("lgIP", "");

        String userPrefs = prefs.getString("lgUser", "lg");
        String lgPasswordPrefs = prefs.getString("lgPassword", "lqgalaxy");
        String lgSShPort = prefs.getString("lgPort", "22");

        String lgKMLNameStr = prefs.getString("lgKMLName", "BYOP.kml");

        String defaultPoisDurationPrefs = prefs.getString("defaultVisitPoiDuration", "10");



        adminPasswordInput1 = (EditText) rootView.findViewById(R.id.lg_password_1_input);
        if (!adminPassword.equals("")) adminPasswordInput1.setText(adminPassword);
        password1 = (TextInputLayout) rootView.findViewById(R.id.lg_password_1);

        adminPasswordInput2 = (EditText) rootView.findViewById(R.id.lg_password_2_input);
        if (!adminPassword.equals("")) adminPasswordInput2.setText(adminPassword);
        password2 = (TextInputLayout) rootView.findViewById(R.id.lg_password_2);

        lgIp = (EditText) rootView.findViewById(R.id.lg_ip_text);
        lgIp.setText(ipPrefs);

        lgUser = (EditText) rootView.findViewById(R.id.lg_user);
        lgUser.setText(userPrefs);

        lgPassword = (EditText) rootView.findViewById(R.id.lg_password);
        lgPassword.setText(lgPasswordPrefs);

        lgSSH =  (EditText) rootView.findViewById(R.id.lg_ssh);
        lgSSH.setText(lgSShPort);

        lgKMLName =  (EditText) rootView.findViewById(R.id.lg_defaultKML);
        lgKMLName.setText(lgKMLNameStr);


        defaultVisitPoiDuration =  (EditText) rootView.findViewById(R.id.defaultVisitPoiDuration);
        defaultVisitPoiDuration.setText(defaultPoisDurationPrefs);


        saveChanges = (Button) rootView.findViewById(R.id.btn_save_preferences);
        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Resources res = getActivity().getResources();
                if (!adminPasswordInput1.getText().toString().equals(adminPasswordInput2.getText().toString())) {
                    password1.setError(res.getString(R.string.passwords_no_match));
                    password2.setError(res.getString(R.string.passwords_no_match));
                } else {
                    password1.setErrorEnabled(false);
                    password2.setErrorEnabled(false);
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("password", adminPasswordInput1.getText() != null ? adminPasswordInput1.getText().toString() : "");
                    editor.putString("lgIP", lgIp.getText() != null ? lgIp.getText().toString() : "");
                    editor.putString("lgUser", lgUser.getText() != null ? lgUser.getText().toString() : "");
                    editor.putString("lgPassword", lgPassword.getText() != null ? lgPassword.getText().toString() : "");
                    editor.putString("lgPort", lgSSH.getText() != null ? lgSSH.getText().toString() : "");
                    editor.putString("lgKMLName", lgKMLName.getText() != null ? lgKMLName.getText().toString() : "");
                    editor.putString("defaultVisitPoiDuration", defaultVisitPoiDuration.getText() != null ? defaultVisitPoiDuration.getText().toString() : "");
                    editor.commit();

                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    fragmentStackManager.popBackStatFragment();
                }
            }
        });

        cancelChanges = (Button) rootView.findViewById(R.id.btn_cancel_preferences);
        cancelChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                fragmentStackManager.popBackStatFragment();
            }
        });


        return rootView;
    }
}
