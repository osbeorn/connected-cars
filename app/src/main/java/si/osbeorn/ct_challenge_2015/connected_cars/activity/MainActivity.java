package si.osbeorn.ct_challenge_2015.connected_cars.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.widget.ShareDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import si.osbeorn.ct_challenge_2015.connected_cars.R;
import si.osbeorn.ct_challenge_2015.connected_cars.service.SpeakerService;

public class MainActivity extends ActionBarActivity// implements RecognitionListener
{
    private static final String TAG = "MainActivity";

    private static final int TAKE_IMAGE_ACTIVITY_REQUEST_CODE = 4;
    private static final int SPEAK_COMMAND_ACTIVITY_REQUEST_CODE = 2;

    private ListView wordsList;

    //private SpeechRecognizer recognizer;



    private final int CHECK_CODE = 1;
    private final int LONG_DURATION = 5000;
    private final int SHORT_DURATION = 1200;

    private SpeakerService speakerService;

    private CallbackManager callbackManager;
    private ProfileTracker profileTracker;
    private ShareDialog shareDialog;

    private PendingAction pendingAction = PendingAction.NONE;
    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }

    private FacebookCallback<Sharer.Result> shareCallback = new FacebookCallback<Sharer.Result>() {
        @Override
        public void onCancel() {
            Log.d("HelloFacebook", "Canceled");
        }

        @Override
        public void onError(FacebookException error) {
            Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
            String title = getString(R.string.error);
            String alertMessage = error.getMessage();
            showResult(title, alertMessage);
        }

        @Override
        public void onSuccess(Sharer.Result result) {
            Log.d("HelloFacebook", "Success!");
            if (result.getPostId() != null) {
                String title = getString(R.string.success);
                String id = result.getPostId();
                String alertMessage = getString(R.string.successfully_posted_post, id);
                showResult(title, alertMessage);
            }
        }

        private void showResult(String title, String alertMessage) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(title)
                    .setMessage(alertMessage)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePictureButton = (Button) findViewById(R.id.takePicture);
        takePictureButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //onTakePictureButtonClick();
                openCamera();
            }
        });

        Button speakCommandButton = (Button) findViewById(R.id.speakCommand);
        speakCommandButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onSpeakCommandButtonClick();
            }
        });

        Button bluetoothButton = (Button) findViewById(R.id.useBluetooth);
        bluetoothButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onBluetoothButtonClick();
            }
        });

        Button beepButton = (Button) findViewById(R.id.makeBeep);
        beepButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.nfcsuccess);
                mp.start();

