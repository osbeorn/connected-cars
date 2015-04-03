package si.osbeorn.ct_challenge_2015.connected_cars;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * Created by Benjamin on 2.4.2015.
 */
public class SpeechRecognizerService
{
    private static final String TAG = "SpeechRecognitionActi";

    public static final String KWS_SEARCH = "wakeup";
    public static final String COMMANDS_SEARCH = "command";
    public static final String DIGITS_SEARCH = "digit";
    public static final String KEYPHRASE = "ok now";

    /**
     * The PocketSphinx speech recognizer.
     */
    private static SpeechRecognizer recognizer;

    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    public SpeechRecognizerService(Context context)
    {
        if (recognizer == null)
        {
            try
            {
                Assets assets = new Assets(context);
                File assetDir = assets.syncAssets();
                setupRecognizer(assetDir);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Error when initializing recognition activity.", e);
            }
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
            {
                Log.d(TAG, "Settings key changed: " + key);
                if (key.equals(Settings.IS_SERVER))
                {
                    boolean isServer = preferences.getBoolean(Settings.IS_SERVER, false);
                    if (!isServer)
                    {
                        recognizer.cancel();
                    }
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    public synchronized void switchSearch(String searchName)
    {
        if (isRecognitionAllowed())
        {
            recognizer.stop();
            recognizer.startListening(searchName);
        }
    }

    public synchronized void stopListening(RecognitionListener listener)
    {
        if (isRecognitionAllowed())
        {
            recognizer.cancel();
            recognizer.removeListener(listener);
        }
    }

    public synchronized void startListening(RecognitionListener listener)
    {
        if (isRecognitionAllowed())
        {
            recognizer.addListener(listener);
            recognizer.startListening(KWS_SEARCH);
        }
    }

    public synchronized String getSearchName()
    {
        return recognizer.getSearchName();
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        File modelsDir = new File(assetsDir, "models");

        Log.d("Assestr dir", assetsDir.getAbsolutePath());

        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir)
                .setKeywordThreshold(/*1e-45f*/1e-20f)
//                .setBoolean("-allphone_ci", true)
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

        //recognizer.stop();
    }

    private boolean isRecognitionAllowed()
    {
        return preferences.getBoolean(Settings.IS_SERVER, false);
    }
}
