package me.zpleum.zPleumCORE;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
            Thread t1 = new Thread(() -> {
                try {
                    pipe(clientSocket, finalRconSocket, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            Socket finalRconSocket1 = rconSocket;
            Thread t2 = new Thread(() -> {
                try {
                    pipe(finalRconSocket1, clientSocket, true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
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

    private void pipe(Socket inSock, Socket outSock, boolean injectFakeReply) throws IOException {
        InputStream in = inSock.getInputStream();
        OutputStream out = outSock.getOutputStream();

        while (running) {
            byte[] packet = readPacket(in);
            if (packet == null) break;

            if (injectFakeReply) {
                int requestId = ByteBuffer.wrap(packet, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

                String warn = "[Server] What's up, Nice try hacker!";
                int type = 0; // SERVERDATA_RESPONSE_VALUE

                byte[] payload = warn.getBytes("UTF-8");
                int length = 4 + 4 + payload.length + 2;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeInt(Integer.reverseBytes(length)); // length
                dos.writeInt(Integer.reverseBytes(requestId)); // requestId
                dos.writeInt(Integer.reverseBytes(type)); // type
                dos.write(payload);
                dos.writeByte(0);
                dos.writeByte(0);

                byte[] fakePacket = baos.toByteArray();
                writePacket(out, fakePacket);

                logger.info("Blocked RCON command — Sent fake reply");
                continue;
            }

            writePacket(out, packet);
        }
    }

    private byte[] readPacket(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);

        try {
            int length = Integer.reverseBytes(dis.readInt());
            if (length < 10 || length > 4096) {
                throw new IOException("Invalid packet length: " + length);
            }
            byte[] packet = new byte[length];
            dis.readFully(packet);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(Integer.reverseBytes(length));
            dos.write(packet);

            return baos.toByteArray();
        } catch (EOFException e) {
            return null; // stream end
        }
    }

    private void writePacket(OutputStream out, byte[] packet) throws IOException {
        out.write(packet);
        out.flush();
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
