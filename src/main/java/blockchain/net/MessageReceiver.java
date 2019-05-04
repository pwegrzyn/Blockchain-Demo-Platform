package blockchain.net;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import java.io.InputStream;
import java.io.OutputStream;

public class MessageReceiver extends ReceiverAdapter {

    @Override
    public void receive(Message msg) {
        super.receive(msg);
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        System.out.println("Get state");

    }

    @Override
    public void setState(InputStream input) throws Exception {
        System.out.println("Set state");

    }

}
