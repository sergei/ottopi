/*
 * NmeaListener.h
 *
 *  Created on: Dec 10, 2014
 *      Author: spodshivalov
 */

#ifndef OTTOPI_NMEALISTENER_H_
#define OTTOPI_NMEALISTENER_H_

#include "minmea.h"

class NmeaListener {
public:
	virtual void onTimerTick() = 0;
	virtual ~NmeaListener(){};
};

#endif /* OTTOPI_NMEALISTENER_H_ */
