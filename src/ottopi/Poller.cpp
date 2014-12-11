#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <assert.h>

#include "log.h"
#include "defines.h"

#include "Poller.h"

bool Poller::s_keepRunning = true;

static void handler(int sig) {
	if (sig == SIGINT) {
		ottolog(LOG_INFO, "Got kill signal\n");
		Poller::stopPolling();
	}
}

Poller::Poller()
:m_iStreamCount(0)
,m_iTimeoutMs(1000)
{
}

void Poller::addStream( IStream  &stream)
{

	assert( m_iStreamCount + 1 < _DIM(streams) );
}

void Poller::startPolling() {

	s_keepRunning = true;

	signal(SIGINT, handler);

	while (s_keepRunning) {
		// Prepare for select
		int fdLen = 0;
		for (int i = 0; i < m_iStreamCount; i++) {
			IStream *stream = streams[i];
			bool debug = stream->isDebugEnabled();
			if (debug)
				ottolog(LOG_DEBUG, "[%d]", i);
			int fd = stream->getFileDesciptor();

			if (debug)
				ottolog(LOG_DEBUG, "fd=%d(", fd);

			if (fd >= 0) {
				fds[fdLen].fd = fd;
				fds[fdLen].events = POLLIN;
				if (debug)
					ottolog(LOG_DEBUG, "r");
				if (stream->hasDataToSend()) {
					fds[fdLen].events |= POLLOUT;
					if (debug)
						ottolog(LOG_DEBUG, "w");
				}
				fds[fdLen].revents = 0;
				fdLen++;
			}
			if (debug)
				ottolog(LOG_DEBUG, ")");
			if (debug)
				ottolog(LOG_DEBUG, "\n");
		}

		if (fdLen > 0) {
			int rc = poll(fds, fdLen, m_iTimeoutMs );

			for (int i = 0; i < fdLen; i++) {
				for (int j = 0; j < _DIM(streams); j++) {
					if (fds[i].fd == streams[j]->getFileDesciptor()) {
						struct pollfd *pfd = &fds[i];
						IStream *stream = streams[j];
						bool debug = stream->isDebugEnabled();

						if (rc < 0) {
							stream->onError();
						} else if (rc == 0) {
							stream->onTimerTick();
						} else // Good return code
						{
							if (pfd->revents & (POLLERR | POLLHUP | POLLNVAL)) // Error occured
									{
								ottolog(LOG_DEBUG,
										"%d descriptor %d for %s revents = %04X \n",
										i, pfd->fd, stream->getName(),
										pfd->revents);
								stream->onError();
							} else // no error
							{
								char buffer[512];

								if (pfd->revents & POLLIN) {
									// Read data from file descriptor
									if (debug)
										ottolog(LOG_DEBUG, "start reading\n");
									int nread = read(pfd->fd, buffer,
											sizeof(buffer));
									if (debug)
										ottolog(LOG_DEBUG, "got %d bytes\n", nread);
									// Process received data
									stream->onDataReceived(buffer, nread);
								}
								if (pfd->revents & POLLOUT) {
									if (debug)
										ottolog(LOG_DEBUG, "Prepare to write\n");
									// Get data to write
									int bytesToWrite = stream->getDataToSend(
											buffer, sizeof(buffer));
									if (bytesToWrite > 0) {
										// Write data to file descriptor
										int nwritten = write(pfd->fd, buffer,
												bytesToWrite);
										ottolog(LOG_DEBUG,
												"Written %d bytes out of %d to %s\n",
												nwritten, bytesToWrite,
												stream->getName());
									}
								}
							}
							// Just to keep internal timers ticking
							stream->onTimerTick();
						}
					}
				}
			}
		} else {
			for (int i = 0; i < _DIM(streams); i++) {
				streams[i]->onTimerTick();
			}
			ottolog(LOG_DEBUG, "No files opened, waiting ...\n");
			sleep(1);
		}
	}

	ottolog(LOG_INFO, "Done polling\n");

}

