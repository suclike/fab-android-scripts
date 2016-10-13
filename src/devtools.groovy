#!/usr/bin/env groovy
@Grab('joda-time:joda-time:2.9.4')
import org.joda.time.DateTime
import org.joda.time.format.*

/**
 * Created by dhelleberg on 24/09/14.
 * Improve command line parsing
 */

gfx_command_map = ['on': 'visual_bars', 'off': 'false', 'lines': 'visual_lines']
layout_command_map = ['on': 'true', 'off': 'false']
overdraw_command_map = ['on': 'show', 'off': 'false', 'deut': 'show_deuteranomaly']
overdraw_command_map_preKitKat = ['on': 'true', 'off': 'false']
show_updates_map = ['on': '0', 'off': '1']
date_single_option_possibilites = ['reset']
date_format_supported = ['d', 'h', 'm', 's']
date_opration_supported = ['+', '-']

command_map = ['gfx'     : gfx_command_map,
               'layout'  : layout_command_map,
               'overdraw': overdraw_command_map,
               'updates' : show_updates_map]

deviceIds = []

verbose = false
serialNumber = 0

FLAG_TARGET_DEVICE_USB = 1
FLAG_TARGET_DEVICE_EMULATOR = 2
FLAG_TARGET_DEVICE_BY_SERIAL = 3
FLAG_TARGET_DEVICE_ALL = 4
targetDevice = FLAG_TARGET_DEVICE_ALL

//get adb exec
adbExec = getAdbPath()
checkConnectedDevices()

def cli = new CliBuilder(usage: 'devtools.groovy command option')
cli.with {
    v longOpt: 'verbose', 'prints additional output'
    d longOpt: 'serialNumber', 'Direct the adb command to the only attached USB device.'
    e longOpt: 'serialNumber', 'Direct the adb command to the only running emulator instance.'
    s longOpt: 'serialNumber', 'specify emulator/device instance, referred to by its adb-assigned serial number.'
}
def opts = cli.parse(args)

if (!opts) {
    printDevtoolsOptionsUsageHelp("Not provided correct option")
}

if (opts.v) {
    verbose = true
}

if (opts.d && opts.e && opts.s || opts.d && opts.e || opts.s && opts.d || opts.s && opts.e) {
    printDevtoolsOptionsUsageHelp("You should specify only 1 target.")
}

if (opts.d) {
    targetDevice = FLAG_TARGET_DEVICE_USB
}

if (opts.e) {
    targetDevice = FLAG_TARGET_DEVICE_EMULATOR
}

if (opts.s) {
    targetDevice = FLAG_TARGET_DEVICE_BY_SERIAL
    serialNumber = opts.arguments().get(0)
    if (!isValidDeviceId(serialNumber)) {
        printDevtoolsOptionsUsageHelp("Not valid serial number " + serialNumber)
    }
    if (verbose) {
        println("Serial Number: " + serialNumber)
    }
}

private boolean isValidDeviceId(def serialNumber) {
    if (serialNumber in deviceIds)
        return true
}

//get args
String command
options = new String[1]
int commandPosition

if (serialNumber == 0) {
    commandPosition = 1
    command = opts.arguments().get(0)
    options = new String[opts.arguments().size() - commandPosition]

} else {
    commandPosition = 2
    command = opts.arguments().get(1)
    options = new String[opts.arguments().size() - commandPosition]
}

switch (command) {
    case "gfx":
    case "layout":
    case "overdraw":
    case "updates":
        if (opts.arguments().size() != 2) {
            printHelpForSpecificCommand(command, false, null)
        }
        options[0] = opts.arguments().get(1)
        break

    case "date":
        for (int i = 0; i < options.length; i++) {
            options[i] = opts.arguments().get(i + commandPosition)
        }

        if (options.size() == 0)
            printHelpForSpecificCommand(command, false, null)

        if (options.size() == 1) {
            if (!isAValidDateSingleOption(options[0]) && !isAValidDateOption(options[0])) {
                printHelpForSpecificCommand(command, false, null)
            }
        }
}

