
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * InternshipProject.java
 *
 * Contains all four tasks:
 * 1. File Handling
 * 2. REST API Fetch + JSON Parsing
 * 3. Client-Server Chat Application
 * 4. Recommendation System
 *
 * How to run:
 *   javac InternshipProject.java
 *   java InternshipProject <mode>
 *
 * Modes:
 *   1      -> Run Task 1: File Handling
 *   2      -> Run Task 2: REST API Fetch
 *   server -> Run Task 3: Start Chat Server
 *   client <name> -> Run Task 3: Start Chat Client
 *   4      -> Run Task 4: Recommendation System
 */

public class InternshipProject {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        try {
            switch (mode) {
                case "1":
                    Task1_FileHandling.run();
                    break;
                case "2":
                    Task2_RestApi.run("https://jsonplaceholder.typicode.com/todos/1");
                    break;
                case "server":
                    Task3_ChatApp.ChatServer.runServer(12345);
                    break;
                case "client":
                    String name = args.length >= 2 ? args[1] : "Guest";
                    Task3_ChatApp.ChatClient.runClient("localhost", 12345, name);
                    break;
                case "4":
                    Task4_Recommendation.run();
                    break;
                default:
                    printUsage();
            }
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java InternshipProject 1       -> Run Task 1 (File Handling)");
        System.out.println("  java InternshipProject 2       -> Run Task 2 (REST API Fetch)");
        System.out.println("  java InternshipProject server  -> Run Task 3 (Start Chat Server)");
        System.out.println("  java InternshipProject client <YourName> -> Run Task 3 (Chat Client)");
        System.out.println("  java InternshipProject 4       -> Run Task 4 (Recommendation System)");
    }

    // ------------------------------
    // Task 1: File Handling
    // ------------------------------
    static class Task1_FileHandling {
        public static void run() {
            System.out.println("=== Task 1: File Handling ===");
            String fileName = "sample.txt";

            writeToFile(fileName, "Hello! This is the first line in the file.\n");
            System.out.println("-- After Writing --");
            readFromFile(fileName);

            appendToFile(fileName, "This is a new line added later.\n");
            System.out.println("-- After Appending --");
            readFromFile(fileName);

            System.out.println("Task 1 completed. Check 'sample.txt'.");
        }

        private static void writeToFile(String fileName, String data) {
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(data);
                System.out.println("File written successfully.");
            } catch (IOException e) {
                System.out.println("Error writing to file: " + e.getMessage());
            }
        }

