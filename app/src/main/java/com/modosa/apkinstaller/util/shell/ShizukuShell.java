package com.modosa.apkinstaller.util.shell;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.modosa.apkinstaller.util.IOUtils;
import com.modosa.apkinstaller.util.ResultUtil;

import java.io.InputStream;
import java.io.OutputStream;

import moe.shizuku.api.RemoteProcess;
import moe.shizuku.api.ShizukuService;

public class ShizukuShell implements Shell {
    private static final String TAG = "ShizukuShell";

    private static ShizukuShell sInstance;

    private ShizukuShell() {
        sInstance = this;
    }

    public static ShizukuShell getInstance() {
        synchronized (ShizukuShell.class) {
            return sInstance != null ? sInstance : new ShizukuShell();
        }
    }

    @Override
    public boolean isAvailable() {
        if (!ShizukuService.pingBinder()) {
            return false;
        }

        try {
            return exec(new Command("echo", "test")).isSuccessful();
        } catch (Exception e) {
            Log.w(TAG, "Unable to access shizuku: ");
            Log.w(TAG, e);
            return false;
        }
    }

    @Override
    public Result exec(Command command) {
        return execInternal(command, null);
    }

    @Override
    public Result exec(Command command, InputStream inputPipe) {
        return execInternal(command, inputPipe);
    }

    @Override
    public String makeLiteral(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private Result execInternal(Command command, @Nullable InputStream inputPipe) {
        // inputPipe 值恒为零

        try {
            Command.Builder shCommand = new Command.Builder("sh", "-c", command.toString());

            RemoteProcess process = ShizukuService.newProcess(shCommand.build().toStringArray(), null, null);

            String err = IOUtils.toString(process.getErrorStream());
            String out = IOUtils.toString(process.getInputStream());

            if (inputPipe != null && process.alive()) {
                try (OutputStream outputStream = process.getOutputStream(); InputStream inputStream = inputPipe) {
                    IOUtils.copyStream(inputStream, outputStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            process.waitFor();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.destroyForcibly();
            } else {
                process.destroy();
            }

            return new Result(command, process.exitValue(), out.trim(), err.trim());
        } catch (Exception e) {
            Log.w(TAG, "Unable execute command: ");
            Log.w(TAG, e);
            return new Result(command, -1, "", "\n\n<!> SAI ShizukuShell Java exception: " + ResultUtil.throwableToString(e));
        }
    }
}