def adbCmd = ""
switch (command) {
    case "gfx":
        adbCmd = "shell setprop debug.hwui.profile " + gfx_command_map[options[0]]
        executeADBCommand(adbCmd)
        break

    case "layout":
        adbCmd = "shell setprop debug.layout " + layout_command_map[options[0]]
        executeADBCommand(adbCmd)
        break

    case "overdraw":
        //tricky, properties have changed over time
        adbCmd = "shell setprop debug.hwui.overdraw " + overdraw_command_map[options[0]]
        executeADBCommand(adbCmd)
        adbCmd = "shell setprop debug.hwui.show_overdraw " + overdraw_command_map_preKitKat[options[0]]
        executeADBCommand(adbCmd)
        break

    case "updates":
        adbCmd = "shell service call SurfaceFlinger 1002 android.ui.ISurfaceComposer" + show_updates_map[options[0]]
        executeADBCommand(adbCmd)
        break

    case "date":
        adbCmd = buildDateCommand()
        executeADBCommand(adbCmd)
        break

    default:
        printHelpForSpecificCommand(command, false, null)

}

kickSystemService()
System.exit(0)

/* CMD METHODS */

String fixFormat(String val) {
    if (val.length() == 1)
        return "0" + val
    return val
}

String buildResetCommand() {
    Calendar calendar = Calendar.getInstance()

    String monthOfYear = fixFormat(String.valueOf((calendar.get(Calendar.MONTH) + 1)))
    String dayOfMonth = fixFormat(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)))
    String minutesOfHour = fixFormat(String.valueOf(calendar.get(Calendar.MINUTE)))
    String secondsOfMinutes = fixFormat(String.valueOf(calendar.get(Calendar.SECOND)))

    String adbCommand
    if (isNOrLater()) {
        adbCommand = "shell date " +
                monthOfYear +
                dayOfMonth +
                calendar.get(Calendar.HOUR_OF_DAY) +
                minutesOfHour +
                calendar.get(Calendar.YEAR) +
                "." +
                secondsOfMinutes

    } else {
        adbCommand = "shell date -s " +
                calendar.get(Calendar.YEAR) +
                monthOfYear +
                dayOfMonth +
                "." +
                calendar.get(Calendar.HOUR_OF_DAY) +
                minutesOfHour +
                secondsOfMinutes
    }

    println("Setting device date and time to now : " + getDeviceDate())
    return adbCommand
}

String buildDateCommand() {
    if (options.size() == 1 && isAValidDateSingleOption(options[0])) {
        return buildResetCommand()

    } else {
        DateTime deviceDateTime = getDeviceDateTime()
        String commandResultMessage = "Date changed from " + getDeviceDate() + " to "

        options.each { option ->
            if (option.length() > 4 || option.length() < 3) {
                printHelpForSpecificCommand("date", true, option)
            }

            def operation = option.take(1)
            def rangeType = option.reverse().take(1).reverse()

            if (!(operation in date_opration_supported)) {
                printHelpForSpecificCommand("date", true, option)
            }

            if (!(rangeType in date_format_supported)) {
                printHelpForSpecificCommand("date", true, option)
            }

            def range = option.substring(1, option.length() - 1)
            if (!range.isNumber()) {
                printHelpForSpecificCommand("date", true, option)
            }

            deviceDateTime = applyRangeToDate(deviceDateTime, operation, Integer.valueOf(range), rangeType)
        }

        String formattedDate = formatDateForAdbCommand(deviceDateTime)
        String adbCommand

        if (isNOrLater()) {
            adbCommand = "shell date " + formattedDate
        } else {
            adbCommand = "shell date -s " + formattedDate
        }

        commandResultMessage += formattedDate
        println(commandResultMessage)

        return adbCommand
    }
}

private boolean isAValidDateOption(String option) {
    def operation = option.take(1)
    def rangeType = option.reverse().take(1).reverse()

    if (!(operation in date_opration_supported)) {
        return false
    }

    if (!(rangeType in date_format_supported)) {
        return false
    }

    def range = option.substring(1, option.length() - 1)
    if (!range.isNumber()) {
        return false
    }

    return true
}

private boolean isAValidDateSingleOption(String option) {
    if (option in date_single_option_possibilites)
        return true

    return false
}

