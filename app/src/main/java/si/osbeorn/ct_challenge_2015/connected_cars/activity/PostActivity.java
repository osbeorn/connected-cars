package si.osbeorn.ct_challenge_2015.connected_cars.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import si.osbeorn.ct_challenge_2015.connected_cars.R;
import si.osbeorn.ct_challenge_2015.connected_cars.application.ConnectedCarsApplication;
import si.osbeorn.ct_challenge_2015.connected_cars.service.SpeechRecognizerService;

public class PostActivity extends ActionBarActivity implements RecognitionListener
{
    private static final String TAG = "PostActivity";

    public static final String IMAGE_FILE_PATH_EXTRA = "IMAGE_FILE_PATH_EXTRA";

    private SpeechRecognizerService recognizer;

    private String imageFilePath;
    private Bitmap imageBitmap;

    private CallbackManager callbackManager;

    private EditText messageEditText;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        Button sendButton = (Button) findViewById(R.id.postButton);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onSendButtonClick();
            }
        });

        messageEditText = (EditText) findViewById(R.id.text_text_view);

        setupImageInView();

        recognizer = ((ConnectedCarsApplication) getApplication()).getInstance().getSpeechRecognizerService();

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>()
        {
            @Override
            public void onSuccess(LoginResult loginResult)
            {
                // continue and share the image
                onSendButtonClick();
            }

            @Override
            public void onCancel()
            {
//                if (pendingAction != PendingAction.NONE)
//                {
                    showAlert();
//                    pendingAction = PendingAction.NONE;
//                }
//                //updateUI();
            }

            @Override
            public void onError(FacebookException exception)
            {
                showAlert();
                Log.e(TAG, "Login error", exception);
            }

            private void showAlert()
            {
                new AlertDialog.Builder(PostActivity.this)
                        .setTitle(R.string.cancelled)
                        .setMessage(R.string.permission_not_granted)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        recognizer.startListening(this);
    }

    protected void onPause()
    {
        super.onPause();

        recognizer.stopListening(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
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
    public void onPartialResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();

        if (recognizer.getSearchName().equals(SpeechRecognizerService.LANGUAGE_SEARCH))
            messageEditText.setText(text);

        if (text.equals(SpeechRecognizerService.KEYPHRASE))
            recognizer.switchSearch(SpeechRecognizerService.COMMANDS_SEARCH);
        else if (text.equals("set message"))
            recognizer.switchSearch(SpeechRecognizerService.LANGUAGE_SEARCH);
    }

    @Override
    public void onResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

        if (SpeechRecognizerService.LANGUAGE_SEARCH.equals(recognizer.getPreviousSearchName()))
            messageEditText.setText(text);

        if (text.equals("send"))
        {
            onSendButtonClick();
        }
    }

    @Override
    public void onError(Exception e)
    {

    }

    @Override
    public void onTimeout()
    {

    }

    private void onSendButtonClick()
    {
        try
        {
            // first check for an active internet connection
            if (!isOnline())
            {
                new AlertDialog.Builder(PostActivity.this)
                        .setTitle("Internet connection")
                        .setMessage("No active internet connection detected.\nCheck and try again later.")
                        .setPositiveButton("OK", null)
                        .show();

                return;
            }

            // second check if user is logged in
            if (AccessToken.getCurrentAccessToken() == null)
            {
                // perform login
                LoginManager
                        .getInstance()
                        .logInWithPublishPermissions(PostActivity.this, Arrays.asList("publish_actions"));
            }

            String message = messageEditText.getText().toString();

            Bundle params = new Bundle();
            params.putString("message", message);
            params.putParcelable("source", imageBitmap);

            GraphRequest request = new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me/photos",
                    params,
                    HttpMethod.POST,
                    new GraphRequest.Callback()
                    {
                        @Override
                        public void onCompleted(GraphResponse graphResponse)
                        {
                            Log.d(TAG, "Posted ...");

                            progressDialog.dismiss();
                            progressDialog = null;

                            finish();
                        }
                    });

            request.executeAsync();

            progressDialog = new ProgressDialog(PostActivity.this);
            progressDialog.setMessage("Posting to Facebook ...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
        }
        catch (Exception exception)
        {
            Log.e(TAG, "Error posting on FB", exception);
        }
    }

    private void setupImageInView()
    {
        Intent intent = getIntent();
        imageFilePath = intent.getStringExtra(PostActivity.IMAGE_FILE_PATH_EXTRA);

        if (imageFilePath == null)
            return;

        imageBitmap = BitmapFactory.decodeFile(imageFilePath);
        ((ImageView) findViewById(R.id.imageToSend)).setImageBitmap(imageBitmap);
    }

    private boolean isOnline() {
        boolean connected = false;

        try {
            ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            connected =
                networkInfo != null &&
                networkInfo.isAvailable() &&
                networkInfo.isConnected();

            return connected;
        }
        catch (Exception e)
        {
            System.out.println("CheckConnectivity Exception: " + e.getMessage());
            Log.v("connectivity", e.toString());
        }

        return connected;
    }
}
