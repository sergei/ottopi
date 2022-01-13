def rgba(r, g, b, a):
    return '#{r:02X}{g:02X}{b:02X}{a:02X}'.format(r=r, g=g, b=b, a=a)


def rgb(r, g, b):
    return '#{r:02X}{g:02X}{b:02X}{a:02X}'.format(r=r, g=g, b=b, a=255)


POLAR_DOT_OUTLINE = rgb(0, 0, 0)
POLAR_GRID_COLOR = rgba(255, 255, 255, 127)
POLAR_TARGET_COLOR = rgb(255, 255, 255)
TIMER_FONT_COLOR = rgba(255, 0, 0, 200)
SEMI_TRANSPARENT_BOX_COLOR_2 = '#32323280'
FULLY_TRANSPARENT_COLOR = '#00000000'
SEMI_TRANSPARENT_FONT_COLOR = '#FFFFFF80'
NON_TRANSPARENT_WHITE_FONT_COLOR = '#FFFFFFFF'
