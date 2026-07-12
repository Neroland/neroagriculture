package za.co.neroland.neroagriculture.telemetry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * Log4j2 appender that feeds {@link NeroAgricultureTelemetry}. Minecraft routes essentially every failure
 * through log4j — handled errors, event-bus listener exceptions, and the crash report itself — so
 * listening on the root logger catches NeroAgriculture failures without mixins. Filtering
 * (NeroAgriculture-only), de-dup, rate-limiting and PII scrubbing all happen in
 * {@link NeroAgricultureTelemetry}; this only selects candidate log events.
 */
final class SentryLogAppender extends AbstractAppender {

    SentryLogAppender() {
        super("NeroAgricultureSentry", null, null, false, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        if (!NeroAgricultureTelemetry.isActive()) {
            return;
        }
        Level level = event.getLevel();
        if (!level.isMoreSpecificThan(Level.ERROR)) {
            return;
        }
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            if (NeroAgricultureTelemetry.touchesNeroAgriculture(thrown)) {
                NeroAgricultureTelemetry.capture(thrown);
            }
        } else if (level == Level.FATAL) {
            String message = event.getMessage() == null ? null : event.getMessage().getFormattedMessage();
            if (message != null && message.contains("za.co.neroland.neroagriculture")) {
                NeroAgricultureTelemetry.captureMessage(message);
            }
        }
    }
}
