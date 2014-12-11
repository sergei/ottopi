/*
 * Poller.h
 */

#ifndef OTTOPI_POLLER_H_
#define OTTOPI_POLLER_H_

#include <poll.h>

#include "IStream.h"

const int MAX_STREAMS_TO_POLL = 10;
class Poller{
public:
	Poller();
	void startPolling();
	void addStream( IStream  &stream);
	static void stopPolling(){s_keepRunning = false;};
private:
	int m_iStreamCount;
	int m_iTimeoutMs;
	IStream *streams[MAX_STREAMS_TO_POLL];
	struct pollfd fds[MAX_STREAMS_TO_POLL];
private:
	static bool s_keepRunning;

};




#endif /* OTTOPI_POLLER_H_ */
