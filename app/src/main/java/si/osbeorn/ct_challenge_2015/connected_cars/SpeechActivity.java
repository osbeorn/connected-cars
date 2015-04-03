package si.osbeorn.ct_challenge_2015.connected_cars;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * Created by Benjamin on 1.4.2015.
 */
public class SpeechActivity extends ActionBarActivity
{
    private static final String TAG = "SpeechRecognitionActi";

    public static final String KWS_SEARCH = "wakeup";
    public static final String COMMANDS_SEARCH = "command";
    public static final String DIGITS_SEARCH = "digit";
    public static final String KEYPHRASE = "oh mighty computer";

    /**
     * The PocketSphinx speech recognizer.
     */
    protected static SpeechRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        new AsyncTask<Void, Void, Exception>()
        {
            @Override
            protected Exception doInBackground(Void... params)
            {
                if (recognizer == null)
                {
                    try
                    {
                        Assets assets = new Assets(SpeechActivity.this);
                        File assetDir = assets.syncAssets();
                        setupRecognizer(assetDir);
                    } catch (IOException e)
                    {
                        Log.e(TAG, "Error when initializing recognition activity.", e);
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Exception result)
            {
                if (result != null)
                {
                    Toast.makeText(SpeechActivity.this, "Failed to init recognizer!", Toast.LENGTH_LONG).show();
                }
//                else
//                {
//                    //boolean isServer = settings.getBoolean(Settings.IS_SERVER, false);
//                    //if (isServer)
//                    //recognizer.startListening(KWS_SEARCH);
//                }
            }
        }.execute();
    }

    protected void switchSearch(final String searchName)
    {
        new AsyncTask<Void, Void, Exception>()
        {
            @Override
            protected Exception doInBackground(Void... params)
            {
                recognizer.stop();
                recognizer.startListening(searchName);

                return null;
            }
        }.execute();
    }

    protected void stopListening(final RecognitionListener listener)
    {
        new AsyncTask<Void, Void, Exception>()
        {
            @Override
            protected Exception doInBackground(Void... params)
            {
                recognizer.stop();
                recognizer.removeListener(listener);

                return null;
            }
        }.execute();
    }

    protected void startListening(final RecognitionListener listener)
    {
        new AsyncTask<Void, Void, Exception>()
        {
            @Override
            protected Exception doInBackground(Void... params)
            {
                recognizer.addListener(listener);
                recognizer.startListening(KWS_SEARCH);

                return null;
            }
        }.execute();
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        File modelsDir = new File(assetsDir, "models");
        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        // Create grammar-based searches.
        //File menuGrammar = new File(modelsDir, "grammar/menu.gram");
        //recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
        File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
        File commandsGrammar = new File(modelsDir, "grammar/commands.gram");
        recognizer.addGrammarSearch(COMMANDS_SEARCH, commandsGrammar);        // Create language model search.

        //File languageModel = new File(modelsDir, "lm/weather.dmp");
        //recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
    }
}
