package lk.ijse.gdse.chatappfinalexam;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML
    private Button btnSend;

    @FXML
    private Button btnUploadFiles;

    @FXML
    private Button btnUploadImage;

    @FXML
    private VBox chat;

    @FXML
    Label lbClientName;

    @FXML
    private TextField txtTypeMessage;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String messege = "";
    private String clientName = "";

    @FXML
    void btnSendOnAction(ActionEvent event) {
        String sendingMessage = txtTypeMessage.getText();

        try {
            dataOutputStream.writeUTF(sendingMessage);
            dataOutputStream.flush();
            txtTypeMessage.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void btnUploadFilesOnAction(ActionEvent event) { // upload file
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                dataOutputStream.writeUTF("FILE:" + file.getName());
                dataOutputStream.flush();
                dataOutputStream.writeInt(fileData.length);
                dataOutputStream.write(fileData);
                dataOutputStream.flush();
                System.out.println("File sent: " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void btnUploadImageOnAction(ActionEvent event) { // upload image
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] imageData = Files.readAllBytes(file.toPath());
                dataOutputStream.writeUTF("IMAGE:" + file.getName());
                dataOutputStream.writeInt(imageData.length);
                dataOutputStream.write(imageData);
                dataOutputStream.flush();
                System.out.println("Image is sent: " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                clientName = lbClientName.getText();

                while (!messege.equals("BYE")) {

                    messege = dataInputStream.readUTF();

                    if (messege.startsWith("IMAGE:")){ // check message is image

                        String[] parts = messege.split(":", 3);
                        String senderName = parts[1];
                        String imageName = parts[2];
                        int length = dataInputStream.readInt();
                        byte[] imageData = new byte[length];
                        dataInputStream.readFully(imageData);

                        Platform.runLater(() -> {
                            Label messageLabel = new Label(senderName + " sent image: " + imageName + "\n");
                            messageLabel.setFont(new Font(14));
                            chat.getChildren().add(messageLabel);

                            try {
                                Files.createDirectories(Paths.get("downloads"));
                                Files.write(Paths.get("downloads/" + imageName), imageData);


                                ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
                                Image image = new Image(bis);
                                ImageView imageView = new ImageView(image);
                                imageView.setFitWidth(100);
                                imageView.setFitHeight(100);
                                chat.getChildren().add(imageView);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    } else if (messege.startsWith("FILE:")) { // check message is file

                        String[] parts = messege.split(":", 3);
                        String senderName = parts[1];
                        String fileName = parts[2];
                        int length = dataInputStream.readInt();
                        byte[] fileData = new byte[length];
                        dataInputStream.readFully(fileData);

                        Platform.runLater(() -> {

                            Label messageLabel = new Label("Received file: " + fileName + "\n");
                            messageLabel.setFont(new Font(14));
                            chat.getChildren().add(messageLabel);

                            try {
                                Files.createDirectories(Paths.get("downloads"));

                                String safeFileName = fileName.replaceAll("[<>:\"/\\\\|?*]", "_");
                                Files.write(Paths.get("downloads/" + safeFileName), fileData);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    }else{ // message is text
                        Platform.runLater(() -> {
                            Label messageLabel = new Label(messege);
                            messageLabel.setFont(new Font(14));
                            chat.getChildren().add(messageLabel);
                        });
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