        private static void readFromFile(String fileName) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        }

        private static void appendToFile(String fileName, String data) {
            try (FileWriter writer = new FileWriter(fileName, true)) {
                writer.write(data);
                System.out.println("File appended successfully.");
            } catch (IOException e) {
                System.out.println("Error appending to file: " + e.getMessage());
            }
        }
    }

    // ------------------------------
    // Task 2: REST API Fetch
    // ------------------------------
    static class Task2_RestApi {
        public static void run(String apiUrl) {
            System.out.println("=== Task 2: REST API Fetch ===");
            try {
                String response = httpGet(apiUrl);
                if (response == null) {
                    System.out.println("No response received.");
                    return;
                }

                System.out.println("Raw JSON Response:\n" + response);

                // Simple parsing
                System.out.println("\nParsed Fields:");
                System.out.println("User ID: " + extractValue(response, "userId"));
                System.out.println("ID: " + extractValue(response, "id"));
                System.out.println("Title: " + extractValue(response, "title"));
                System.out.println("Completed: " + extractValue(response, "completed"));
            } catch (Exception e) {
                System.out.println("Error in REST API fetch: " + e.getMessage());
            }
        }

        private static String httpGet(String urlStr) throws IOException {
            URI uri = URI.create(urlStr);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                System.out.println("HTTP error: " + code);
                return null;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        }

        private static String extractValue(String json, String key) {
            int start = json.indexOf(key);
            if (start == -1) return "Not found";
            int colon = json.indexOf(":", start);
            int comma = json.indexOf(",", colon);
            int endBrace = json.indexOf("}", colon);
            int end = (comma == -1) ? endBrace : Math.min(comma, endBrace);
            return json.substring(colon + 1, end).replace(""", "").trim();
        }
    }

    // ------------------------------
    // Task 3: Chat Application
    // ------------------------------
    static class Task3_ChatApp {

        static class ChatServer {
            private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

            public static void runServer(int port) {
                System.out.println("=== Task 3: Chat Server ===");
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    System.out.println("Server running on port " + port);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket);
                        clients.add(handler);
                        new Thread(handler).start();
                    }
                } catch (IOException e) {
                    System.out.println("Server error: " + e.getMessage());
                }
            }

            static class ClientHandler implements Runnable {
                private Socket socket;
                private PrintWriter out;
                private BufferedReader in;
                private String name = "Guest";

                public ClientHandler(Socket socket) {
                    this.socket = socket;
                }

                public void run() {
                    try {
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("Enter your name:");
                        name = in.readLine();

                        broadcast(name + " joined the chat.", this);

                        String message;
                        while ((message = in.readLine()) != null) {
                            if (message.equalsIgnoreCase("/quit")) break;
                            broadcast(name + ": " + message, this);
                        }
                    } catch (IOException e) {
                        System.out.println("Connection error: " + e.getMessage());
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException ignored) {}
                        clients.remove(this);
                        broadcast(name + " left the chat.", this);
                    }
                }

                private void broadcast(String msg, ClientHandler sender) {
                    for (ClientHandler client : clients) {
                        if (client != sender) {
                            client.out.println(msg);
                        }
                    }
                }
            }
        }

        static class ChatClient {
            public static void runClient(String host, int port, String name) {
                try (Socket socket = new Socket(host, port);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

                    out.println(name);
                    System.out.println("Connected to chat as " + name);

                    Thread reader = new Thread(() -> {
                        try {
                            String line;
                            while ((line = in.readLine()) != null) {
                                System.out.println(line);
                            }
                        } catch (IOException e) {
                            System.out.println("Disconnected from server.");
                        }
                    });
                    reader.start();

                    String msg;
                    while ((msg = console.readLine()) != null) {
                        out.println(msg);
                        if (msg.equalsIgnoreCase("/quit")) break;
                    }

                } catch (IOException e) {
                    System.out.println("Client error: " + e.getMessage());
                }
            }
        }
    }

    // ------------------------------
    // Task 4: Recommendation System
    // ------------------------------
    static class Task4_Recommendation {
        public static void run() {
            System.out.println("=== Task 4: Recommendation System ===");

            // Sample data
            Map<String, List<String>> userPreferences = new HashMap<>();
            userPreferences.put("Alice", Arrays.asList("Shoes", "Bags", "Watches"));
            userPreferences.put("Bob", Arrays.asList("Shoes", "Belts", "Sunglasses"));
            userPreferences.put("Charlie", Arrays.asList("Watches", "Bags", "Sunglasses"));

            String currentUser = "Alice";
            System.out.println("Current user: " + currentUser);

            // Find similar user
            String similarUser = null;
            int maxCommon = 0;

            for (Map.Entry<String, List<String>> entry : userPreferences.entrySet()) {
                if (entry.getKey().equals(currentUser)) continue;
                int common = 0;
                for (String item : entry.getValue()) {
                    if (userPreferences.get(currentUser).contains(item)) {
                        common++;
                    }
                }
                if (common > maxCommon) {
                    maxCommon = common;
                    similarUser = entry.getKey();
                }
            }

            if (similarUser != null) {
                System.out.println("Most similar user: " + similarUser);
                List<String> recommendations = new ArrayList<>(userPreferences.get(similarUser));
                recommendations.removeAll(userPreferences.get(currentUser));
                System.out.println("Recommended items for " + currentUser + ": " + recommendations);
            } else {
                System.out.println("No similar users found.");
            }
        }
    }
}