private DateTime applyRangeToDate(DateTime dateTime, def operation, int range, def rangeType) {
    if (operation.equals("+")) {
        return addRange(dateTime, rangeType, range)
    } else {
        return minusRange(dateTime, rangeType, range)
    }
}

private DateTime addRange(DateTime fromDate, def rangeType, int range) {
    switch (rangeType) {
        case "d":
            return fromDate.plusDays(range)

        case "h":
            return fromDate.plusHours(range)

        case "m":
            return fromDate.plusMinutes(range)

        case "s":
            return fromDate.plusSeconds(range)
    }
}

private DateTime minusRange(DateTime fromDate, def rangeType, int range) {
    switch (rangeType) {
        case "d":
            return fromDate.minusDays(range)
            break

        case "h":
            return fromDate.minusHours(range)
            break

        case "m":
            return fromDate.minusMinutes(range)
            break

        case "s":
            return fromDate.minusSeconds(range)
    }
}

private String getDeviceDate() {
    if (isNOrLater()) {
        adbCmd = "shell date +%Y%m%d.%H%M%S"
    } else {
        adbCmd = "shell date +%Y%m%d.%H%M%S"
    }
    return executeADBCommand(adbCmd)
}

private DateTime getDeviceDateTime() {
    deviceDate = getDeviceDate()
    if (verbose)
        println("Device current Date: " + deviceDate)

    int year = Integer.valueOf(deviceDate.take(4))
    int month = Integer.valueOf(deviceDate[4..5])
    int day = Integer.valueOf(deviceDate[6..7])
    int hours = Integer.valueOf(deviceDate[9..10])
    int minutes = Integer.valueOf(deviceDate[11..12])
    int seconds = Integer.valueOf(deviceDate[13..14])

    return new DateTime(year, month, day, hours, minutes, seconds)
}

private String formatDateForAdbCommand(DateTime dateTime) {
    def dateFormat
    if (isNOrLater()) {
        dateFormat = "MMddHHmmYYYY.ss"
    } else {
        dateFormat = "YYYYMMd.HHmmss"
    }

    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(dateFormat)
    return dateTime.toString(dateTimeFormatter)
}

/* print help */

void printDevtoolsUsageHelp(String additionalMessage) {
    println()
    if (additionalMessage) {
        println("Error $additionalMessage")
        println()
    }

    println("Usage: devtools.groovy [-v] command option")
    print("Command: ")
    command_map.each { command, options ->
        print("\n  $command -> ")
        options.each {
            option, internal_cmd -> print("$option ")
        }
    }
    println()
    System.exit(-1)
}

void printDevtoolsOptionsUsageHelp(String additionalMessage) {
    println()
    println(additionalMessage)

    println("Usage: devtools.groovy options")
    println()
    println("Specify target device:")
    println("-d                     Direct an adb command to the only attached USB device.")
    println("                       Returns an error if more than one USB device is attached.")
    println()
    println("-e                     Direct an adb command to the only running emulator instance.")
    println("                       Returns an error if more than one emulator instance is running.")
    println()
    println("-s <serialNumber>      Direct an adb command a specific emulator/device instance, referred to by its adb-assigned serial number.")
    println("                       Directing Commands to a Specific Emulator/Device Instance.")
    println()

    println("Run devtools --help for more details on how to use devtools.")
    println()

    System.exit(-1)
}

void printHelpForSpecificCommand(String command, boolean isOptionError, String option) {
    println()
    switch (command) {
        case "gfx":
        case "layout":
        case "overdraw":
        case "updates":
            println("You need to provide two arguments: command and option")
            break

        case "date":
            if (isOptionError) {
                println("Not valid command option: " + option + " for: " + command)
            } else {
                println("Not valid command: " + command)
            }
            println()
            println("Usage: devtools.groovy [-v] date options")
            println()
            println("+xd     Add [x] days to the device time.")
            println("-xd     Subtract [x] days from the device time.")
            println()
            println("+xh     Add [x] hours to the device time.")
            println("-xh     Subtract [x] hours from the device time.")
            println()
            println("+xm     Add [x] minutes to the device time.")
            println("-xm     Subtract [x] minutes from the device time.")
            println()
            println("+xs     Add [x] seconds to the device time.")
            println("-xs     Subtract [x] seconds from the device time.")
            println()
            break

        case "devtools":
            printDevtoolsUsageHelp(option)
            break

        default:
            println("Could not find the command $command you provided")
            printDevtoolsUsageHelp(null)
    }
    System.exit(-1)
}

