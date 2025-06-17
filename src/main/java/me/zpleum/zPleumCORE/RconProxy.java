package me.zpleum.zPleumCORE;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class RconProxy {
    private final int fakeRconPort;
    private final int realRconPort;
    private final List<String> blacklist;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    private static final Logger logger = Logger.getLogger(RconProxy.class.getName());

    public RconProxy(int fakeRconPort, int realRconPort, List<String> blacklist) {
        this.fakeRconPort = fakeRconPort;
        this.realRconPort = realRconPort;
        this.blacklist = blacklist;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        if (running) {
            logger.warning("RCON Proxy is already running!");
            return;
        }

        try {
            serverSocket = new ServerSocket(fakeRconPort);
            running = true;
            logger.info("RCON Proxy started on port " + fakeRconPort + " -> " + realRconPort);

            // รัน loop รับ connection
            executor.submit(() -> {
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        executor.submit(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (running) {
                            logger.severe("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            });

        } catch (IOException e) {
            logger.severe("Failed to start RCON Proxy: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdownNow();
            logger.info("RCON Proxy stopped");
        } catch (IOException e) {
            logger.severe("Error stopping RCON Proxy: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        Socket rconSocket = null;
        try {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            logger.info("Client connected from: " + clientIP);

            if (blacklist != null && blacklist.contains(clientIP)) {
                logger.warning("Blocked blacklisted IP: " + clientIP);
                clientSocket.close();
                return;
            }

            // ต่อไปยัง RCON จริง
            rconSocket = new Socket("localhost", realRconPort);

            // สร้าง forwarding สองทาง
            Socket finalRconSocket = rconSocket;
            Thread t1 = new Thread(() -> pipe(clientSocket, finalRconSocket));
            Socket finalRconSocket1 = rconSocket;
            Thread t2 = new Thread(() -> pipe(finalRconSocket1, clientSocket));
            t1.start();
            t2.start();

            t1.join();
            t2.join();

        } catch (Exception e) {
            logger.warning("Proxy handler error: " + e.getMessage());
        } finally {
            closeQuietly(clientSocket);
            closeQuietly(rconSocket);
        }
    }

    private void pipe(Socket inSock, Socket outSock) {
        try (InputStream in = inSock.getInputStream();
             OutputStream out = outSock.getOutputStream()) {

            byte[] buf = new byte[4096];
            int len;
            while (running && (len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(Socket sock) {
        if (sock != null && !sock.isClosed()) {
            try { sock.close(); } catch (IOException ignored) {}
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getFakeRconPort() {
        return fakeRconPort;
    }

    public int getRealRconPort() {
        return realRconPort;
    }
}
