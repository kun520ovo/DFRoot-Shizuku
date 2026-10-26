package df.root.shizuku.cn;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/** One manual run's completion and reboot decision, shared with bootstrap. */
final class SoftRebootGate implements Closeable {
    static final String FILE_NAME = "soft_reboot_gate";
    static final int WAIT = 0, ALLOW = 1, CANCEL = 2;
    static final int PENDING = 0, READY = 1, REQUESTED = 2, FAILED = 3, TIMED_OUT = 4;
    static final int LOAD_PENDING = 0, LOAD_COMPLETE = 1, LOAD_FAILED = 2, LOAD_TIMED_OUT = 3;
    private static final int AUTH_OFFSET = 4, RESULT_OFFSET = 5, LOAD_OFFSET = 6, ACTIVE_OFFSET = 7;
    private final RandomAccessFile file;
    private boolean closed;

    SoftRebootGate(File dataDirectory) throws IOException {
        File pending = File.createTempFile("soft-reboot-", ".tmp", dataDirectory);
        try {
            try (RandomAccessFile opened = new RandomAccessFile(pending, "rw")) {
                // Completion never grants reboot permission. The last byte cancels both waits.
                opened.write(new byte[]{'D', 'F', 'R', '2', WAIT, PENDING, LOAD_PENDING, 1});
                opened.getFD().sync();
            }
            // Never truncate a previous run's inode while bootstrap may still map it.
            Files.move(pending.toPath(), new File(dataDirectory, FILE_NAME).toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            file = new RandomAccessFile(new File(dataDirectory, FILE_NAME), "rw");
        } catch (IOException e) {
            Files.deleteIfExists(pending.toPath());
            throw e;
        }
    }

    static void clearForBoot(File dataDirectory) throws IOException {
        Files.deleteIfExists(new File(dataDirectory, FILE_NAME).toPath());
    }

    boolean authorize(boolean requested, int rootCode, boolean restored, boolean recoveryPending)
            throws IOException {
        if (closed) throw new IOException("reboot gate is closed");
        boolean allowed = requested && rootCode == 0 && restored && !recoveryPending;
        file.seek(AUTH_OFFSET);
        file.writeByte(allowed ? ALLOW : CANCEL);
        return allowed;
    }

    int awaitResult(long timeoutMillis) throws IOException, InterruptedException {
        if (closed) throw new IOException("reboot gate is closed");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        do {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            file.seek(ACTIVE_OFFSET);
            if (file.readUnsignedByte() != 1) throw new IOException("run acknowledgement is cancelled");
            file.seek(RESULT_OFFSET);
            int result = file.readUnsignedByte();
            if (result == REQUESTED || result == FAILED || result == TIMED_OUT) return result;
            if (result != PENDING && result != READY) throw new IOException("invalid reboot status");
            if (System.nanoTime() >= deadline) break;
            Thread.sleep(50);
        } while (true);
        return TIMED_OUT;
    }

    int awaitManagerCompletion(long timeoutMillis) throws IOException, InterruptedException {
        if (closed) throw new IOException("reboot gate is closed");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        do {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            file.seek(LOAD_OFFSET);
            int result = file.readUnsignedByte();
            if (file.readUnsignedByte() != 1) throw new IOException("run acknowledgement is cancelled");
            if (result == LOAD_COMPLETE || result == LOAD_FAILED || result == LOAD_TIMED_OUT) return result;
            if (result != LOAD_PENDING) throw new IOException("invalid manager completion status");
            // A bootstrap that cannot map or validate this run only reports the existing failure byte.
            file.seek(RESULT_OFFSET);
            int rebootResult = file.readUnsignedByte();
            if (rebootResult == FAILED) return LOAD_FAILED;
            if (rebootResult == TIMED_OUT) return LOAD_TIMED_OUT;
            if (rebootResult != PENDING && rebootResult != READY) {
                throw new IOException("unexpected reboot status while waiting for manager");
            }
            if (System.nanoTime() >= deadline) break;
            Thread.sleep(50);
        } while (true);
        return LOAD_TIMED_OUT;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        try {
            file.seek(ACTIVE_OFFSET);
            file.writeByte(0);
        } finally {
            try {
                file.seek(AUTH_OFFSET);
                file.writeByte(CANCEL);
            } finally {
                closed = true;
                file.close();
            }
        }
        // Keep the cancelled record for a delayed bootstrap. The next run replaces
        // it atomically, or the independent boot path explicitly removes it.
    }
}
