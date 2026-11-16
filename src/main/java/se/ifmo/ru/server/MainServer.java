package se.ifmo.ru.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainServer {
    private static void handleClientSocket(Socket clientSocket) throws IOException {

        try (clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            String requestLine;
//           Читаем запрос и получаем количество допустимых переходов и ссылку
//           В формате <countOfRef>:<url>
            try {
                String url = reader.readLine();
                Integer countOfRef = Integer.valueOf(reader.readLine());
                clientStorage.get(clientSocket).put(url, countOfRef);
                writer.println(url + " " + countOfRef);
            }
            catch (Exception e){
                writer.println("The format of the data are incorrect");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void main() throws IOException {

        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket clientSocket = serverSocket.accept();

            handleClientSocket(clientSocket);
        }
    }
}
