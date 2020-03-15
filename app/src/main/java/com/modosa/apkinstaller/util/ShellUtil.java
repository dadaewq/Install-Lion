package com.modosa.apkinstaller.util;
// Copyright (C) 2018 Bave Lee
// This file is part of Quick-Android.
// https://github.com/Crixec/Quick-Android
//
// Quick-Android is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Quick-Android is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License

import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ShellUtil {

    private static String[] exec(final String sh, final List<String> cmds) {
        final String[] myResult = new String[4];
        Process process;
        DataOutputStream stdin = null;
        OutputReader stdout = null;
        OutputReader stderr = null;
        try {
            process = Runtime.getRuntime().exec(sh);
            stdin = new DataOutputStream(process.getOutputStream());

            ArrayList<String> errlist = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            stdout = new OutputReader(new BufferedReader(new InputStreamReader(process.getInputStream())),
                    text -> {
                        myResult[0] = text;
//                        Log.e("onStdout", myResult[0]);
                    });

            stderr = new OutputReader(new BufferedReader(new InputStreamReader(process.getErrorStream())),
                    errlist::add);

            stdout.start();
            stderr.start();
            for (String cmd : cmds) {
                myResult[2] = cmd;
//                Log.e("cmd", myResult[2]);
                stdin.write(cmd.getBytes());
                stdin.writeBytes("\n");
                stdin.flush();
            }
            stdin.writeBytes("exit $?\n");
            stdin.flush();
            int resultCode = process.waitFor();

            myResult[3] = resultCode + "";
//            Log.e("resultCode", myResult[3] + "");

            if (errlist.size() == 1) {
                myResult[1] = errlist.get(0);
            } else {
                int num = errlist.size() < 6 ? errlist.size() : 5;

                for (int i = 1; i < num; i++) {
                    stringBuilder.append(errlist.get(i)).append("\n");
                }
                myResult[1] = stringBuilder + "";
            }

            Log.e("onStderr===>", myResult[1]);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            safeCancel(stderr);
            safeCancel(stdout);
            safeClose(stdout);
            safeClose(stderr);
            safeClose(stdin);

        }
        return myResult;
    }

    private static void safeCancel(OutputReader reader) {
        try {
            if (reader != null) {
                reader.cancel();
            }
        } catch (Exception ignored) {

        }
    }

    private static void safeClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ignored) {

        }
    }

    private static String[] exec(final List<String> cmds, final boolean isRoot) {
        String sh = isRoot ? "su" : "sh";
        return exec(sh, cmds);
    }


    private static String[] exec(final String cmd, boolean isRoot) {
        List<String> cmds = new ArrayList<>();
        cmds.add(cmd);
        return exec(cmds, isRoot);
    }


    public static String[] execWithRoot(final String cmd) {
        return exec(cmd, true);
    }

    private interface Output {
        void output(String text);
    }

    static class OutputReader extends Thread implements Closeable {
        private final Output output;
        private final BufferedReader reader;
        private boolean isRunning;

        private OutputReader(BufferedReader reader, Output output) {
            this.output = output;
            this.reader = reader;
            this.isRunning = true;
        }

        @Override
        public void close() {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void run() {
            super.run();
            String line;
            while (isRunning) {
                try {
                    line = reader.readLine();
                    if (line != null) {
                        output.output(line);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        private void cancel() {
            synchronized (this) {
                isRunning = false;
                this.notifyAll();
            }
        }
    }
}