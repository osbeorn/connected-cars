package si.osbeorn.ct_challenge_2015.connected_cars;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import si.osbeorn.ct_challenge_2015.connected_cars.application.ConnectedCarsApplication;


public class BluetoothActivity extends ActionBarActivity implements RecognitionListener
{
    private static final String TAG = "BluetoothActivity";

    private static final int REQUEST_CONNECT_DEVICE_SECURE_CODE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE_CODE = 2;
    private static final int REQUEST_ENABLE_BT_CODE = 3;

    private static final int REQUEST_TAKE_IMAGE_REQUEST_CODE = 4;

    private static final int CHECK_CODE = 5;

    private static final String RESPONSE_OBJECT_EXTRA = "RESPONSE_OBJECT_EXTRA";

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    //private SpeechRecognizer recognizer;

    Button sendCommandButton = null; // temp
    Button sendPictureCommandButton = null; // temp

    private SharedPreferences settings;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private SpeechRecognizerService recognizer;
    //private SpeakerService speaker;

    // region Activity implementation

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Button connectButton = (Button) findViewById(R.id.connectDevice);
        connectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                selectDeviceForConnection();
            }
        });

        sendCommandButton = (Button) findViewById(R.id.sendCommand);
        sendPictureCommandButton = (Button) findViewById(R.id.sendPictureCommand);

//        checkTTS();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
            {
                Log.d(TAG, "Settings key changed: " + key);
                if (key.equals(Settings.IS_SERVER))
                {
                    // when this event happens the values are not yet updated
                    // so if the isServer value is "false", that means the new value
                    // is "true"

                    boolean isServer = settings.getBoolean(Settings.IS_SERVER, false);
                    if (isServer)
                    {
                    //    recognizer.startListening(BluetoothActivity.this);
                    }
                }
            }
        };
        settings.registerOnSharedPreferenceChangeListener(prefListener);

        recognizer = ((ConnectedCarsApplication) getApplication()).getInstance().getSpeechRecognizerService();
        //speaker = ((ConnectedCarsApplication) getApplication()).getInstance().getSpeakerService();
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT_CODE);
            // Otherwise, setup the chat session
        }
        else if (mChatService == null)
        {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null)
        {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null)
        {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE)
            {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }

        recognizer.startListening(this);
    }

    public void onPause()
    {
        super.onPause();

        recognizer.stopListening(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            showSettingsActivity();
            return true;
        }
        else if (id == R.id.make_discoverable)
        {
            makeDiscoverableByBluetooth(300);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_CONNECT_DEVICE_SECURE_CODE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK)
                {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE_CODE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK)
                {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT_CODE:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK)
                {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                }
                else
                {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, "Bluetooth not enabled",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case REQUEST_TAKE_IMAGE_REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    Log.d(TAG, "Received byte array picture data from CameraActivity");
                    sendImageToRequester(data);
                }
                break;

//            case CHECK_CODE:
//                if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
//                {
//                    speakerService = new SpeakerService(BluetoothActivity.this);
//                }
//                else
//                {
//                    Intent install = new Intent();
//                    install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
//                    startActivity(install);
//                }
        }
    }

    // endregion

    // region RecognitionListener implementation

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(SpeechRecognizerService.KEYPHRASE))
            recognizer.switchSearch(SpeechRecognizerService.COMMANDS_SEARCH);