/* ADB UTILS */

String getAdbPath() {
    def adbExec = "adb"
    if (isWindows())
        adbExec = adbExec + ".exe"
    try {
        def command = "$adbExec"    //try it plain from the path
        command.execute()
        if (verbose)
            println("using adb in " + adbExec)
        return adbExec
    }
    catch (IOException e) {
        //next try with Android Home
        def env = System.getenv("ANDROID_HOME")
        if (verbose)
            println("adb not in path trying Android home")
        if (env != null && env.length() > 0) {
            //try it here
            try {
                adbExec = env + File.separator + "platform-tools" + File.separator + "adb"
                if (isWindows())
                    adbExec = adbExec + ".exe"

                def command = "$adbExec"// is actually a string
                command.execute()
                if (verbose)
                    println("using adb in " + adbExec)

                return adbExec
            }
            catch (IOException ex) {
                println("Could not find $adbExec in path and no ANDROID_HOME is set :(")
                System.exit(-1)
            }
        }
        println("Could not find $adbExec in path and no ANDROID_HOME is set :(")
        System.exit(-1)
    }
}

void checkConnectedDevices() {
    def adbDevicesCmd = "$adbExec devices"
    def proc = adbDevicesCmd.execute()
    proc.waitFor()

    def foundDevice = false

    proc.in.text.eachLine {
            //start at line 1 and check for a connected device
        line, number ->
            if (number > 0 && line.contains("device")) {
                foundDevice = true
                //grep out device ids
                def values = line.split('\\t')
                if (verbose)
                    println("found id: " + values[0])
                deviceIds.add(values[0])
            }
    }

    if (!foundDevice) {
        println("No usb devices")
        System.exit(-1)
    }
}

String executeADBCommand(String adbCommand) {
    if (deviceIds.size == 0) {
        println("no devices connected")
        System.exit(-1)
    }

    def proc

    if (targetDevice == FLAG_TARGET_DEVICE_ALL) {
        deviceIds.each { deviceId ->
            def adbConnect = "$adbExec -s $deviceId $adbCommand"
            if (verbose)
                println("Executing $adbConnect")
            proc = adbConnect.execute()
            proc.waitFor()
        }
        return proc.text
    }

    def adbConnect = "$adbExec "
    switch (targetDevice) {
        case FLAG_TARGET_DEVICE_USB:
            adbConnect += "-d $adbCommand"
            break

        case FLAG_TARGET_DEVICE_EMULATOR:
            adbConnect += "-e $adbCommand"
            break

        case FLAG_TARGET_DEVICE_BY_SERIAL:
            adbConnect += "-s $serialNumber $adbCommand"
            break
    }

    if (verbose) {
        println("Executing $adbConnect")
    }

    proc = adbConnect.execute()
    proc.waitFor()
    return proc.text
}

private boolean isNOrLater() {
    GString apiLevelCmd = "$adbExec shell getprop ro.build.version.sdk";
    proc = apiLevelCmd.execute()
    proc.waitFor()

    Integer apiLevel = 0
    proc.in.text.eachLine { apiLevel = it.toInteger() }
    if (apiLevel == 0) {
        println("Could not retrieve API Level")
        System.exit(-1)
    } else {
        if (apiLevel >= 24) {
            return true
        } else {
            return false
        }
    }
}

void kickSystemService() {
    def proc
    int SYSPROPS_TRANSACTION = 1599295570 // ('_'<<24)|('S'<<16)|('P'<<8)|'R'

    def pingService = "shell service call activity $SYSPROPS_TRANSACTION"
    executeADBCommand(pingService)
}

boolean isWindows() {
    return (System.properties['os.name'].toLowerCase().contains('windows'))
}