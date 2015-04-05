package si.osbeorn.ct_challenge_2015.connected_cars.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import si.osbeorn.ct_challenge_2015.connected_cars.lib.Settings;

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
    public static final String LANGUAGE_SEARCH = "language";
    public static final String KEYPHRASE = "ok now";

    private static final int RECOGNITION_TIMEOUT = 5000; // 5 seconds

    private String previousSearchName;

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
                if (key.equals(Settings.USE_SPEECH_RECOGNITION))
                {
                    boolean useSpeechRecognition = preferences.getBoolean(Settings.USE_SPEECH_RECOGNITION, false);
                    if (!useSpeechRecognition)
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
            setPreviousSearchName();
            recognizer.stop();

            if (searchName.equals(KWS_SEARCH))
            {
                recognizer.startListening(searchName);
            }
            else
            {
                recognizer.startListening(searchName, RECOGNITION_TIMEOUT);
            }
        }
    }

    public synchronized void stopListening(RecognitionListener listener)
    {
        if (isRecognitionAllowed())
        {
            setPreviousSearchName();
            recognizer.cancel();
            recognizer.removeListener(listener);
        }
    }

    public synchronized void startListening(RecognitionListener listener)
    {
        if (isRecognitionAllowed())
        {
            setPreviousSearchName();
            recognizer.addListener(listener);
            recognizer.startListening(KWS_SEARCH);
        }
    }

    public synchronized String getSearchName()
    {
        return recognizer.getSearchName();
    }

    public synchronized String getPreviousSearchName()
    {
        return previousSearchName;
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        File modelsDir = new File(assetsDir, "models");

        Log.d("Assest dir", assetsDir.getAbsolutePath());

        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir)
                .setKeywordThreshold(/*1e-45f*/1e-20f)
                .setBoolean("-allphone_ci", true)
                .getRecognizer();

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based searches.
        File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
        File commandsGrammar = new File(modelsDir, "grammar/commands.gram");
        recognizer.addGrammarSearch(COMMANDS_SEARCH, commandsGrammar);

        // Create language model search.
        File languageModel = new File(modelsDir, "lm/cmusphinx-5.0-en-us.lm.dmp");
        recognizer.addNgramSearch(LANGUAGE_SEARCH, languageModel);

        //recognizer.stop();
    }

    private boolean isRecognitionAllowed()
    {
        return preferences.getBoolean(Settings.USE_SPEECH_RECOGNITION, false);
    }

    private void setPreviousSearchName()
    {
        previousSearchName = getSearchName();
    }
}
