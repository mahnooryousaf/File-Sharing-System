/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filesharingsystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;


public class Client extends Application {

    File clientDirectory;

    ObservableList<String> clientFileslist = FXCollections.observableArrayList();
    ObservableList<String> serverFileslist = FXCollections.observableArrayList();

    ListView<String> clientListView;
    ListView<String> serverListView;
    
    TextArea textarea;

    @Override
    public void start(Stage primaryStage) {

        Button dbtn = new Button("Download");
        Button ubtn = new Button("Upload");

        ubtn.setPrefSize(200, 30);
        dbtn.setPrefSize(200, 30);

        dbtn.setOnAction(e -> {
            downloadServerFile();
        });
        ubtn.setOnAction(e -> {
            uploadFileToServer();
        });

        clientListView = new ListView();
        serverListView = new ListView();

        clientListView.setOnMouseClicked(e -> {
            displayContent();
        });
        serverListView.setOnMouseClicked(e -> {
            textarea.setText("Download file to display content");
        });

        Label lblClient = new Label("Client's Directory");
        Label lblServer = new Label("Server's Directory");
        lblClient.setTextFill(Color.BLUE);
        lblClient.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
        lblServer.setTextFill(Color.BLUE);
        lblServer.setFont(Font.font("Verdana", FontWeight.BOLD, 15));

        VBox clientVBox = new VBox();
        clientVBox.getChildren().addAll(lblClient, clientListView, ubtn);
        clientVBox.setAlignment(Pos.CENTER);
        clientVBox.setPadding(new Insets(20));
        clientVBox.setSpacing(15);
        VBox serverVBox = new VBox();
        serverVBox.getChildren().addAll(lblServer, serverListView, dbtn);
        serverVBox.setAlignment(Pos.CENTER);
        serverVBox.setPadding(new Insets(20));
        serverVBox.setSpacing(15);

        HBox hbox2 = new HBox();
        hbox2.getChildren().addAll(clientVBox, serverVBox);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(clientVBox, serverVBox);

        textarea = new TextArea();
        textarea.setEditable(false);
        textarea.setPrefSize(400, 400);
        textarea.setPadding(new Insets(20));
        
        VBox root = new VBox();
        root.getChildren().addAll(hbox, hbox2, textarea);

        Scene scene = new Scene(root, 500, 450);

        //Prompt the client to select his folder
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("."));
        clientDirectory = directoryChooser.showDialog(primaryStage);

        if (clientDirectory != null) {
            readDirectoryFiles();
        }

        //Connect with server at start and get shared folder data
        serverConnection();

        //Set File contenets in list view
        clientListView.getItems().addAll(clientFileslist);
        serverListView.getItems().addAll(serverFileslist);

        primaryStage.setTitle("File Sharing System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private void readDirectoryFiles() {

        //Reading all files
        for (final File fileEntry : clientDirectory.listFiles()) {

            if (fileEntry.isFile()) {

                clientFileslist.add(fileEntry.getName());

            }
        }

    }

    private void serverConnection() {

        try (Socket socket = new Socket("localhost", 1234)) {

            // writing to server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // reading from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //send computer name and client's folder
            // sending the user input to server
            out.println("Computer Name:"+InetAddress.getLocalHost().getHostName());
            out.println(clientDirectory);

            //Command to get contents of shared folder
            out.println("DIR");
            out.flush();

            // Server reply
            serverFileslist.clear();

            String line;

            while (true) {
                line = in.readLine();

                if (line.equals("EOF")) {
                    break;
                }
                serverFileslist.add(line);
            }

        } catch (IOException e) {
            //e.printStackTrace();
            showAlert("Connection Error", "Server is not connected!", Alert.AlertType.ERROR);

        }

    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void downloadServerFile() {

        //get selected file to download from server
        String selectedFile = serverListView.getSelectionModel().getSelectedItem();

        if (selectedFile == null) {
            showAlert("Download Error", "Server file is not selected!", Alert.AlertType.ERROR);
        } else {

            downloadFile(selectedFile);

        }

    }

    private void downloadFile(String selectedFile) {

        //Connect to server
        try (Socket socket = new Socket("localhost", 1234)) {

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Command to get contents of shared folder
            out.println("DOWNLOAD " + selectedFile);
            out.flush();

            //Creating new file in client's directory
            File f = new File(clientDirectory + "/" + selectedFile);
            f.createNewFile();

            String line;

            try {
                //Writing all data in file downloaded from server
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));

                while (true) {
                    line = in.readLine();
                    if (line.equals("EOF")) {
                        break;
                    }
                    bw.write(line);
                    bw.newLine();

                }
                bw.close();
                //Add file to client list
                if(!clientFileslist.contains(selectedFile))
                    clientFileslist.add(selectedFile);

            } catch (IOException e) {
                e.printStackTrace();
            }

            refreshLists();

        } catch (IOException e) {
            //e.printStackTrace();
            showAlert("Connection Error", "Server is not connected!", Alert.AlertType.ERROR);

        }

    }

    private void refreshLists() {

        clientListView.getItems().setAll(clientFileslist);
        serverListView.getItems().setAll(serverFileslist);

    }

    private void uploadFileToServer() {
        //Get selected file to upload
        String selectedFile = clientListView.getSelectionModel().getSelectedItem();

        if (selectedFile == null) {
            showAlert("Upload Error", "Client file is not selected!", Alert.AlertType.ERROR);
        } else {

            uploadFile(selectedFile);
        }

    }

    //Upload file to server
    private void uploadFile(String selectedFile) {

        //Connect to server
        try (Socket socket = new Socket("localhost", 1234)) {

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            //Command to upload file
            out.println("UPLOAD " + selectedFile);

            //Path to read file from client
            String filePath = clientDirectory + "/" + selectedFile;

            try {

                BufferedReader br = new BufferedReader(new FileReader(filePath));

                String line;
                //Read data from file and send to server
                while ((line = br.readLine()) != null) {

                    out.println(line);
                }
                //Send EOF 
                out.println("EOF");
                br.close();

            } catch (IOException ex) {
                out.println("EOF");
            }

            out.flush();
            //Add file name in server file list
            if(!serverFileslist.contains(selectedFile))
                serverFileslist.add(selectedFile);

            refreshLists();

        } catch (IOException e) {
            //e.printStackTrace();
            showAlert("Connection Error", "Server is not connected!", Alert.AlertType.ERROR);

        }

    }

    private void displayContent() {

        textarea.setText("");
        
        String selectedFile = clientListView.getSelectionModel().getSelectedItem();

        if (selectedFile != null) {

            //Path to read file from client
            String filePath = clientDirectory + "/" + selectedFile;

            try {

                BufferedReader br = new BufferedReader(new FileReader(filePath));

                String line;
                //Read data from file and send to server
                while ((line = br.readLine()) != null) {

                    textarea.appendText(line+"\n");
                }
                br.close();

            } catch (IOException ex) {
            }


        }

    }

}
