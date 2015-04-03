package si.osbeorn.ct_challenge_2015.connected_cars;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Benjamin on 31.3.2015.
 */
public class SpeakerService implements TextToSpeech.OnInitListener
{
    private TextToSpeech tts;

    private boolean ready = false;

    private boolean allowed = false;

    public SpeakerService(Context context){
        tts = new TextToSpeech(context, this);
    }

    public boolean isAllowed()
    {
        return allowed;
    }

    public void allow(boolean allowed)
    {
        this.allowed = allowed;
    }

    @Override
    public void onInit(int status)
    {
        if(status == TextToSpeech.SUCCESS)
        {
            tts.setLanguage(Locale.UK);
            ready = true;
        }
        else
        {
            ready = false;
        }
    }

    public void speak(String text)
    {
        // Speak only if the TTS is ready
        // and the user has allowed speech
        if(ready && allowed)
        {
            HashMap<String, String> hash = new HashMap<>();
            hash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
            tts.speak(text, TextToSpeech.QUEUE_ADD, hash);
        }
    }

    public void pause(int duration)
    {
        tts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
    }

    public void destroy()
    {
        tts.shutdown();
    }
}
