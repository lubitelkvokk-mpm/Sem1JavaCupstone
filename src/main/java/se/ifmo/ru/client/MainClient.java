package se.ifmo.ru.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MainClient {
    static void main() throws IOException {
        int port = 8080;
        String host = "127.0.0.1";

        Scanner scanner = new Scanner(System.in);


        try (Socket socket = new Socket(host, port)) {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            String url;
            String countOfRef;
            while (true) {

                System.out.println("Введите url ссылку, которую хотите сократить:");
                url = scanner.nextLine();
                writer.println(url);

                System.out.println("Введите количество возможных переходов для ссылки:");
                countOfRef = scanner.nextLine();
                writer.println(countOfRef);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String responseLine;
                StringBuilder responseBody = new StringBuilder();

                while ((responseLine = reader.readLine()) != null) {
                    responseBody.append(responseLine).append("\n");
                }

                // Выводим ответ сервера
                System.out.println("Ответ сервера:");
                System.out.println(responseBody);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
