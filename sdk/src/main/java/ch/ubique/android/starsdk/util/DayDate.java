package ch.ubique.android.starsdk.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;

public class DayDate {

	private static SimpleDateFormat dayDateFormat = new SimpleDateFormat("YYYY-MM-dd");

	static {
		dayDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private long timestampRepresentation;

	public DayDate() {
		this(System.currentTimeMillis());
	}

	public DayDate(String dayDate) throws ParseException {
		synchronized (dayDateFormat) {
			timestampRepresentation = convertToDay(dayDateFormat.parse(dayDate).getTime());
		}
	}

	public DayDate(long timestamp) {
		timestampRepresentation = convertToDay(timestamp);
	}

	public String formatAsString() {
		synchronized (dayDateFormat) {
			return dayDateFormat.format(timestampRepresentation);
		}
	}

	public DayDate getNextDay() {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(timestampRepresentation);
		calendar.add(Calendar.DATE, 1);
		return new DayDate(calendar.getTimeInMillis());
	}

	public long getStartOfDayTimestamp() {
		return timestampRepresentation;
	}

	public boolean isBefore(DayDate other) {
		return timestampRepresentation < other.timestampRepresentation;
	}

	public boolean isBeforeOrEquals(DayDate other) {
		return timestampRepresentation <= other.timestampRepresentation;
	}

	private long convertToDay(long time) {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(time);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DayDate dayDate = (DayDate) o;
		return timestampRepresentation == dayDate.timestampRepresentation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestampRepresentation);
	}

	public DayDate subtractDays(int days) {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(timestampRepresentation);
		cal.add(Calendar.DATE, -14);
		return new DayDate(cal.getTimeInMillis());
	}

}
