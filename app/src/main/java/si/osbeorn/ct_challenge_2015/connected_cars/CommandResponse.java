package si.osbeorn.ct_challenge_2015.connected_cars;

/**
 * Created by Benjamin on 27.3.2015.
 */
public class CommandResponse extends CommandMessage
{
    private byte[] payload;
    private String arg1;

    public CommandResponse(int command)
    {
        super(command);
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    public String getArg1()
    {
        return arg1;
    }

    public void setArg1(String arg1)
    {
        this.arg1 = arg1;
    }
}
