package net.simplehardware.engine.core;

import java.io.*;
import java.util.concurrent.*;

/**
 * Wrapper for a player JAR process that handles I/O communication
 */
public class PlayerProcess {
    private final int playerId;
    private final Process process;
    private final BufferedReader stdoutReader;
    private final BufferedReader stderrReader;
    private final PrintWriter stdinWriter;
    private final ExecutorService executor;
    private volatile boolean timedOut;
    private final StringBuilder stdoutBuffer = new StringBuilder();
    private final StringBuilder stderrBuffer = new StringBuilder();

    public PlayerProcess(int playerId, String jarPath) throws IOException {
        this.playerId = playerId;
        this.executor = Executors.newFixedThreadPool(2);

        File policyFile = new File("bot.policy");
        String policyPath = policyFile.getAbsolutePath();

        String absJarPath = new File(jarPath).getAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Djava.security.manager",
                "-Djava.security.policy=" + policyPath,
                "-Dbot.jar.path=" + absJarPath,
                "-jar",
                jarPath);
        this.process = pb.start();
        this.stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        this.stdinWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
        this.timedOut = false;

        startStderrCapture();
    }

    public void resetIO() {
        stdoutBuffer.setLength(0);
        stderrBuffer.setLength(0);
    }

    private void startStderrCapture() {
        executor.submit(() -> {
            try {
                String line;
                while ((line = stderrReader.readLine()) != null) {
                    synchronized (stderrBuffer) {
                        stderrBuffer.append(line).append("\n");
                    }
                }
            } catch (IOException ignored) { }
        });
    }

    public void sendLine(String line) {
        stdinWriter.println(line);
        stdinWriter.flush();
    }

    public String readLine(long timeoutMs) throws TimeoutException {
        Future<String> future = executor.submit(() -> {
            try {
                String line = stdoutReader.readLine();
                if (line != null) {
                    synchronized (stdoutBuffer) {
                        stdoutBuffer.append(line).append("\n");
                    }
                }
                return line;
            } catch (IOException e) {
                return null;
            }
        });

        try {
            String result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result == null) {
                timedOut = true;
                throw new TimeoutException("Player " + playerId + " disconnected");
            }
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            timedOut = true;
            throw e;
        } catch (InterruptedException | ExecutionException e) {
            timedOut = true;
            throw new TimeoutException("Player " + playerId + " error: " + e.getMessage());
        }
    }

    public String getStdout() {
        synchronized (stdoutBuffer) {
            return stdoutBuffer.toString();
        }
    }

    public String getStderr() {
        synchronized (stderrBuffer) {
            return stderrBuffer.toString();
        }
    }

    public void clearOutput() {
        synchronized (stdoutBuffer) {
            stdoutBuffer.setLength(0);
        }
        synchronized (stderrBuffer) {
            stderrBuffer.setLength(0);
        }
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void destroy() {
        try {
            executor.shutdownNow();
            stdinWriter.close();
            stdoutReader.close();
            stderrReader.close();
            process.destroyForcibly();
        } catch (IOException e) {
            System.err.println("Error closing player process: " + e.getMessage());
        }
    }

    /**
     * Check if there is more output available to read without blocking
     */
    public boolean hasMoreOutput() throws IOException {
        return stdoutReader.ready();
    }

    /**
     * Read a line from stdout without blocking if available
     * 
     * @return the line read, or null if no line is available
     */
    public String readLineNonBlocking() throws IOException {
        if (stdoutReader.ready()) {
            String line = stdoutReader.readLine();
            if (line != null) {
                synchronized (stdoutBuffer) {
                    stdoutBuffer.append(line).append("\n");
                }
            }
            return line;
        }
        return null;
    }

    /**
     * Drain all available output from stdout buffer
     * This prevents buffer accumulation when player outputs multiple lines
     */
    public void drainStdout() throws IOException {
        while (stdoutReader.ready()) {
            readLineNonBlocking();
        }
    }
}
