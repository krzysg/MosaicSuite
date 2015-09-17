package mosaic.core.utils;


import ij.IJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.cluster.ShellProcessOutput;


/**
 * Utility class to run shell command All function members are static
 *
 * @author Pietro Incardona
 */
public class ShellCommand {

    /**
     * Produce a shell command from cmd
     *
     * @param cmd command to be run
     * @return
     */
    private static String[] produceShell(String cmd) {
        if (IJ.isLinux() == true || IJ.isMacOSX() == true) {
            // we can use bash
            return new String[] { "bash", "-c", cmd };
        }
        else {
            return new String[] { "cmd", cmd };
        }
    }

    /**
     * Execute a command without printout
     *
     * @param cmd Command
     * @throws IOException
     * @throws InterruptedException
     */
    public static void exeCmdNoPrint(String cmd) throws IOException, InterruptedException {
        String cmd_[] = produceShell(cmd);
        Process tProcess = Runtime.getRuntime().exec(cmd_);

        tProcess.waitFor();
    }

    /**
     * Execute a command and get output as String
     *
     * @param cmd
     * @throws IOException
     * @throws InterruptedException
     */

    static String exeCmdString(String cmd) throws IOException, InterruptedException {
        String cmd_[] = produceShell(cmd);
        Process tProcess = Runtime.getRuntime().exec(cmd_);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(tProcess.getInputStream()));

        String out = new String();
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
            out += s;
        }

        tProcess.waitFor();
        return out;
    }

    /**
     * Execute a command and print
     *
     * @param cmd
     * @throws IOException
     * @throws InterruptedException
     */
    public static void exeCmd(String cmd) throws IOException, InterruptedException {
        String cmd_[] = produceShell(cmd);
        Process tProcess = Runtime.getRuntime().exec(cmd_);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(tProcess.getInputStream()));

        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        tProcess.waitFor();
    }

    /**
     * Execute a command, with a defined working dir and defined environment
     * variables (System environment variables are appended)
     *
     * @param cmd
     * @param wdir
     * @param env
     * @throws IOException
     * @throws InterruptedException
     */
    public static void exeCmd(String cmd, File wdir, String env[]) throws IOException, InterruptedException {
        Map<String, String> envi = System.getenv();
        if (env == null) {
            env = new String[0];
        }
        String[] envi_p_env = new String[envi.size() + env.length];

        int i = 0;
        for (String envName : envi.keySet()) {
            envi_p_env[i] = new String(envName + "=" + envi.get(envName));
            i++;
        }

        for (String envName : env) {
            envi_p_env[i] = new String(envName);
            i++;
        }

        String cmd_[] = produceShell(cmd);
        Process tProcess = Runtime.getRuntime().exec(cmd_, envi_p_env, wdir);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(tProcess.getInputStream()));

        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        tProcess.waitFor();
    }

    /**
     * Execute a command, with a defined working dir and defined environment
     * variables (System environment variables are appended)
     *
     * @param cmd
     * @param wdir
     * @param env
     * @param out
     * @throws IOException
     * @throws InterruptedException
     */
    public static void exeCmd(String cmd, File wdir, String env[], ShellProcessOutput out) throws IOException, InterruptedException {
        Map<String, String> envi = System.getenv();
        if (env == null) {
            env = new String[0];
        }
        String[] envi_p_env = new String[envi.size() + env.length];

        int i = 0;
        for (String envName : envi.keySet()) {
            envi_p_env[i] = new String(envName + "=" + envi.get(envName));
            i++;
        }

        for (String envName : env) {
            envi_p_env[i] = new String(envName);
            i++;
        }

        String cmd_[] = produceShell(cmd);
        Process tProcess = Runtime.getRuntime().exec(cmd_, envi_p_env, wdir);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(tProcess.getInputStream()));

        String s_full = new String();
        String s;
        while ((s = stdInput.readLine()) != null) {
            s_full += s + "\n";
            if (out != null) {
                s_full = out.Process(s_full);
            }
            System.out.println(s);
        }

        tProcess.waitFor();
    }

    /**
     * Copy one directory/file recursively
     *
     * @param from dir source
     * @param to dir destination
     * @param Optionally a progress bar window
     */
    public static void copy(File from, File to, ProgressBarWin wn) {
        File[] f = from.listFiles();

        if (f == null) {
            return;
        }

        int cnt = 0;

        for (File t : f) {
            if (wn != null) {
                if (t != null) {
                    wn.SetStatusMessage("Copy: " + t.getName());
                }
                wn.SetProgress(cnt / f.length);
            }

            try {
                exeCmd("cp -R " + t.getAbsoluteFile() + " " + to.getAbsolutePath());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            cnt++;
        }
    }
}
