package si.osbeorn.ct_challenge_2015.connected_cars.application;

import android.app.Application;
import android.content.Context;

import com.facebook.FacebookSdk;

import si.osbeorn.ct_challenge_2015.connected_cars.SpeakerService;
import si.osbeorn.ct_challenge_2015.connected_cars.SpeechRecognizerService;

/**
 * Created by Benjamin on 2.4.2015.
 */
public class ConnectedCarsApplication extends Application
{
    /**
     * The ConnectedCarsApplication instance and its constructor.
     */
    private static ConnectedCarsApplication instance;

    /**
     * ConnectedCars application context.
     */
    private static Context context;

    /**
     * Default constructor.
     */
    public ConnectedCarsApplication() { }

    @Override
    public void onCreate()
    {
        super.onCreate();

        instance = this;
        instance.initialize();

        FacebookSdk.sdkInitialize(context);
    }

    // region Private members

    private SpeechRecognizerService speechRecognizerService;
    private SpeakerService speakerService;

    // endregion

    public static ConnectedCarsApplication getInstance()
    {
        return instance;
    }

    private void initialize()
    {
        initializeContext();
        initializeMembers();
    }

    private void initializeContext()
    {
        context = getApplicationContext();
    }

    private void initializeMembers()
    {
        speechRecognizerService = new SpeechRecognizerService(context);
        speakerService = new SpeakerService(context);
    }

    // region Getters

    public synchronized SpeechRecognizerService getSpeechRecognizerService()
    {
        return speechRecognizerService;
    }

    public SpeakerService getSpeakerService()
    {
        return speakerService;
    }

    // endregion
}