/*
 * sendnmea.h
 *
 *  Created on: Dec 3, 2014
 *      Author: spodshivalov
 */

#ifndef SENDNMEA_H_
#define SENDNMEA_H_

void openNmea  ( const char *fname );
void sendNmea  ( const char *text  );
void closeNmea ( void );

#endif /* SENDNMEA_H_ */
