package org.sprinting.hardware;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Spawns hardware_bridge.py and sends sprint/working/idle commands each epoch.
 *
 * Usage:
 *   HardwareBridge hw = new HardwareBridge("python3", "hardware_bridge.py");
 *   hw.start();
 *   hw.sendEpoch(epoch, ServerState.SPRINTING);  // call once per epoch
 *   hw.stop();                                    // on shutdown
 */
public class HardwareBridge {

    public enum ServerState {
        SPRINTING, WORKING, IDLE;

        public String toWire() {
            return name().toLowerCase();
        }
    }

    private Process process;
    private BufferedWriter toHardware;
    private BufferedReader fromHardware;
    private boolean running = false;

    private final String pythonCmd;
    private final String scriptPath;
    private final String[] extraArgs;

    /**
     * @param pythonCmd  e.g. "python3"
     * @param scriptPath e.g. "hardware_bridge.py" (relative to project root)
     * @param extraArgs  e.g. "--dry-run" or "--sprint-voltage", "15.0"
     */
    public HardwareBridge(String pythonCmd, String scriptPath, String... extraArgs) {
        this.pythonCmd = pythonCmd;
        this.scriptPath = scriptPath;
        this.extraArgs = extraArgs;
    }

    public void start() throws IOException {
        String[] cmd = new String[2 + extraArgs.length];
        cmd[0] = pythonCmd;
        cmd[1] = scriptPath;
        System.arraycopy(extraArgs, 0, cmd, 2, extraArgs.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Python logs -> console
        process = pb.start();

        toHardware = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        fromHardware = new BufferedReader(new InputStreamReader(process.getInputStream()));
        running = true;

        System.out.println("[HardwareBridge] Started");
    }

    /**
     * Tell the power supply what state this epoch is in: SPRINTING, WORKING, or IDLE.
     * Blocks until Python acknowledges.
     */
    public boolean sendEpoch(int epoch, ServerState state) {
        if (!running) return false;

        try {
            String json = String.format("{\"epoch\":%d,\"state\":\"%s\"}", epoch, state.toWire());
            toHardware.write(json);
            toHardware.newLine();
            toHardware.flush();

            String response = fromHardware.readLine();
            if (response == null) {
                System.err.println("[HardwareBridge] Python process closed");
                running = false;
                return false;
            }
            return response.contains("\"ack\":true") || response.contains("\"ack\": true");

        } catch (IOException e) {
            System.err.println("[HardwareBridge] Error: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        running = false;
        try {
            if (toHardware != null) toHardware.close();
            if (process != null) {
                process.waitFor(3, TimeUnit.SECONDS);
                if (process.isAlive()) process.destroyForcibly();
            }
        } catch (Exception e) {
            System.err.println("[HardwareBridge] Shutdown error: " + e.getMessage());
        }
        System.out.println("[HardwareBridge] Stopped");
    }

    public boolean isRunning() {
        return running && process != null && process.isAlive();
    }
}