from bt_remote import BtRemote
from pynput.keyboard import Key, Listener


def sim_bt_remote(client):
    key_map = {
        Key.down: BtRemote.MINUS_BUTTON,
        Key.up: BtRemote.PLUS_BUTTON,
        Key.left: BtRemote.PREV_BUTTON,
        Key.right: BtRemote.NEXT_BUTTON,
        Key.enter: BtRemote.PLAY_BUTTON,
        Key.delete: BtRemote.VENDOR_BUTTON,
    }

    def on_press(key):
        print('{0} pressed'.format(key))
        if key in key_map:
            client.on_remote_key(key_map[key])

    def on_release(key):
        print('{0} release'.format(key))
        if key == Key.esc:
            # Stop listener
            return False

    # Collect events until released
    print('Use your keyboard arrow keys to simulate BT remote, ESC to exit')
    with Listener(
            on_press=on_press,
            on_release=on_release) as listener:
        listener.join()
