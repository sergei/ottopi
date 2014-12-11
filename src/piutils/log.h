/*
 * log.h
 */

#ifndef LOG_H_
#define LOG_H_

#include <syslog.h>

#ifdef	__cplusplus
extern "C" {
#endif

void useSyslog(const char *priority);
void ottolog(int priority, const char *format, ...);


#ifdef	__cplusplus
}
#endif

#endif /* LOG_H_ */