//                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
//                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            }
        });

        Button freeSpeechButton = (Button) findViewById(R.id.testSpeech);
        freeSpeechButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, PostActivity.class);
                startActivity(intent);
            }
        });

        wordsList = (ListView) findViewById(R.id.list);

        callbackManager = CallbackManager.Factory.create();

        //AccessToken.getCurrentAccessToken() // check if logged in

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>()
        {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        //handlePendingAction();
                        //updateUI();
                    }

                    @Override
                    public void onCancel() {
                        if (pendingAction != PendingAction.NONE) {
                            showAlert();
                            pendingAction = PendingAction.NONE;
                        }
                        //updateUI();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        if (pendingAction != PendingAction.NONE
                                && exception instanceof FacebookAuthorizationException) {
                            showAlert();
                            pendingAction = PendingAction.NONE;
                        }
                        //updateUI();
                    }

                    private void showAlert() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.cancelled)
                                .setMessage(R.string.permission_not_granted)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });

        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(
                callbackManager,
                shareCallback);
    }

    private void checkTTS(){
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_make_discoverable:
                makeDiscoverableByBluetooth(300);
                break;

            case R.id.action_add_device:
                speakerService.allow(true);
                speakerService.speak("Hello there!");
                speakerService.allow(false);
                return true;

            case R.id.action_settings:
                //showSettingsActivity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSettingsActivity()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == CHECK_CODE)
        {
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
            {
                speakerService = new SpeakerService(MainActivity.this);
            }
            else
            {
                Intent install = new Intent();
                install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
            }
        }
        if (requestCode == SPEAK_COMMAND_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK)
        {
            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            wordsList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, matches));

            processSpeakCommandMatches(matches);
        }
        else if (requestCode == TAKE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK)
        {
            byte[] pictureByteArray = data.getByteArrayExtra(CameraActivity.PICTURE_BYTE_DATA);
            Log.d(TAG, "Recieved byte array picture data from CameraActivity");
        }

        // Other onActivityResult events

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onTakePictureButtonClick()
    {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(intent, TAKE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }
    }

    public void onSpeakCommandButtonClick()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition Demo...");
        startActivityForResult(intent, SPEAK_COMMAND_ACTIVITY_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + File.separator + "ConnectedCars" + File.separator);

        if (!storageDir.exists())
            storageDir.mkdirs();

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

    private void processSpeakCommandMatches(ArrayList<String> matches)
    {
        //if (matches.contains("take a picture") || matches.contains("take "))
        //{
            openCamera();
        //}
    }

    private void openCamera()
    {
        Intent intent = new Intent(this, CameraActivity.class);

        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        startActivityForResult(intent, TAKE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private void onBluetoothButtonClick()
    {
        Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
        startActivity(intent);
    }





//    @Override
//    public void onPartialResult(Hypothesis hypothesis) {
//        String text = hypothesis.getHypstr();
//        if (text.equals(KEYPHRASE))
//            switchSearch(COMMANDS_SEARCH);
////        if (text.equals(KEYPHRASE))
////            switchSearch(MENU_SEARCH);
////        else if (text.equals(DIGITS_SEARCH))
////            switchSearch(DIGITS_SEARCH);
////        else if (text.equals(FORECAST_SEARCH))
////            switchSearch(FORECAST_SEARCH);
//        else
//        {
//            //((TextView) findViewById(R.id.result_text)).setText(text);
//            List<String> list = new ArrayList<>();
//            list.add(text);
//            wordsList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
//        }
//    }
//
//    @Override
//    public void onResult(Hypothesis hypothesis) {
//        //((TextView) findViewById(R.id.result_text)).setText("");
//        if (hypothesis != null) {
//            String text = hypothesis.getHypstr();
//
//            if (text.equals("please take a picture"))
//            {
//            }
//
//            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @Override
//    public void onBeginningOfSpeech() {
//    }

//    @Override
//    public void onEndOfSpeech() {
////        if (DIGITS_SEARCH.equals(recognizer.getSearchName())
////                || FORECAST_SEARCH.equals(recognizer.getSearchName()))
////            switchSearch(KWS_SEARCH);
//        if (COMMANDS_SEARCH.equals(recognizer.getSearchName()))
//            switchSearch(KWS_SEARCH);
//    }
//
//    private void switchSearch(String searchName) {
//        recognizer.stop();
//        recognizer.startListening(searchName);
//        //String caption = getResources().getString(captions.get(searchName));
//        //((TextView) findViewById(R.id.caption_text)).setText(caption);
//    }
//
//    private void setupRecognizer(File assetsDir) {
//        File modelsDir = new File(assetsDir, "models");
//        recognizer = defaultSetup()
//                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
//                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
//                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
//                .getRecognizer();
//        recognizer.addListener(this);
//
//        // Create keyword-activation search.
//        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
//        // Create grammar-based searches.
//        //File menuGrammar = new File(modelsDir, "grammar/menu.gram");
//        //recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
//        //File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
//        //recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
//        File commandsGrammar = new File(modelsDir, "grammar/commands.gram");
//        recognizer.addGrammarSearch(COMMANDS_SEARCH, commandsGrammar);
//        // Create language model search.
//        //File languageModel = new File(modelsDir, "lm/weather.dmp");
//        //recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
//    }

    private void makeDiscoverableByBluetooth(int duration)
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        startActivity(intent);
    }

    private void addDeviceFromBluetooth()
    {
        // TODO
        // show a list of bluetooth paired devices
        // on selection store MAC address to DB
    }
}