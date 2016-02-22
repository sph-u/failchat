package failchat.test;

import failchat.core.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TestChatClient implements ChatClient, Runnable {

    private static final Logger logger = Logger.getLogger(TestChatClient.class.getName());

    private MessageManager messageManager = MessageManager.getInstance();
    boolean exitFlag = false;


    @Override
    public void goOffline() {
        exitFlag = true;
    }

    @Override
    public void goOnline() {
        Thread t = new Thread(this, "TestChatClient");
        t.start();
    }

    @Override
    public ChatClientStatus getStatus() {
        return null;
    }

    @Override
    public void run() {
        int i = 0;
        while (!exitFlag) {
            Message m = new Message();
            m.setAuthor("Test,author");
            m.setText("test text " + i);
            m.setSource(Source.TEST);
            if (i % 3 == 0) {
                m.setHighlighted(true);
            }
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            try {
//                m.setText(br.readLine());
//            } catch (IOException e) {
//                logger.log(Level.WARNING, "Something goes wrong...", e);
//            }
            messageManager.sendMessage(m);

            Source[] sources = Source.values();
            if (i % 5 == 0) {
                MessageManager.getInstance().sendInfoMessage(new InfoMessage(sources[(i/5)%sources.length], "test info message " + i));
            }
            i++;
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Something goes wrong...", e);
                }
            }
        }
    }
}
