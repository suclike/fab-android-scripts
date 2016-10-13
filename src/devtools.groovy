#!/usr/bin/env groovy
import groovyjarjarantlr.collections.List
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

commands = ["gfx", "layout", "overdraw", "updates", "date"]

verbose = false
serialNumber = ""
targetDevice = ADBUtils.FLAG_TARGET_DEVICE_EMULATOR

def cli = new CliBuilder(usage: 'devtools.groovy command option')
cli.with {
    v longOpt: 'verbose', 'prints additional output'
    d longOpt: 'serialNumber', 'Direct the adb command to the only attached USB device.'
    e longOpt: 'serialNumber', 'Direct the adb command to the only running emulator instance.'
    s longOpt: 'serialNumber', 'specify emulator/device instance, referred to by its adb-assigned serial number.'
}
def opts = cli.parse(args)

if (!opts) {
    Log.printDevtoolsOptionsUsageHelp("Not provided correct option")
}

if (opts.v) {
    verbose = true
}

if (opts.d && opts.e && opts.s || opts.d && opts.e || opts.s && opts.d || opts.s && opts.e) {
    Log.printDevtoolsOptionsUsageHelp("You should specify only 1 target.")
}

if (opts.d) {
    targetDevice = ADBUtils.FLAG_TARGET_DEVICE_USB
}

if (opts.e) {
    targetDevice = ADBUtils.FLAG_TARGET_DEVICE_EMULATOR
}

if (opts.s) {
    targetDevice = ADBUtils.FLAG_TARGET_DEVICE_BY_SERIAL
    serialNumber = opts.arguments().get(0)
    if (!ADBUtils.isValidDeviceId(serialNumber)) {
        Log.printDevtoolsOptionsUsageHelp("Not valid serial number " + serialNumber)
    }
}

//get adb exec
ADBUtils adbUtils = new ADBUtils(targetDevice, verbose, serialNumber)

//get args
String command
options = new String[1]
int commandPosition

if (serialNumber == "") {
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
            Log.printHelpForSpecificCommand(command, true, null)
        }
        options[0] = opts.arguments().get(1)
        break

    case "date":
        for (int i = 0; i < options.length; i++) {
            options[i] = opts.arguments().get(i + commandPosition)
        }

        if (options.size() == 0)
            Log.printHelpForSpecificCommand(command, false, null)

        if (options.size() == 1) {
            if (!isAValidDateSingleOption(options[0]) && !isAValidDateOption(options[0])) {
                Log.printHelpForSpecificCommand(command, false, null)
            }
        }
}

Command adbCommand
switch (command) {
    case "gfx":
        adbCommand = new Command()
        adbCommand.execute("shell setprop debug.hwui.profile " + gfx_command_map[options[0]])
        break

    case "layout":
        adbCommand = new Command("shell setprop debug.layout " + layout_command_map[options[0]])
        adbCommand.execute(adbUtils.adbPath)
        break

    case "overdraw":
        //tricky, properties have changed over time
        adbCommand = new Command("shell setprop debug.hwui.overdraw " + overdraw_command_map[options[0]])
        adbCommand.execute(adbUtils.adbPath)
        adbCommand = new Command("shell setprop debug.hwui.show_overdraw " + overdraw_command_map_preKitKat[options[0]])
        adbCommand.execute(adbUtils.adbPath)
        break

    case "updates":
        adbCommand = new Command("shell service call SurfaceFlinger 1002 android.ui.ISurfaceComposer" + show_updates_map[options[0]])
        adbCommand.execute(adbUtils.adbPath)
        break

    case "date":
        DateCommand dateCommand = new DateCommand(buildDateCommand())
        dateCommand.execute(adbUtils.adbPath)
        break

    default:
        Log.printHelpForSpecificCommand(command, false, null)

}

kickSystemService()
System.exit(0)

/* CMD METHODS */

DateTime buildResetCommand() {
    return DateTime.now()
}

