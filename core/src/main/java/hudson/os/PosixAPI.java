package hudson.os;

import org.jruby.ext.posix.JavaPOSIX;
import org.jruby.ext.posix.POSIX;
import org.jruby.ext.posix.POSIXFactory;
import org.jruby.ext.posix.POSIXHandler;
import org.jruby.ext.posix.POSIX.ERRORS;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

/**
 * POSIX API wrapper.
 * 
 * @author Kohsuke Kawaguchi
 */
public class PosixAPI {
    public static POSIX get() {
        return posix;
    }

    /**
     * @deprecated as of 1.448
     *      Use {@link #supportsNative()}.
     */
    public boolean isNative() {
        return supportsNative();
    }

    /**
     * Determine if the jna-posix library could not provide native support, and
     * used a fallback java implementation which does not support many operations.
     */
    public static boolean supportsNative() {
        return !(posix instanceof JavaPOSIX);
    }
    
    private static final POSIX posix = POSIXFactory.getPOSIX(new POSIXHandler() {
        public void error(ERRORS errors, String s) {
            throw new PosixException(s,errors);
        }

        public void unimplementedError(String s) {
            throw new UnsupportedOperationException(s);
        }

        public void warn(WARNING_ID warning_id, String s, Object... objects) {
            LOGGER.fine(s);
        }

        public boolean isVerbose() {
            return true;
        }

        public File getCurrentWorkingDirectory() {
            // TODO
            throw new UnsupportedOperationException();
        }

        public String[] getEnv() {
            // TODO
            throw new UnsupportedOperationException();
        }

        public InputStream getInputStream() {
            // TODO
            throw new UnsupportedOperationException();
        }

        public PrintStream getOutputStream() {
            // TODO
            throw new UnsupportedOperationException();
        }

        public int getPID() {
            // TODO
            throw new UnsupportedOperationException();
        }

        public PrintStream getErrorStream() {
            // TODO
            throw new UnsupportedOperationException();
        }
    }, true);

    private static final Logger LOGGER = Logger.getLogger(PosixAPI.class.getName());
}
