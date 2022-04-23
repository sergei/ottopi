package com.santacruzinstruments.ottopi.navengine.geo;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class is supposed to be  thread-safe immutable, since it has final methods only
 * */
public class UtcTime extends Quantity{
	private final static String INVALID_VALUE = "--:--:--";
	public static final UtcTime INVALID = new UtcTime();
	private final static GregorianCalendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

	private final Date mdate;

	public UtcTime() {
		super(false);
		mdate = null;
	}

	public UtcTime(Date date) {
		super(true);
		mdate = date;
	}
	public static int diffSec(UtcTime from, UtcTime to)
	{
		return (int) ((to.mdate.getTime() - from.mdate.getTime() + 500) / 1000);
	}
	@Override
	public String toString() {
		if ( mIsValid )
		{
			CALENDAR.setTime(mdate);

			return String.format("%02d:%02d:%02d",
					CALENDAR.get(Calendar.HOUR_OF_DAY),
					CALENDAR.get(Calendar.MINUTE),
					CALENDAR.get(Calendar.SECOND));
		}
		else
		{
			return INVALID_VALUE;
		}
	}

    public long toMiliSec() {
        return mdate.getTime();
    }

	public Date getDate() {
		return mdate;
	}


}
