package com.santacruzinstruments.ottopi.ui;


import android.view.KeyEvent;
import java.util.HashMap;

@SuppressWarnings("Convert2Diamond")
public class KeyMapper {

    public enum CurrentScreen {
        OTHER,
        START_SCREEN,
        NAV_SCREEN,
    }

    public enum Action {
        NO_ACTION,
        SET_PIN,
        SET_RCB,
        STOP_RACE,
        START_BUTTON,
        NEXT_MARK,
        PREV_MARK,
    }

    /*
    Chubby Buttons keys
-   keyCode=KEYCODE_VOLUME_DOWN
<<  keyCode=KEYCODE_MEDIA_PREVIOUS
>|| keyCode=KEYCODE_MEDIA_PLAY_PAUSE
>>  keyCode=KEYCODE_MEDIA_NEXT
+   keyCode=KEYCODE_VOLUME_UP
     */

    private final static HashMap<CurrentScreen, HashMap<Integer, Action>> SCREENS_MAPS = new HashMap<CurrentScreen, HashMap<Integer, Action>>() {{
        put(CurrentScreen.START_SCREEN, new HashMap<Integer, Action>() {{
            put(KeyEvent.KEYCODE_VOLUME_DOWN, Action.SET_PIN);
            put(KeyEvent.KEYCODE_P, Action.SET_PIN);

            put(KeyEvent.KEYCODE_VOLUME_UP, Action.SET_RCB);
            put(KeyEvent.KEYCODE_R, Action.SET_RCB);

            put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, Action.START_BUTTON);
            put(KeyEvent.KEYCODE_S, Action.START_BUTTON);
        }});
        put(CurrentScreen.NAV_SCREEN, new HashMap<Integer, Action>() {{
            put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, Action.STOP_RACE);
            put(KeyEvent.KEYCODE_S, Action.STOP_RACE);

            put(KeyEvent.KEYCODE_MEDIA_NEXT, Action.NEXT_MARK);
            put(KeyEvent.KEYCODE_N, Action.NEXT_MARK);

            put(KeyEvent.KEYCODE_MEDIA_PREVIOUS, Action.PREV_MARK);
            put(KeyEvent.KEYCODE_P, Action.PREV_MARK);
        }});
    }};

    public static Action translateKeycode(CurrentScreen screen, int keyEvent) {
        HashMap<Integer, Action> keyToActionMap = SCREENS_MAPS.get(screen);
        if ( keyToActionMap != null){
            return keyToActionMap.getOrDefault(keyEvent, Action.NO_ACTION);
        }else {
            return Action.NO_ACTION;
        }
    }
}
