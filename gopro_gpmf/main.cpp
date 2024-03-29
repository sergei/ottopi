#pragma clang diagnostic push
#pragma ide diagnostic ignored "hicpp-signed-bitwise"
#include <iostream>

#include <gpmf-parser/GPMF_parser.h>
#include <gpmf-parser/GPMF_utils.h>
#include "GPMF_mp4reader.h"


void printHelp(char* name)
{
    printf("usage: %s <file_with_GPMF> \n", name);
    printf("       ver 1.0\n");
}


static int dd(double deg){
    return int(deg);

}

static double mm(double deg){
    return (deg - dd(deg)) * 60.;
}

static unsigned char cc(const char *msg){
    unsigned char cc = 0;
    for(int i =0; msg[i] != '\0'; i++)
        cc ^= msg[i];
    return cc;
}

int main(int argc, char* argv[]) {
    size_t mp4handle = OpenMP4Source(argv[1], MOV_GPMF_TRAK_TYPE, MOV_GPMF_TRAK_SUBTYPE, 0);

    bool nmeaOnly = false;
    if (argc == 3){
        nmeaOnly = true;
    }

    if (argc < 2)
    {
        printHelp(argv[0]);
        return -1;
    }

    if (mp4handle == 0)
    {
        printf("error: %s is an invalid MP4/MOV or it has no GPMF data\n\n", argv[1]);

        printHelp(argv[0]);
        return -1;
    }

    double  metadatalength = GetDuration(mp4handle);

    if (metadatalength > 0.0) {
        uint32_t payloads = GetNumberPayloads(mp4handle);

        printf("idx,t,fix_valid,utc,lat,lon,alt,sog_ms,speed3d_ms\n");

        for (uint32_t index = 0; index < payloads; index++) {
            double in = 0.0, out = 0.0; //times
            uint32_t payloadsize = GetPayloadSize(mp4handle, index);
            size_t payloadres=0;
            payloadres = GetPayloadResource(mp4handle, payloadres, payloadsize);
            uint32_t *payload = GetPayload(mp4handle, payloadres, index);
            if (payload == nullptr)
                return -1;

            GPMF_ERR ret = GetPayloadTime(mp4handle, index, &in, &out);
            if (ret != GPMF_OK)
                return -1;

            GPMF_stream gs_stream;
            ret = GPMF_Init(&gs_stream, payload, payloadsize);
            if (ret != GPMF_OK)
                return -1;

            GPMF_ResetState(&gs_stream);
            GPMF_ERR nextret;

            double lat = 0;
            double lon = 0;
            double alt = 0;
            double sog_ms = 0;
            double speed3d_ms = 0;
            bool   valid = false;
            char   utc[256] = "";
            do {
                uint32_t key = GPMF_Key(&gs_stream);

                switch (key) {
                    case STR2FOURCC("GPS5"):{
                        auto *data = (int32_t *) GPMF_RawData(&gs_stream);
                        // Get only the first position
                        lat = (double)BYTESWAP32(data[0]) / 10000000.;
                        lon = (double)BYTESWAP32(data[1]) / 10000000.;
                        alt = (double)BYTESWAP32(data[2]) / 1000.;
                        sog_ms = (double)BYTESWAP32(data[3]) / 1000.;
                        speed3d_ms = (double)BYTESWAP32(data[4]) / 100.;
                    }
                        break;

                    case STR2FOURCC("GPSF"):{
                        auto *data = (uint32_t *) GPMF_RawData(&gs_stream);
                        valid = BYTESWAP32(data[0]) != 0;
                    }
                        break;

                    case STR2FOURCC("GPSU"): {
                        char *data = (char *) GPMF_RawData(&gs_stream);
                        sprintf(utc,"20%2.2s-%2.2s-%2.2sT%2.2s:%2.2s:%2.2s.%3.3s",
                                data, data + 2, data + 4, data + 6, data + 8, data + 10 , data+13);
                    }
                        break;
                    default:
                        break;
                }

                nextret = GPMF_Next(&gs_stream, (GPMF_LEVELS)(GPMF_RECURSE_LEVELS | GPMF_TOLERANT));

            } while (GPMF_OK == nextret); // Scan through all GPMF data
            GPMF_ResetState(&gs_stream);


            if( valid ) {
                if ( !nmeaOnly ){
                    printf("%d,%f,%s,", index, in,  "True" );
                    printf("%s,%.5f,%.5f,%.1f,%.1f,%.1f\n", utc, lat, lon, alt, sog_ms, speed3d_ms);
                }else {
                    char nmea[120];
                    sprintf(nmea, "GPRMC,%.2s%.2s%.2s,A,%02d%.5f,%c,%03d%.5f,%c,%.1f,,%.2s%.2s%.2s,,,A",
                            utc + 11, utc + 14, utc + 17,
                            dd(abs(lat)), mm(abs(lat)), lat > 0 ? 'N' : 'S',
                            dd(abs(lon)), mm(abs(lon)), lon > 0 ? 'E' : 'W',
                            sog_ms / 1852. * 3600,
                            utc + 8, utc + 5, utc + 2
                    );
                    printf("$%s*%02X\n", nmea, cc(nmea));
                }
            }else {
                if ( !nmeaOnly ) {
                    printf("%d,%f,%s,", index, in, "False");
                    printf("%s,,\n", utc);
                }else {
                    char nmea[120];
                    sprintf(nmea, "GPRMC,%.2s%.2s%.2s,,,,,,,,%.2s%.2s%.2s,,,A",
                            utc + 11, utc + 14, utc + 17,
                            utc + 8, utc + 5, utc + 2
                    );
                    printf("$%s*%02X\n", nmea, cc(nmea));
                }
            }
        }
    }

    return 0;
}

