/*
 * Controller.h
 *
 *  Created on: Dec 10, 2014
 *      Author: spodshivalov
 */

#ifndef OTTOPI_CONTROLLER_H_
#define OTTOPI_CONTROLLER_H_

#include "NmeaListener.h"

class Controller: public NmeaListener {
public:
	Controller();
	virtual void onTimerTick();
	virtual ~Controller();
};

#endif /* OTTOPI_CONTROLLER_H_ */
