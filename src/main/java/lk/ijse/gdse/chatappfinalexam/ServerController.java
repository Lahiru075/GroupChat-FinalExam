package lk.ijse.gdse.chatappfinalexam;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    @FXML
    private Button btnAddClient;

    @FXML
    private TextArea serverArea;

    @FXML
    private TextField textClientName;

    ArrayList<Client> clients = new ArrayList<>();
    private ServerSocket serverSocket;
    private String message = "";
    private Client client;

    @FXML
    void btnAddClientOnAction(ActionEvent event) throws IOException { // add client ui
        serverArea.appendText(textClientName.getText() + " is joined the chat\n");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Client.fxml"));
        Parent parent = loader.load();
        ClientController clientController = loader.getController();
        clientController.lbClientName.setText(textClientName.getText());

        Stage stage = new Stage();
        stage.setTitle(textClientName.getText());
        stage.setScene(new Scene(parent));
        stage.show();
        textClientName.clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        new Thread(() -> {
            System.out.println("server thread");

            try {
                serverSocket = new ServerSocket(5000);
                while (true) {
                    Socket socket = serverSocket.accept();
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    client = new Client();
                    client.setDataInputStream(dataInputStream);
                    client.setDataOutputStream(dataOutputStream);
                    client.setSocket(socket);
                    client.setClientName(textClientName.getText());
                    clients.add(client);

                    handleClient(client);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }).start();
    }

    private void handleClient(Client client) {
        new Thread(() -> {
            try {
                DataInputStream dataInputStream = client.getDataInputStream();
                while (true) {
                    message = dataInputStream.readUTF();

                    if (message.startsWith("IMAGE:")){ // check message is image

                        String imageName = message.substring(6);
                        int length = dataInputStream.readInt();
                        byte[] imageData = new byte[length];
                        dataInputStream.readFully(imageData);
                        brotcastImage(imageName, imageData, client);

                    } else if (message.startsWith("FILE:")){ // check message is file

                        String fileName = message.substring(5);
                        int length = dataInputStream.readInt();
                        byte[] fileData = new byte[length];
                        dataInputStream.readFully(fileData);
                        brotcastFile(fileName, fileData, client);
                    }else { // message is text
                        System.out.println("Received Massage : " + message);
                        brotcast(message , client);
                    }

                    if (message.equals("Finished")) {
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                synchronized (clients) {
                    clients.remove(client);
                }
                try {
                    client.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void brotcastImage(String imageName, byte[] imageData, Client sender) { // if image
        synchronized (clients) {
            for (Client c : clients) {
                if (c != sender) {
                    try {
                        c.getDataOutputStream().writeUTF("IMAGE:" + sender.getClientName() + ":" + imageName);
                        c.getDataOutputStream().writeInt(imageData.length);
                        c.getDataOutputStream().write(imageData);
                        c.getDataOutputStream().flush();
                        System.out.println("Image sent to: " + c.getClientName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void brotcastFile(String fileName, byte[] fileData, Client sender) { // if file
        synchronized (clients) {
            for (Client c : clients) {
                if (c != sender) {
                    try {
                        c.getDataOutputStream().writeUTF("FILE:"+ sender.getClientName() + ":"+ fileName);
                        c.getDataOutputStream().writeInt(fileData.length);
                        c.getDataOutputStream().write(fileData);
                        c.getDataOutputStream().flush();
                        System.out.println("File sent to: " + c.getClientName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void brotcast(String massage, Client sender) { // if text
        synchronized (clients) {
            for (Client c : clients) {
                if (c != sender) {
                    try {
                        c.getDataOutputStream().writeUTF(sender.getClientName() + ": " +massage);
                        System.out.println("sender client name : " + sender.getClientName());
                        c.getDataOutputStream().flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }

    }
}
