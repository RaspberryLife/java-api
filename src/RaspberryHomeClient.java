import protobuf.ProtoFactory;
import protobuf.RBHproto.*;

import java.io.*;
import java.net.Socket;

/**
 * Created by Peter Mösenthin.
 */
public class RaspberryHomeClient {

    public static final String VERSION_ID = "v0.01";
    public static final int DEFAULT_PORT = 6666;
    public static final String CLIENT_ID = "java_testclient_0";

    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    Socket serverSocket = null;

    public RaspberryHomeClient(){
        System.out.println("RaspberryHomeClient " + VERSION_ID);
        try {
            System.out.println("Connecting to RaspberryHomeServer on port "
                    + DEFAULT_PORT);
            serverSocket = new Socket("localhost",DEFAULT_PORT);
            inputStream = new ObjectInputStream(serverSocket.getInputStream());
            outputStream = new ObjectOutputStream(serverSocket.getOutputStream());
            read();
            authenticate();
        } catch (IOException e) {
            System.out.println("Could not set up client");
            //e.printStackTrace();
        }
    }


    public void write(Object message){
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            System.out.println(
                    "Client could not write message");
            close();
            //e.printStackTrace();
        }
    }

    public void read(){
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                RBHMessage message;
                boolean read = true;
                while (read) {
                    try {
                        try {
                            if ((message = (RBHMessage) inputStream.readObject()) != null) {
                                String messageID = message.getId();
                                System.out.println(
                                        "Client received message from " + messageID + ": ");

                                if(message.getMType() == RBHMessage.MessageType.PLAIN_TEXT){
                                    String text = message.getPlainText().getText();

                                    System.out.println(text);
                                }else if(message.getMType() == RBHMessage.MessageType.AUTH_DENIED){
                                    String denyMessage = message.getPlainText().getText();
                                    System.out.println("Authentication denied. Reason: " + denyMessage);
                                } else if(message.getMType() == RBHMessage.MessageType.AUTH_ACCEPT){
                                    String acceptMessage = message.getPlainText().getText();
                                    System.out.println("Authentication successful: " +  acceptMessage);
                                    sendTestData();
                                } else if(message.getMType() == RBHMessage.MessageType.DATA_SET){
                                    RBHMessage.DataSet dataSet = message.getDataSet();
                                    System.out.print("Message is of type DataSet from " + dataSet.getModulID());
                                    for(RBHMessage.Data d : dataSet.getDataList()){
                                        if(d.hasFloatData()) {
                                            System.out.println(dataSet.getFieldID() + ": " + d.getFloatData());
                                        }
                                    }
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            System.out.println(
                                    "Client could not cast message");
                            e.printStackTrace();
                        }
                    }catch(IOException e){
                        System.out.println(
                                "Client could not read message");
                        read = false;
                        //close();
                        //e.printStackTrace();
                    }
                }
            }
        });
        readThread.start();
    }

    public void close(){
        try {
            inputStream.close();
            outputStream.close();
            serverSocket.close();
            System.out.println(
                    "Client closed connection");
        } catch (IOException e) {
            System.out.println(
                    "Client could not close connection");
            //e.printStackTrace();
        }
    }

    public void authenticate(){
        RBHMessage m = ProtoFactory.buildAuthRequestMessage(
                CLIENT_ID,
                "abc12345"
        );
        write(m);
    }

    public void sendTestData(){
        RBHMessage m = ProtoFactory.buildGetDataSetMessage(
                CLIENT_ID,
                "livingroom_sensormodule",
                "temp",
                100,
                "geht noch nicht",
                "geht noch nicht"
        );
        write(m);
    }


    public static void main(String[] args){
        RaspberryHomeClient client = new RaspberryHomeClient();
    }
}
