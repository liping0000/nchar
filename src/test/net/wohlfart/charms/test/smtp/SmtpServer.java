package net.wohlfart.charms.test.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpServer extends Thread {

    private final static Logger     LOGGER       = LoggerFactory.getLogger(SmtpServer.class);

    /**
     * Stores all of the email received since this instance started up.
     */
    private final List<SmtpMessage> receivedMail = Collections.synchronizedList(new ArrayList<SmtpMessage>());

    /**
     * Indicates whether this server is stopped or not.
     */
    private volatile boolean        stopped      = true;

    /** indicates weather the server is started and running or not */
    private volatile boolean        running      = false;

    /**
     * Handle to the server socket this server listens to.
     */
    private ServerSocket            serverSocket;

    /**
     * Port the server listens on
     */
    private final int               port;

    /**
     * Constructor.
     * 
     * @param port
     *            port number
     */
    public SmtpServer(final int port) {
        this.port = port;
    }

    // this method blocks until we got the port
    public synchronized void begin() {    
        running = false;
        start();
        while (!running) {
            try {wait(5000);} catch (InterruptedException e) {} 
        }
    }

    /**
     * Main loop of the SMTP server.
     */
    @Override
    public void run() {
        stopped = false;
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("starting smtp server on port {}", port);
            while (!stopped) {
                LOGGER.info("accepting connections on port " + port);
                // serverSocket.setSoTimeout(TIMEOUT); // Block for maximum of 1.5 seconds
                running = true;
                final Socket socket = serverSocket.accept();
                LOGGER.info("accepted connection, starting thread");
                new Thread(new MailReceiver(socket, receivedMail)).start();
                LOGGER.info("accepted connection, started thread");
            }
        } catch (final SocketException ex) {
            // this happens when the stop method calls close() on the socket
            // its usually "socket closed"
            if (stopped) {
                return;
            } else {
                LOGGER.warn("problem running smtp server (SocketException): {}", ex);
                throw new RuntimeException("socket exception in smtp server", ex);
            }
        } catch (final IOException ex) {
            LOGGER.warn("problem running smtp server (IOException): {}", ex);
            throw new RuntimeException("io exception in smtp server", ex);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
            running = false;
        }
        LOGGER.info("returning from run method " + port);
    }

    /**
     * Check if the server has been placed in a stopped state. Allows another
     * thread to stop the server safely.
     * 
     * @return true if the server has been sent a stop signal, false otherwise
     */
    public synchronized boolean isStopped() {
        return stopped;
    }

    /**
     * Stops the server. Server is shutdown after processing of the current
     * request is complete.
     */
    public synchronized void end() {
        // Mark us closed
        stopped = true;
        try {
            while (running) {
                if ((serverSocket != null) && (!serverSocket.isClosed())) {
                    // Kick the server accept loop
                    serverSocket.close();
                }
                try {wait(5000);} catch (InterruptedException e) {} 
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get email received by this instance since start up.
     * 
     * @return List of String
     */
    public synchronized Iterator<SmtpMessage> getReceivedEmail() {
        return receivedMail.iterator();
    }

    /**
     * Get the number of messages received.
     * 
     * @return size of received email list
     */
    public synchronized int getReceivedEmailSize() {
        return receivedMail.size();
    }

    /**
     * Creates an instance of BetterSmtpServer and starts it. Will listen on the
     * default port.
     * 
     * @return a reference to the SMTP server public static BetterSmtpServer
     *         start() { return start(DEFAULT_SMTP_PORT); }
     */

    /**
     * Creates an instance of BetterSmtpServer and starts it.
     * 
     * @param port
     *            port number the server should listen to
     * @return a reference to the SMTP server public static BetterSmtpServer
     *         start(int port) { BetterSmtpServer server = new
     *         BetterSmtpServer(port); synchronized (server) { Thread t = new
     *         Thread(server); t.start(); } return server; }
     */

    private static class MailReceiver implements Runnable {

        Socket            socket;
        List<SmtpMessage> receivedMail; // a global list of received mails
        // shared by all receivers

        protected MailReceiver(final Socket socket, final List<SmtpMessage> receivedMail) {
            this.socket = socket;
            this.receivedMail = receivedMail;
        }

        @Override
        public void run() {
            try {
                // Get the input and output streams
                final BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PrintWriter out = new PrintWriter(socket.getOutputStream());
                handleTransaction(out, input);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Send response to client.
         * 
         * @param out
         *            socket output stream
         * @param smtpResponse
         *            response object
         */
        private void sendResponse(final PrintWriter out, final SmtpResponse smtpResponse) {
            if (smtpResponse.getCode() > 0) {
                final int code = smtpResponse.getCode();
                final String message = smtpResponse.getMessage();
                out.print(code + " " + message + "\r\n");
                out.flush();
            }
        }

        /**
         * Handle an SMTP transaction, i.e. all activity between initial connect
         * and QUIT command. and add the mail to the receivedMail list
         * 
         * @param out
         *            output stream
         * @param input
         *            input stream
         * @return List of SmtpMessage
         * @throws IOException
         */
        private void handleTransaction(final PrintWriter out, final BufferedReader input) throws IOException {
            // Initialize the state machine
            SmtpState smtpState = SmtpState.CONNECT;
            final SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "", smtpState);

            // Execute the connection request
            final SmtpResponse smtpResponse = smtpRequest.execute();

            // Send initial response
            sendResponse(out, smtpResponse);
            smtpState = smtpResponse.getNextState();

            final SmtpMessage msg = new SmtpMessage();
            while (smtpState != SmtpState.CONNECT) {
                final String line = input.readLine();

                if (line == null) {
                    break;
                }

                // Create request from client input and current state
                final SmtpRequest request = SmtpRequest.createRequest(line, smtpState);
                // Execute request and create response object
                final SmtpResponse response = request.execute();
                // Move to next internal state
                smtpState = response.getNextState();
                // Send reponse to client
                sendResponse(out, response);

                // Store input in message
                final String params = request.getParams();
                msg.store(response, params);

                // If message reception is complete save it
                if (smtpState == SmtpState.QUIT) {
                    receivedMail.add(msg);
                }
            }
        }

    }
}
