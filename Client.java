import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        Socket socket = null;
        Scanner scanner = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            // Connect to server
            socket = new Socket(host, port);
            System.out.println("Connected to chat server.");
            scanner = new Scanner(System.in);

            // Setup streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // === LOGIN FLOW ===
            String usernamePrompt = in.readLine();
            System.out.print(usernamePrompt + " "); // "Enter your username:"

            String username;
            while (true) {
                username = scanner.nextLine();
                if (username != null && !username.trim().isEmpty()) {
                    out.println(username);
                    break;
                } else {
                    out.println(username); // Send empty input to trigger server response
                    String response = in.readLine(); // Read invalid message from server
                    if (response != null) {
                        System.out.println(response); // Print "Invalid username..."
                    }
                }
            }

            while (true) {
                String passwordPrompt = in.readLine();
                if (passwordPrompt == null) {
                    System.out.println("Server closed connection unexpectedly.");
                    return;
                }

                // Print prompt and wait for input
                System.out.print(passwordPrompt + " ");
                
                String password = scanner.nextLine();
                out.println(password);

                // If server accepts the password, it will send a line like "Login successful..."
                // So we check the next response
                String response = in.readLine();
                if (response == null) {
                    System.out.println("Connection closed by server.");
                    return;
                }

                // Print the response
                if (response.contains("successful")) {
                    System.out.println(response);
                    break; // Exit loop on success
                } else if (response.contains("Wrong password") || response.contains("Password cannot be empty")) {
                    System.out.println(response); // Show error message
                    continue; // Try again
                } else {
                    System.out.println(response);
                    // Optionally handle other responses
                    continue;
                }
            }

           // No need to read another line — we already got the "successful" message during password loop
            System.out.println("Proceeding to main menu...\n");

            // Start background thread to receive messages
            // Make sure `in` is not reassigned after this point
            BufferedReader finalIn = in; // 🔥 Make a final copy to use in lambda
            Thread readerThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = finalIn.readLine()) != null) {
                        // If response is a menu option, print on new line
                        if (response.contains("1. Join a Room") ||
                            response.contains("2. Create a Room") ||
                            response.contains("3. Friend Menu") ||
                            response.contains("1. View friends") ||
                            response.contains("Friend Menu:") ||
                            response.contains("2. Add friend") ||
                            response.contains("3. Message a friend") ||
                            response.contains("4. Back to main menu")) {
                            System.out.println(response);
                        } else if (response.contains("Do you want to")){
                            System.out.println("\n\n" + response);

                        }
                        // For normal prompts ending with : . or ?
                        else if (response.endsWith(":") || response.endsWith(".") || response.endsWith("?")) {
                            System.out.print(response + " ");
                        } 
                        // For everything else
                        else {
                            System.out.println(response);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed by server.");
                }
            });
            readerThread.start();

            // Main input loop
            String msg;
            while (true) {
                if (scanner.hasNextLine()) {
                    msg = scanner.nextLine();
                    if ("/exit".equalsIgnoreCase(msg)) {
                        System.out.println("Closing connection...");
                        break;
                    }
                    out.println(msg);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (scanner != null) scanner.close();
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client shut down.");
        }
    }
}