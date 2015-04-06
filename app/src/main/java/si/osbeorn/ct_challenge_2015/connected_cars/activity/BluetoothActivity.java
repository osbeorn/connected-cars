package si.osbeorn.ct_challenge_2015.connected_cars.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import si.osbeorn.ct_challenge_2015.connected_cars.R;
import si.osbeorn.ct_challenge_2015.connected_cars.application.ConnectedCarsApplication;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.CommandMessage;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.CommandRequest;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.CommandResponse;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.Commands;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.Constants;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.Utils;
import si.osbeorn.ct_challenge_2015.connected_cars.service.BluetoothConnectionService;
import si.osbeorn.ct_challenge_2015.connected_cars.service.SpeakerService;
import si.osbeorn.ct_challenge_2015.connected_cars.service.SpeechRecognizerService;


public class BluetoothActivity extends ActionBarActivity implements RecognitionListener
{
    private static final String TAG = "BluetoothActivity";

    private static final String PENDING_FILE_PATH_BUNDLE_KEY = "PENDING_FILE_PATH_BUNDLE_KEY";

    private static final int REQUEST_CONNECT_DEVICE_SECURE_CODE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE_CODE = 2;
    private static final int REQUEST_ENABLE_BT_CODE = 3;

    private static final int REQUEST_TAKE_IMAGE_REQUEST_CODE = 4;

    private static final int CHECK_CODE = 5;

    private static final String RESPONSE_OBJECT_EXTRA = "RESPONSE_OBJECT_EXTRA";
    public static final String CAMERA_TYPE_EXTRA = "CAMERA_TYPE_EXTRA";

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothConnectionService mConnectionService = null;

    Button sendCommandButton = null; // temp
    Button sendPictureCommandButton = null; // temp
    ProgressDialog progressDialog = null;

    private SharedPreferences settings;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private SpeechRecognizerService recognizer;
    private SpeakerService speaker;

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

