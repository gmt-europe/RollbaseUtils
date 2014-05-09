package nl.gmt.rollbase.logging;

import org.apache.commons.lang.Validate;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.io.PrintStream;

public class RedirectAppender extends AppenderSkeleton {
    private static Level level = Level.WARN;

    public static void setLevel(Level level) {
        Validate.notNull(level, "level");

        RedirectAppender.level = level;
    }

    @Override
    protected void append(LoggingEvent event) {
        if (event.getLevel().toInt() >= level.toInt()) {
            PrintStream out;

            if (event.getLevel().toInt() >= Level.WARN.toInt()) {
                out = System.err;
            } else {
                out = System.out;
            }

            out.println(String.format("[%s] %s (%s)", event.getLevel(), event.getRenderedMessage(), event.getLoggerName()));
        }
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {
    }
}