//        if (text.equals(KEYPHRASE))
//            switchSearch(MENU_SEARCH);
//        else if (text.equals(DIGITS_SEARCH))
//            switchSearch(DIGITS_SEARCH);
//        else if (text.equals(FORECAST_SEARCH))
//            switchSearch(FORECAST_SEARCH);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

        if (text.equals("take a picture"))
        {
            //speaker.allow(true);
            //speaker.speak("Select the device by speaking its sequence number.");
            //speaker.allow(false);

            sendCommand(new CommandRequest(Commands.TAKE_PICTURE));
        }
        else if (text.equals("connect device"))
        {
            selectDeviceForConnection();
        }
    }

    @Override
    public void onBeginningOfSpeech()
    {
    }

    @Override
    public void onEndOfSpeech()
    {
        if (!recognizer.getSearchName().equals(SpeechRecognizerService.KWS_SEARCH))
            recognizer.switchSearch(SpeechRecognizerService.KWS_SEARCH);
//        if (SpeechRecognizerService.COMMANDS_SEARCH.equals(recognizer.getSearchName()))
//            recognizer.switchSearch(SpeechRecognizerService.KWS_SEARCH);
    }

    @Override
    public void onError(Exception error) {
        //((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        recognizer.switchSearch(SpeechRecognizerService.KWS_SEARCH);
    }

    // endregion

    private void checkTTS()
    {
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    private void sendImageToRequester(Intent data)
    {
        //byte[] pictureByteArray = data.getByteArrayExtra(CameraActivity.PICTURE_BYTE_DATA);
        Uri uri = (Uri) data.getParcelableExtra(MediaStore.EXTRA_OUTPUT);

        byte[] pictureByteArray = getImageBytes(uri);
        CommandResponse response = (CommandResponse) data.getSerializableExtra(RESPONSE_OBJECT_EXTRA);

//                    ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//                    Deflater comp = new Deflater();
//                    comp.setLevel(Deflater.DEFAULT_COMPRESSION);
//
//                    DeflaterOutputStream dos = new DeflaterOutputStream(out, comp);
//
//                    try
//                    {
//                        dos.write(pictureByteArray);
//                        dos.finish();
//                    } catch (IOException e)
//                    {
//                        e.printStackTrace();
//                    }

        response.setPayload(pictureByteArray/*out.toByteArray()*/);
        sendCommand(response);
    }

    private byte[] getImageBytes(Uri uri)
    {
        try
        {
            InputStream iStream =  getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;

            while ((len = iStream.read(buffer)) != -1)
            {
                byteBuffer.write(buffer, 0, len);
            }

            return byteBuffer.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    // region UI <-> BluetoothChatService handler

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            //FragmentActivity activity = getActivity();
            switch (msg.what)
            {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1)
                    {
                        case BluetoothChatService.STATE_CONNECTED:
                            Toast.makeText(BluetoothActivity.this, "Bluetooth connected!", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Toast.makeText(BluetoothActivity.this, "Bluetooth connecting!", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Toast.makeText(BluetoothActivity.this, "Bluetooth listening/idle!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case Constants.MESSAGE_TAKE_PICTURE:
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    processMessage(readBuf);

                    Toast.makeText(BluetoothActivity.this, "Message received", Toast.LENGTH_SHORT).show();
                    Log.d("BluetoothActivity", "Message received");
                    break;

//                case Constants.MESSAGE_DEVICE_NAME:
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
//                    break;
//                case Constants.MESSAGE_TOAST:
//                    if (null != activity) {
//                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                    break;
            }
        }
    };

    // endregion

    private void processMessage(byte[] messageByteArray)
    {
        processCommand(Utils.<CommandMessage>byteArrayToObject(messageByteArray));
    }

    private void processCommand(CommandMessage message)
    {
        if (message instanceof CommandRequest)
        {
            processCommandRequest((CommandRequest)message);
        }
        else if (message instanceof CommandResponse)
        {
            processCommandResponse((CommandResponse)message);
        }
    }

    private void processCommandRequest(CommandRequest request)
    {
        int command = request.getCommand();

        CommandResponse response;
        if (command == Commands.TAKE_PICTURE)
        {
            response = new CommandResponse(command);

            Intent intent = new Intent(BluetoothActivity.this, CameraActivity.class);
            intent.putExtra(RESPONSE_OBJECT_EXTRA, response);

            // Create the File where the photo should go
            File photoFile = Utils.createTempFile(null, ".jpg");

            if (photoFile != null)
            {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(intent, REQUEST_TAKE_IMAGE_REQUEST_CODE);
            }
        }
    }

    private void processCommandResponse(CommandResponse response)
    {
        int command = response.getCommand();

        if (command == Commands.TAKE_PICTURE)
        {
            // process image data
            byte[] imageByteArray = response.getPayload();

            saveImageToDisk(imageByteArray);
        }
    }

    private boolean saveImageToDisk(byte[] data)
    {
        FileOutputStream outStream = null;
        File outFile = null;

        try
        {
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//            InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(data));
//
//            byte[] buf = new byte[8192];
//            while(iis.read(buf) > 0)
//            {
//                out.write(buf);
//            }
//
//            byte[] decompressed = out.toByteArray();

            //String fileName = String.format("/sdcard/%d.jpg", System.currentTimeMillis());

            //outFile = new File(fileName);
            outFile = Utils.createTempFile(null, ".jpg");
            outStream = new FileOutputStream(outFile);
            outStream.write(data/*decompressed*/);
            outStream.close();
            Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);

            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(outFile), "image/jpg");

            startActivity(intent);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally {}

        return true;
    }

    private void setupChat()
    {
        // Initialize the send button with a listener that for click events
        sendCommandButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                /* TODO
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
                */
                sendCommand("BLA");
            }
        });

        sendPictureCommandButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                sendCommand(new CommandRequest(Commands.TAKE_PICTURE));
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(BluetoothActivity.this, mHandler);
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    private void makeDiscoverableByBluetooth(int duration)
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        startActivity(intent);
    }

    private void sendCommand(String command)
    {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "Ni povezave", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (command.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = command.getBytes();
            mChatService.write(send);
        }
    }

    private void sendCommand(CommandResponse responseObject)
    {
        if (responseObject == null)
            return;

        byte[] responseByteArray = Utils.objectToByteArray(responseObject);
        mChatService.write(responseByteArray);
    }

    private void sendCommand(CommandRequest requestObject)
    {
        if (requestObject == null)
            return;

        byte[] requestByteArray = Utils.objectToByteArray(requestObject);
        mChatService.write(requestByteArray);
    }

    private void selectDeviceForConnection()
    {
        Log.d(TAG, "Creating intent for DeviceActivity");
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE_CODE);
    }

    private void showSettingsActivity()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