        recognizer = ((ConnectedCarsApplication) getApplication()).getInstance().getSpeechRecognizerService();
        speaker = ((ConnectedCarsApplication) getApplication()).getInstance().getSpeakerService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupConnectionService() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT_CODE);
            // Otherwise, setup the chat session
        }
        else if (mConnectionService == null)
        {
            setupConnectionService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mConnectionService != null)
        {
            mConnectionService.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mConnectionService != null)
        {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mConnectionService.getState() == BluetoothConnectionService.STATE_NONE)
            {
                // Start the Bluetooth chat services
                mConnectionService.start();
            }
        }

        recognizer.startListening(this);
        speaker.allow(true);
    }

    protected void onPause()
    {
        super.onPause();

        recognizer.stopListening(this);
        speaker.allow(false);
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
        else if (id == R.id.file_list)
        {
            showFileListActivity();
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
                    setupConnectionService();
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
    public void onPartialResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(SpeechRecognizerService.KEYPHRASE))
            recognizer.switchSearch(SpeechRecognizerService.COMMANDS_SEARCH);
    }

    @Override
    public void onResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

            String text = hypothesis.getHypstr();

            if (text.equals(SpeechRecognizerService.KEYPHRASE))
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
            else if (text.equals("take a picture"))
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                sendCommand(Commands.TAKE_PICTURE);
            }
            else if (text.equals("take a picture front"))
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                sendCommand(Commands.TAKE_PICTURE_FRONT);
            }
            else if (text.equals("connect device"))
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public void onError(Exception error)
    {
    }

    @Override
    public void onTimeout()
    {
        recognizer.switchSearch(SpeechRecognizerService.KWS_SEARCH);
    }

    // endregion

    private void sendImageToRequester(Intent data)
    {
        //byte[] pictureByteArray = data.getByteArrayExtra(CameraActivity.PICTURE_BYTE_DATA);
        Uri uri = (Uri) data.getParcelableExtra(MediaStore.EXTRA_OUTPUT);

        byte[] pictureByteArray = Utils.fileToByteArray(getContentResolver(), uri);
        CommandResponse response = (CommandResponse) data.getSerializableExtra(RESPONSE_OBJECT_EXTRA);

        response.setPayload(pictureByteArray/*out.toByteArray()*/);
        sendCommand(response);
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
                        case BluetoothConnectionService.STATE_CONNECTED:
                            Toast.makeText(BluetoothActivity.this, "Bluetooth connected!", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothConnectionService.STATE_CONNECTING:
                            Toast.makeText(BluetoothActivity.this, "Bluetooth connecting!", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothConnectionService.STATE_LISTEN:
                        case BluetoothConnectionService.STATE_NONE:
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
                    if (progressDialog != null)
                    {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    byte[] readBuf = (byte[]) msg.obj;
                    processMessage(readBuf);

                    Toast.makeText(BluetoothActivity.this, "Message received", Toast.LENGTH_SHORT).show();
                    Log.d("BluetoothActivity", "Message received");
                    break;

                case Constants.MESSAGE_IN_PROGRESS:
                    if (progressDialog == null)
                    {
                        progressDialog = new ProgressDialog(BluetoothActivity.this);
                        progressDialog.setMessage("Receiving data ...");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.show();
                    }
                    break;
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
            processCommandRequest((CommandRequest) message);
        }
        else if (message instanceof CommandResponse)
        {
            processCommandResponse((CommandResponse) message);
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
        else if (command == Commands.TAKE_PICTURE_FRONT)
        {
            response = new CommandResponse(command);

            Intent intent = new Intent(BluetoothActivity.this, CameraActivity.class);
            intent.putExtra(RESPONSE_OBJECT_EXTRA, response);
            intent.putExtra(CAMERA_TYPE_EXTRA, Camera.CameraInfo.CAMERA_FACING_FRONT);

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

        String filePath = null;
        if (command == Commands.TAKE_PICTURE)
        {
            // process image data
            byte[] imageByteArray = response.getPayload();
            filePath = saveImageToDisk(imageByteArray);

            speaker.speak("Picture received!");
            speaker.pause(100);
            speaker.speak("Do you want to post it on Facebook?");
        }

        shareResponseChooser(command, filePath);
    }

    private void shareResponseChooser(final int command, final String filePath)
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        shareResponse(filePath, false);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // do nothing
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothActivity.this);
        builder
            .setMessage("Want to share image on Facebook?")
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener)
            .show();
    }

    private void shareResponse(String filePath, boolean fromCallback)
    {
        Intent intent = new Intent(BluetoothActivity.this, PostActivity.class);
        intent.putExtra(PostActivity.IMAGE_FILE_PATH_EXTRA, filePath);
        startActivity(intent);
    }

    private String saveImageToDisk(byte[] data)
    {
        FileOutputStream outStream = null;
        File outFile = null;

        try
        {
            outFile = Utils.createTempFile(null, ".jpg");
            outStream = new FileOutputStream(outFile);
            outStream.write(data/*decompressed*/);
            outStream.close();
            Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
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

        return outFile.getAbsolutePath();
    }

    private void setupConnectionService()
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
        mConnectionService = new BluetoothConnectionService(BluetoothActivity.this, mHandler);
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mConnectionService.connect(device, secure);
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
        if (mConnectionService.getState() != BluetoothConnectionService.STATE_CONNECTED) {
            Toast.makeText(this, "Ni povezave", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (command.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = command.getBytes();
            mConnectionService.write(send);
        }
    }

    private void sendCommand(CommandResponse responseObject)
    {
        if (responseObject == null)
            return;

        byte[] responseByteArray = Utils.objectToByteArray(responseObject);
        mConnectionService.write(responseByteArray);
    }

    private void sendCommand(int command)
    {
        sendCommand(new CommandRequest(command));
    }

    private void sendCommand(CommandRequest requestObject)
    {
        if (requestObject == null)
            return;

        byte[] requestByteArray = Utils.objectToByteArray(requestObject);
        mConnectionService.write(requestByteArray);
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

    private void showFileListActivity()
    {
        Intent intent = new Intent(this, FileListActivity.class);
        startActivity(intent);
    }
}
