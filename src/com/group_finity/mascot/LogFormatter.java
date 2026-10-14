package com.group_finity.mascot;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Klasse zur Formatierung von Protokolleinträgen.
 * @author Yuki
 */
public class LogFormatter extends SimpleFormatter {

	private final Date dat = new Date();
	private final static String format = "{0,date} {0,time}";
	private MessageFormat formatter;

	private Object args[] = new Object[1];

	private String lineSeparator = System.getProperty("line.separator");

	/**
	 * Formatiert den angegebenen LogRecord.
	 * @param record Zu formatierender Protokolleintrag.
	 * @return Formatierter Protokolleintrag
	 */
	@Override
	public synchronized String format(final LogRecord record) {
		final StringBuffer sb = new StringBuffer();

		// Speicherzuweisungen hier minimieren.
		this.dat.setTime(record.getMillis());
		this.args[0] = this.dat;
		final StringBuffer text = new StringBuffer();
		if (this.formatter == null) {
			this.formatter = new MessageFormat(format);
		}
		this.formatter.format(this.args, text, null);
		sb.append(text);
		sb.append(" ");

		sb.append(record.getLevel().getLocalizedName());
		sb.append(": ");


		if (record.getSourceClassName() != null) {
			sb.append(record.getSourceClassName());
		} else {
			sb.append(record.getLoggerName());
		}
		if (record.getSourceMethodName() != null) {
			sb.append(" ");
			sb.append(record.getSourceMethodName());
		}
		sb.append(" ");

		final String message = formatMessage(record);
		sb.append(message);
		sb.append(this.lineSeparator);
		if (record.getThrown() != null) {
			try {
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (final Exception ex) {
			}
		}
		return sb.toString();
	}
}
