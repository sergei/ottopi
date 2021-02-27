import dbus


def extract_objects(object_list):
    out_list = ""
    for obj in object_list:
        val = str(obj)
        out_list = out_list + val[val.rfind("/") + 1:] + " "
    return out_list


def extract_uuids(uuid_list):
    out_list = ""
    for uuid in uuid_list:
        if uuid.endswith("-0000-1000-8000-00805f9b34fb"):
            if uuid.startswith("0000"):
                val = "0x" + uuid[4:8]
            else:
                val = "0x" + uuid[0:8]
        else:
            val = str(uuid)
        out_list = out_list + val + " "
    return out_list


def list_devices():
    bus = dbus.SystemBus()

    manager = dbus.Interface(bus.get_object("org.bluez", "/"),
                             "org.freedesktop.DBus.ObjectManager")
    objects = manager.GetManagedObjects()
    all_devices = [str(path) for path, interfaces in objects.items() if
                   "org.bluez.Device1" in interfaces.keys()]

    for path, interfaces in objects.items():
        if "org.bluez.Adapter1" not in interfaces.keys():
            continue

        print("[ " + path + " ]")

        properties = interfaces["org.bluez.Adapter1"]
        for key in properties.keys():
            value = properties[key]
            if key == "UUIDs":
                uuid_list = extract_uuids(value)
                print("    %s = %s" % (key, uuid_list))
            else:
                print("    %s = %s" % (key, value))

        device_list = [d for d in all_devices if d.startswith(path + "/")]

        for dev_path in device_list:
            print("    [ " + dev_path + " ]")

            dev = objects[dev_path]
            properties = dev["org.bluez.Device1"]

            for key in properties.keys():
                value = properties[key]
                if key == "UUIDs":
                    uuid_list = extract_uuids(value)
                    print("        %s = %s" % (key, uuid_list))
                elif key == "Class":
                    print("        %s = 0x%06x" % (key, value))
                elif key == "Vendor":
                    print("        %s = 0x%04x" % (key, value))
                elif key == "Product":
                    print("        %s = 0x%04x" % (key, value))
                elif key == "Version":
                    print("        %s = 0x%04x" % (key, value))
                else:
                    print("        %s = %s" % (key, value))

        print("")


if __name__ == '__main__':
    list_devices()
