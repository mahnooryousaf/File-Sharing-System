package filesharingsystem;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

class Server {

    //Server Directory
    private static final File ServerDirectory = new File("ServerDirectory");

    public static void main(String[] args) {

        ServerSocket server = null;

        try {

            // Listening on port 1234
            server = new ServerSocket(1234);
            server.setReuseAddress(true);

            // Getting mulitple clients
            while (true) {

                // receive incoming client requests
                Socket client = server.accept();

                // Create a new client connection handler thread
                ClientConnectionHandler clientSock = new ClientConnectionHandler(client);
                new Thread(clientSock).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ClientConnectionHandler class
    private static class ClientConnectionHandler implements Runnable {

        private final Socket clientSocket;

        private String computerName;
        private String path;

        // Constructor
        public ClientConnectionHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            try {

                // get the outputstream of client
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                // get the inputstream of client
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String line;

                //Reading commands
                while ((line = in.readLine()) != null) {

                    if (line.contains("Computer Name")) {
                        computerName = line.split(":")[1];
                        System.out.println("Client connected.");
                        System.out.println("Computer Name: "+computerName);
                    } else if (line.contains("Path")) {
                        computerName = line.split(":")[1];
                    } else if (line.equals("DIR")) {
                        sendListContents(out);
                    } else if (line.contains("UPLOAD")) {
                        String fileName = line.split(" ")[1];
                        recieveFileData(in, fileName);
                    } else if (line.contains("DOWNLOAD")) {
                        String fileName = line.split(" ")[1];
                        sendFileData(out, fileName);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendListContents(PrintWriter out) {

            List<String> list = new ArrayList<>();

            //Reading all files from server directory
            for (final File fileEntry : ServerDirectory.listFiles()) {

                if (fileEntry.isFile()) {

                    out.println(fileEntry.getName());

                }
            }

            out.println("EOF");

        }

        private void sendFileData(PrintWriter out, String fileName) {

            //Setting server directory file path to read data
            String filePath = ServerDirectory + "/" + fileName;

            try {

                BufferedReader br = new BufferedReader(new FileReader(filePath));

                String line;
                //Read whole file data and send to client
                while ((line = br.readLine()) != null) {
                    out.println(line);
                }
                out.println("EOF");
                br.close();

            } catch (IOException ex) {
                out.println("EOF");
            }

        }

        private void recieveFileData(BufferedReader in, String fileName) {

            try {

                //Creating new file in server directory
                File f = new File(ServerDirectory + "/" + fileName);
                f.createNewFile();

                String line;

                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                //Add all data in new file
                while (true) {
                    line = in.readLine();
                    if (line.equals("EOF")) {
                        break;
                    }
                    bw.write(line);
                    bw.newLine();

                }
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
