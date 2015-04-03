/*
 * Copyright (C) 2014 The Android Open Source Project
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

package si.osbeorn.ct_challenge_2015.connected_cars;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import si.osbeorn.ct_challenge_2015.connected_cars.application.ConnectedCarsApplication;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity implements RecognitionListener
{
    /**
     * Tag for Log
     */
    private static final String TAG = "DeviceListActivity";

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;
    private SpeechRecognizerService recognizer;
    private Map<String, Integer> textToNumberMap;
    private ListView pairedListView;

    /**
     * Newly discovered devices
     */
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private SharedPreferences settings;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        // Setup the window
        setContentView(R.layout.activity_device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        setUpTextToNumberMap();

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0)
        {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices)
            {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else
        {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
            {
                Log.d(TAG, "Settings key changed: " + key);
                if (key.equals(Settings.IS_SERVER))
                {
                    boolean isServer = settings.getBoolean(Settings.IS_SERVER, false);
                    if (isServer)
                    {
                    //    recognizer.startListening(DeviceListActivity.this);
                    }
                }
            }
        };
        settings.registerOnSharedPreferenceChangeListener(prefListener);

        recognizer = ((ConnectedCarsApplication) getApplication()).getInstance().getSpeechRecognizerService();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        recognizer.startListening(this);
    }

    protected void onPause()
    {
        super.onPause();

        recognizer.stopListening(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    private void setUpTextToNumberMap()
    {
        textToNumberMap = new HashMap<>();
        textToNumberMap.put("one", 1);
        textToNumberMap.put("two", 2);
        textToNumberMap.put("three", 3);
        textToNumberMap.put("four", 4);
        textToNumberMap.put("five", 5);
        textToNumberMap.put("six", 6);
        textToNumberMap.put("seven", 7);
        textToNumberMap.put("eight", 8);
        textToNumberMap.put("nine", 9);
        textToNumberMap.put("ten", 10);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery()
    {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = getIntent();//new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0)
                {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    @Override
    public void onBeginningOfSpeech()
    {

    }

    @Override
    public void onEndOfSpeech()
    {
        if (SpeechRecognizerService.DIGITS_SEARCH.equals(recognizer.getSearchName()))
            recognizer.switchSearch(SpeechRecognizerService.KWS_SEARCH);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(SpeechRecognizerService.KEYPHRASE))
            recognizer.switchSearch(SpeechRecognizerService.DIGITS_SEARCH);
    }

    @Override
    public void onResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

        Log.d(TAG, "select device command");

        String text = hypothesis.getHypstr();
        if (!textToNumberMap.containsKey(text))
            return;

        mBtAdapter.cancelDiscovery();

        Integer number = textToNumberMap.get(text);

        String deviceInfo = (String) pairedListView.getItemAtPosition(number);
        String address = deviceInfo.substring(deviceInfo.length() - 17);

        // Create the result Intent and include the MAC address
        Intent intent = getIntent();//new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();

//            switch (text)
//            {
//                case "one":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//                case "two":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//                case "three":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//                case "four":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//                case "five":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//                case "six":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//                case "seven":
//                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//                    break;
//            }

//            if (text.equals("please take a picture"))
//            {
//                Log.d(TAG, "select device command");
//                //speaker.allow(true);
//                //speaker.speak("Select the device by speaking its sequence number.");
//                //speaker.allow(false);
//            }
    }

    @Override
    public void onError(Exception error) {
//        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        recognizer.switchSearch(SpeechRecognizerService.KWS_SEARCH);
    }
}
