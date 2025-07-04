// Server.java
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    // Stores all connected users
    private static Map<String, ClientHandler> clients = new HashMap<>();
    // Stores passwords for users
    private static Map<String, String> userPasswords = new HashMap<>();


    // Stores all rooms by name
    private static Map<String, ChatRoom> rooms = new HashMap<>();

    // ChatRoom structure
    static class ChatRoom {
        String roomName;
        String password;
        Set<ClientHandler> members = new HashSet<>();

        ChatRoom(String name, String password) {
            this.roomName = name;
            this.password = password;
        }

        void broadcast(String message, ClientHandler sender) {
            for (ClientHandler member : members) {
                if (member != sender) {
                    member.out.println(message);
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + "...");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handles each client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private ChatRoom currentRoom = null;
        private Set<String> friends = new HashSet<>();
        private String privateTarget = null;

        private void handleFriendMenu() throws IOException {
            while (true) {
                //  // Check if this user has been targeted by someone (to be able to reply back)
                // if (privateTarget != null) {
                //     out.println("You are now chatting privately with " + privateTarget + ". Type /back to return to main menu.");

                //     while (true) {
                //         String msg = in.readLine();
                //         if (msg.equalsIgnoreCase("/back")) {
                //             out.println("You left the private chat with " + privateTarget + ".");
                //             privateTarget = null;
                //             break;
                //         }

                //         ClientHandler targetHandler = clients.get(privateTarget);
                //         if (targetHandler != null) {
                //             targetHandler.out.println("[" + username + "]: " + msg);
                //         } else {
                //             out.println("Friend is not online.");
                //             privateTarget = null;
                //             break;
                //         }
                //     }

                //     // go back to main menu loop
                //     continue;
                // }
                out.println("\nFriend Menu:");
                out.println("1. View friends");
                out.println("2. Add friend");
                out.println("3. Message a friend");
                out.println("4. Back to main menu");
                out.println("Enter: ");

                String input = in.readLine();
                if ("1".equals(input)) {
                    out.println("Your friends: " + friends);
                } else if ("2".equals(input)) {
                    out.println("Enter username to add:");
                    String friendName = in.readLine();
                    if (clients.containsKey(friendName) && !friendName.equals(username)) {
                        friends.add(friendName);
                        out.println(friendName + " has been added to your friend list.");
                    } else {
                        out.println("User not found.");
                    }
                } else if ("3".equals(input)) {
                    out.println("Enter a user username:");
                    String target = in.readLine();

                    if (friends.contains(target)) {
                        ClientHandler targetHandler = clients.get(target);

                        if (targetHandler != null) {
                            out.println("Start messaging " + target + " (type /back to stop):");

                            // Set each other's a direct reply back and forth
                            privateTarget = target;
                            targetHandler.privateTarget = this.username;

                            while (true) {
                                String msg = in.readLine();
                                if (msg.equalsIgnoreCase("/back")) {
                                    privateTarget = null;
                                    targetHandler.privateTarget = null;
                                    break;
                                }

                                targetHandler = clients.get(privateTarget);

                                if (targetHandler != null) {
                                    targetHandler.out.println("[" + username + "]: " + msg);
                                } else {
                                    out.println("Friend is no longer online.");
                                    privateTarget = null;
                                    break;
                                }
                            }
                        }

                    } else {
                        out.println("User is not in your friend list.");
                    }
                } else if ("4".equals(input)) {
                    break;
                } else {
                    out.println("Invalid option.");
                }
            }
        }
    
        // Connect clients to server
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void showAllRooms() {
            synchronized (rooms) {  // Add thread safety
                if (rooms.isEmpty()) {
                    out.println("No rooms available.");
                } else {
                    out.println("Available Rooms:");
                    for (Map.Entry<String, ChatRoom> entry : rooms.entrySet()) {
                        String name = entry.getKey();
                        int memberCount = entry.getValue().members.size();
                        out.println("- " + name + " (" + memberCount + " members)");
                    }
                }
            }
            out.flush();
        }

        // Run
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Login / Register
                while (true) {
                    out.println("Enter your username:");
                    username = in.readLine();

                    if (username == null || username.trim().isEmpty()) {
                        out.println("Invalid username.");
                        continue;
                    }

                    synchronized (userPasswords) {
                        if (userPasswords.containsKey(username)) {
                            out.println("Username exists. Enter your password:");
                            String enteredPassword = in.readLine();
                            if (!userPasswords.get(username).equals(enteredPassword)) {
                                out.println("Wrong password. Try again.\n");
                                continue;
                            }
                        } else {
                            out.println("New user. Set your password:");
                            String newPassword = in.readLine();
                            if (newPassword == null || newPassword.trim().isEmpty()) {
                                out.println("Password cannot be empty.");
                                continue;
                            }
                            userPasswords.put(username, newPassword);
                            out.println("User registered successfully.");
                        }
                    }

                    // Check if user is already connected
                    synchronized (clients) {
                        if (clients.containsKey(username)) {
                            out.println("User already logged in. Connection closing.");
                            socket.close();
                            return;
                        }
                        clients.put(username, this);
                    }

                    out.println("Login successful. Welcome, " + username + "!");
                    break;
                }


                // Options
                while (true) {
                    out.println("Do you want to:");
                    out.println("1. Join a Room");
                    out.println("2. Create a Room");
                    out.println("3. Friend Menu");
                    out.println("Enter: ");

                    String option = in.readLine();

                   if ("1".equals(option)) {
                        showAllRooms();
                        // First show rooms and verify there are rooms available
                        if (rooms.isEmpty()) {
                            out.println("No rooms exist. Please create one first.");
                            continue;  // Go back to main menu
                        }
                        
                        out.println("Enter room name (or /back to cancel):");
                        String roomName = in.readLine();
                        
                        if ("/back".equalsIgnoreCase(roomName)) {
                            continue;  // Return to main menu
                        }

                        synchronized (rooms) {
                            ChatRoom room = rooms.get(roomName);
                            if (room == null) {
                                out.println("Room does not exist. Try again or type /back to go back.");
                                continue;
                            }

                            out.println("Enter room password:");
                            String pass = in.readLine();

                            if (!room.password.equals(pass)) {
                                out.println("Incorrect password. Try again.");
                                continue;
                            }

                            room.members.add(this);
                            currentRoom = room;
                            out.println("Joined room: " + roomName);
                            room.broadcast(username + " joined the room.", this);
                            break;
                        }
                    } else if ("2".equals(option)) {
                        out.println("Enter new room name:");
                        String roomName = in.readLine();

                        synchronized (rooms) {
                            if (rooms.containsKey(roomName)) {
                                out.println("Room already exists. Choose another name.");
                                continue;
                            }

                            out.println("Set room password:");
                            String pass = in.readLine();

                            ChatRoom room = new ChatRoom(roomName, pass);
                            rooms.put(roomName, room);
                            room.members.add(this);
                            currentRoom = room;
                            out.println("Room created. You joined: " + roomName);
                            break;
                        }
                    } else if ("3".equals(option)) {
                        handleFriendMenu();
                    } else {
                        out.println("Invalid option.");
                    }
                }

                // Main message loop
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equalsIgnoreCase("/exit")) {
                        break;
                    }

                    // Room chat
                    if (currentRoom != null) {
                        currentRoom.broadcast(username + ": " + msg, this);
                    }              
                }

            } catch (IOException e) {
                System.out.println("Connection error with user: " + username);
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException e) {}

                // Cleanup
                synchronized (clients) {
                    clients.remove(username);
                }

                if (currentRoom != null) {
                    synchronized (rooms) {
                        currentRoom.members.remove(this);
                        currentRoom.broadcast(username + " left the room.", this);
                    }
                }

                System.out.println("User " + username + " disconnected.");
            }
        }

    }
}
