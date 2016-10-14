# devtools

Do you use the developer options on your android device? Like "show overdraw" or "show layout bounds"?
Wouldn't it be handy to switch those tools on and off via command line?
via devtools you can switch:
* date (manipulating the date and time)
* gfx (profile gpu rendering)
* layout (show layout bounds)
* overdraw
* updates (show screen updates)
on and off via command line.
Works on emulators and devices. And, yes: it will control multiple devices at once.

## Options
    -d                      Direct an adb command to the only attached USB device.
                            Returns an error if more than one USB device is attached.

    -e                      Direct an adb command to the only running emulator instance.
                            Returns an error if more than one emulator instance is running.

    -s <serialNumber>       Direct an adb command a specific emulator/device instance, referred to by its adb-assigned serial number.
                            Directing Commands to a Specific Emulator/Device Instance.

    -v                      Print additional details about the results of your command.

## Commands

### date

Set the device date and time using +FORMAT or reset options

#### Usage

Example: `date +1h +30m -1d`

#### Options

`date reset` to set the device date and time to now

`date +[0-99]d` will add the specified number of *days* to the device date

`date -[0-99]d` will subtract the specified number of *days* from the device date

`date +[0-99]h` will add the specified number of *hours* to the device time

`date -[0-99]h` will subtract the specified number of *hours* from the device time

`date +[0-99]m` will add the specified number of *minutes* to the device time

`date -[0-99]m` will subtract the specified number of *minutes* from the device time

`date +[0-99]s` will add the specified number of *seconds* to the device time

`date -[0-99]s` will subtract the specified number of *seconds* from the device time

Check the video:

[![IMAGE ALT TEXT HERE](http://img.youtube.com/vi/GOJaOsJ0BJs/0.jpg)](http://www.youtube.com/watch?v=GOJaOsJ0BJs)

# adbwifi

Do you use adb-wifi connections? tired of figuring out the ip of your phone and typing it in?

This script tries to solve that.

## Usage

Connect your phone via USB (in best case with WiFi switched on) and run the script.
Wait until it tells you to disconnect and press enter.

## Sample output

    $> src/adbwifi.groovy
    WLAN IP 192.168.178.44
    mobile ip on WLAN: 192.168.178.44
    now disconnect your phone and press enter

    List of devices attached
    192.168.178.44:5555	unauthorized

    $> adb shell
    shell@hammerhead:/ $

# adbscreenrecord

Will execute screenrecord on your API Level 19+ device and will pull the file automatically after you finish the screenrecording.

    adbscreenrecord <filename.mp4, optional>

# Install

Just checkout or download the scrips to your system.
Make sure you have groovy installed, and adb-executeable in your PATH

On mac with brew just run:

    brew tap thefabulous/fab-android-scripts git@github.com:thefabulous/fab-android-scripts.git
    brew update
    brew install fab-android-scripts

You can use devtools with adb-wrapper https://github.com/zielmicha/adb-wrapper
    
    curl https://raw.githubusercontent.com/zielmicha/adb-wrapper/master/adb_wrapper.sh > /usr/local/bin/adb-wrapper.sh
    alias adb=/usr/local/bin/adb-wrapper.sh

# Troubleshooting

If you have issues installing fab-android-scripts via brew as described above try:

    brew install https://raw.githubusercontent.com/thefabulous/fab-android-scripts/master/fab-android-scripts.rb