/*
 * log.cpp
 */

#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <time.h>

#ifdef __MACH__
#include <mach/clock.h>
#include <mach/mach.h>
#endif


#include "defines.h"
#include "log.h"

static int __useSyslog = 0;

typedef struct _code {
	const char	*c_name;
	int	c_val;
} CODE;

static CODE prioritynames[] =
  {
    { "alert", LOG_ALERT },
    { "crit", LOG_CRIT },
    { "debug", LOG_DEBUG },
    { "emerg", LOG_EMERG },
    { "err", LOG_ERR },
    { "info", LOG_INFO },
    { "notice", LOG_NOTICE },
    { "warning", LOG_WARNING }
  };

void useSyslog(const char *priority)
{
	int i;
	__useSyslog = 1;

	setlogmask(LOG_UPTO(LOG_INFO));

	for ( i = 0; i < _DIM(prioritynames); i++ )
	{
		if ( !strcmp(priority, prioritynames[i].c_name ))
		{
			setlogmask(LOG_UPTO(prioritynames[i].c_val));
			break;
		}
	}

}

void ottolog(int priority, const char *format, ...)
{
	 va_list ap;
	 va_start(ap, format);

	 if ( __useSyslog )
	 {
		 vsyslog(priority, format, ap);
	 }
	 else
	 {
		 struct timespec ts;


#ifdef __MACH__ // OS X does not have clock_gettime, use clock_get_time
		clock_serv_t cclock;
		mach_timespec_t mts;
		host_get_clock_service(mach_host_self(), CALENDAR_CLOCK, &cclock);
		clock_get_time(cclock, &mts);
		mach_port_deallocate(mach_task_self(), cclock);
		ts.tv_sec = mts.tv_sec;
		ts.tv_nsec = mts.tv_nsec;
#else
		 clock_gettime(CLOCK_MONOTONIC, &ts);
#endif

		 int  sec = ts.tv_sec  % (12*3600);
		 int  msec = ts.tv_nsec / 1000 / 1000;
		 printf("%05d.%03d ", sec, msec ) ;
		 vprintf(format, ap);
	 }

	 va_end(ap);

}

