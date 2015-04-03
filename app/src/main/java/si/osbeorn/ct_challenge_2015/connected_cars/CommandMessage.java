package si.osbeorn.ct_challenge_2015.connected_cars;

import java.io.Serializable;

/**
 * Created by Benjamin on 27.3.2015.
 */
public class CommandMessage implements Serializable
{
    /**
     * MAC address of the sender device.
     */
    private String sender;

    /**
     *
     */
    private int command;

    public CommandMessage(int command)
    {
        setCommand(command);
    }

    public int getCommand()
    {
        return command;
    }

    public void setCommand(int command)
    {
        this.command = command;
    }

    public String getSender()
    {
        return sender;
    }

    public void setSender(String sender)
    {
        this.sender = sender;
    }
}
