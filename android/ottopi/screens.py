import json
import os
import sys

PIC_FORMAT = """
<div>
 <p style="float: left;"><a href="{img}"><img src="{img}" width="360"></a></p>
 <p>{body}</p>
</div>
<div style="clear: left;">
</div>
"""


def json_to_md(json_file):
    src_dir = os.path.split(json_file)[0]
    md_file = src_dir + os.sep + os.path.splitext(os.path.split(json_file)[1])[0] + '.md'
    with open(json_file, 'r') as f, open(md_file, 'w') as md:
        s = f.read()
        json_s = '[' + s[:-2] + ']'
        screen_shots = json.loads(json_s)
        for screen_shot in screen_shots:
            md.write('### ' + screen_shot['header'])
            md.write('\n\n')
            md.write(PIC_FORMAT.format(img=src_dir + os.sep + screen_shot['image'], body=screen_shot.get('body', '')))
            md.write('\n\n')
        print('{} created'.format(md_file))


if __name__ == '__main__':
    if len(sys.argv) == 2:
        json_to_md(sys.argv[1])
    else:
        print('Usage {} screens.json'.format(sys.argv[0]))
