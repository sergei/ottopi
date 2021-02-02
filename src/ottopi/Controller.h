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
	virtual void onVwr( struct minmea_sentence_vwr &vwr );
	virtual void onLsn( struct minmea_sentence_lsn &lsn );
	virtual ~Controller();
};

#endif /* OTTOPI_CONTROLLER_H_ */
