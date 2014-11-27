#include <pocketsphinx.h>

#define MODELDIR "/usr/local/share/pocketsphinx/model"

int
main(int argc, char *argv[])
{
        ps_decoder_t *ps;
        cmd_ln_t *config;

        config = cmd_ln_init(NULL, ps_args(), TRUE,
                             "-hmm", MODELDIR "/hmm/en_US/hub4wsj_sc_8k",
                             "-lm", MODELDIR "/lm/en/turtle.DMP",
                             "-dict", MODELDIR "/lm/en/turtle.dic",
                             NULL);
        if (config == NULL)
                return 1;

        ps = ps_init(config);
        if (ps == NULL)
                return 1;

       FILE *fh;

        fh = fopen("goforward.raw", "rb");
        if (fh == NULL) {
                perror("Failed to open goforward.raw");
                return 1;
        }

	int rv = ps_decode_raw(ps, fh, "goforward", -1);
        if (rv < 0)
                return 1;

        char const *hyp, *uttid;
        int32 score;

        hyp = ps_get_hyp(ps, &score, &uttid);
        if (hyp == NULL)
                return 1;
        printf("Recognized: %s\n", hyp);

	fclose(fh);
        ps_free(ps);

        return 0;
}
