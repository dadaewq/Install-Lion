package com.modosa.apkinstaller.utils.shell;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public interface Shell {

    boolean isAvailable();

    Result exec(Command command);

    Result exec(Command command, InputStream inputPipe);

    class Command {
        final ArrayList<String> mArgs = new ArrayList<>();

        public Command(String command, String... args) {
            mArgs.add(command);
            mArgs.addAll(Arrays.asList(args));
        }

        String[] toStringArray() {
            String[] array = new String[mArgs.size()];

            for (int i = 0; i < mArgs.size(); i++) {
                array[i] = mArgs.get(i);
            }

            return array;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < mArgs.size(); i++) {
                String arg = mArgs.get(i);

                if (arg.contains(" ")) {
                    sb.append('"').append(arg).append('"');
                } else {
                    sb.append(arg);
                }

                if (i < mArgs.size() - 1) {
                    sb.append(" ");
                }
            }

            return sb.toString();
        }

    }

    class Result {
        public final String out;
        final int exitCode;
        final String err;
        final Command cmd;

        Result(Command cmd, int exitCode, String out, String err) {
            this.cmd = cmd;
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }

        public boolean isSuccessful() {
            return exitCode == 0;
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public String toString() {
            return String.format("Command: %s\nExit code: %d\nOut:\n%s\n=============\nErr:\n%s", cmd, exitCode, out, err);
        }
    }

}
