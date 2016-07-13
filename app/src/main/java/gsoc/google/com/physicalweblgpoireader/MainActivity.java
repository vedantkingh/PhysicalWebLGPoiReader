/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gsoc.google.com.physicalweblgpoireader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import gsoc.google.com.physicalweblgpoireader.PW.NearbyBeaconsFragment;
import gsoc.google.com.physicalweblgpoireader.PW.ScreenListenerService;
import gsoc.google.com.physicalweblgpoireader.utils.FragmentStackManager;

/**
 * The main entry point for the app.
 */

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String NEARBY_BEACONS_FRAGMENT_TAG = "NearbyBeaconsFragmentTag";

    private FragmentStackManager fragmentStackManager;

    public static AlertDialog.Builder closeDialog(Context context, DialogInterface.OnClickListener accept, DialogInterface.OnClickListener cancel) {
        final AlertDialog.Builder closeDialog = new AlertDialog.Builder(context);
        closeDialog.setTitle(context.getResources().getString(R.string.close));
        closeDialog.setMessage(context.getResources().getString(R.string.close_body));
        closeDialog.setPositiveButton(context.getResources().getString(R.string.accept), accept);
        closeDialog.setNegativeButton(context.getResources().getString(R.string.cancel), cancel);
        return closeDialog;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentStackManager = FragmentStackManager.getInstance(this);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager != null ? btManager.getAdapter() : null;
        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    R.string.error_bluetooth_support, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ensureBluetoothIsEnabled(btAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Ensures Bluetooth is available on the beacon and it is enabled. If not,
     * displays a dialog requesting user permission to enable Bluetooth.
     */
    private void ensureBluetoothIsEnabled(BluetoothAdapter bluetoothAdapter) {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        showNearbyBeaconsFragment();
        Intent intent = new Intent(this, ScreenListenerService.class);
        startService(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                showNearbyBeaconsFragment();
                Intent intent = new Intent(this, ScreenListenerService.class);
                startService(intent);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onBackPressed() {
        try {
            if (!fragmentStackManager.popBackStatFragment()) {
                closeDialog(MainActivity.this, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                        System.exit(0);
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }


    private void showNearbyBeaconsFragment() {
        NearbyBeaconsFragment fragment = new NearbyBeaconsFragment();
        fragmentStackManager.loadFragment(fragment, R.id.main_frame);
    }

}