DateTime buildDateCommand() {
    if (options.size() == 1 && isAValidDateSingleOption(options[0])) {
        return buildResetCommand()

    } else {
        DateTime deviceDateTime = ADBUtils.getDeviceDateTime()

        options.each { option ->
            if (option.length() > 4 || option.length() < 3) {
                Log.printHelpForSpecificCommand("date", true, option)
            }

            def operation = option.take(1)
            def rangeType = option.reverse().take(1).reverse()

            if (!(operation in date_opration_supported)) {
                Log.printHelpForSpecificCommand("date", true, option)
            }

            if (!(rangeType in date_format_supported)) {
                Log.printHelpForSpecificCommand("date", true, option)
            }

            def range = option.substring(1, option.length() - 1)
            if (!range.isNumber()) {
                Log.printHelpForSpecificCommand("date", true, option)
            }

            deviceDateTime = applyRangeToDate(deviceDateTime, operation, Integer.valueOf(range), rangeType)
        }

        return deviceDateTime
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

void kickSystemService() {
    int SYSPROPS_TRANSACTION = 1599295570 // ('_'<<24)|('S'<<16)|('P'<<8)|'R'

    def pingService = "shell service call activity $SYSPROPS_TRANSACTION"
    Command adbCommand = new Command(pingService)
    adbCommand.execute(ADBUtils.adbPath)
}

interface ICommand {
    void execute(String adbPath)
}

public class Command implements ICommand {
    private String cmd
    private String resultMessage = "Error"

    public Command(String cmd) {
        this.cmd = cmd;
    }

    public void execute(String adbPath) {
        def adbCommand = adbPath + cmd
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        resultMessage = proc.text
    }

    public String getResultMessage() {
        return resultMessage
    }
}

public class DateCommand {

    private DateTime requestedDate
    private String resultMessage = "Error"

    DateCommand(DateTime requestedDate) {
        this.requestedDate = requestedDate
    }

    void execute(String adbPath) {
        def adbCommand = adbPath + buildCommand(adbPath)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        setResultMessage(proc.text)

        println(getResultMessage())
    }

    void executeImmediate(String adbPath, String dateCommand) {
        def adbCommand = adbPath + buildCommandImmediate(adbPath, dateCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        setResultMessage(proc.text)

        println(getResultMessage())

    }

    private String buildCommand(String adbPath) {
        if (isNOrLater(adbPath)) {
            return "shell date " + formatDate("MMddHHmmYYYY.ss")

        } else {
            return "shell date -s " + formatDate("YYYYMMd.HHmmss")
        }
    }

    private String buildCommandImmediate(String adbPath, String dateCommand) {
        if (isNOrLater(adbPath)) {
            return "shell date " + dateCommand

        } else {
            return "shell date -s " + dateCommand
        }
    }

    private String formatDate(String format) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(format)
        return requestedDate.toString(dateTimeFormatter)
    }

    void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage
    }

    public String getResultMessage() {
        return resultMessage
    }

    private boolean isNOrLater(String adbPath) {
        GString apiLevelCmd = "$adbPath shell getprop ro.build.version.sdk";
        def proc
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
}

public class ADBUtils {
    public static String[] commands = ["gfx", "layout", "overdraw", "updates", "date"]

    public static int FLAG_TARGET_DEVICE_USB = 1
    public static int FLAG_TARGET_DEVICE_EMULATOR = 2
    public static int FLAG_TARGET_DEVICE_BY_SERIAL = 3

    public static String adbPath
    private int targetDevice = FLAG_TARGET_DEVICE_EMULATOR
    private String serialNumber
    private boolean verbose = false

    public ADBUtils(int targetDevice, boolean verbose, String serialNumber) {
        this.targetDevice = targetDevice
        this.verbose = verbose
        this.serialNumber = serialNumber
        adbPath = getAdbPath()

    }

    public int getTargetDevice() {
        return targetDevice
    }

    public String getAdbExec() {
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

    public String getAdbPath() {
        def adbConnect = getAdbExec() + " "
        switch (targetDevice) {
            case FLAG_TARGET_DEVICE_USB:
                adbConnect += "-d "
                break

            case FLAG_TARGET_DEVICE_EMULATOR:
                adbConnect += "-e "
                break

            case FLAG_TARGET_DEVICE_BY_SERIAL:
                adbConnect += "-s $serialNumber "
                break
        }
        return adbConnect
    }

    private boolean isWindows() {
        return (System.properties['os.name'].toLowerCase().contains('windows'))
    }

    public static String getDeviceDate() {

        DateCommand dateCommand = new DateCommand(null)
        dateCommand.executeImmediate(adbPath, "+%Y%m%d.%H%M%S")
        return dateCommand.getResultMessage()

        //Command adbCommand = new Command("shell date +%Y%m%d.%H%M%S")
        //adbCommand.execute(adbPath)
        //return adbCommand.getResultMessage()
    }

    public static DateTime getDeviceDateTime() {
        String deviceDate = getDeviceDate()
        println(deviceDate)

        int year = Integer.valueOf(deviceDate.take(4))
        int month = Integer.valueOf(deviceDate[4..5])
        int day = Integer.valueOf(deviceDate[6..7])
        int hours = Integer.valueOf(deviceDate[9..10])
        int minutes = Integer.valueOf(deviceDate[11..12])
        int seconds = Integer.valueOf(deviceDate[13..14])

        return new DateTime(year, month, day, hours, minutes, seconds)
    }

    public static boolean isValidDeviceId(def serialNumber) {
        if (serialNumber in commands) {
            return false
        }
        //if (verbose) {
            println("Serial Number: " + serialNumber)
        //}
        return true
    }

}

public class Log {
    public static String[] commands = ["gfx", "layout", "overdraw", "updates", "date"]

    public static void printDevtoolsUsageHelp(String additionalMessage) {
        println()
        if (additionalMessage) {
            println("Error $additionalMessage")
            println()
        }

        println("Usage: devtools.groovy [-v] command option")
        printDevtoolsOptionsUsageHelp(null)

        for (int i = 0; i < commands.length; i++) {
            printHelpForSpecificCommand(commands[i], false, null)
        }

        System.exit(-1)
    }

    public static void printDevtoolsOptionsUsageHelp(String additionalMessage) {
        println()

        if (additionalMessage) {
            println(additionalMessage)
        }

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

    public static void printHelpForSpecificCommand(String command, boolean isOptionError, String option) {
        println()
        switch (command) {
            case "gfx":
                if (isOptionError) {
                    println("You need to provide two arguments: command and option")
                }
                println("Usage: devtools.groovy [-v] gfx option")
                println()
                println("on         visual_bars")
                println("off        false")
                println("lines      visual_lines")
                println()
                break

            case "layout":
                if (isOptionError) {
                    println("You need to provide two arguments: command and option")
                }
                println("Usage: devtools.groovy [-v] layout option")
                println()
                println("on         true")
                println("off        false")
                println()
                break

            case "overdraw":
                if (isOptionError) {
                    println("You need to provide two arguments: command and option")
                }
                println("Usage: devtools.groovy [-v] overdraw option")

                println()
                println("on         show")
                println("off        false")
                println("deut      show_deuteranomaly")
                println()
                println("On pre-kitkat")
                println("on         true")
                println("off        false")
                println()
                break

            case "updates":
                if (isOptionError) {
                    println("You need to provide two arguments: command and option")
                }
                println("Usage: devtools.groovy [-v] updates option")
                println()
                println("on         0")
                println("off        1")
                println()

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

}