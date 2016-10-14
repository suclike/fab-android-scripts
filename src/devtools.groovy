#!/usr/bin/env groovy
import org.codehaus.groovy.tools.shell.Command

/**
 * Created by dhelleberg on 24/09/14.
 * Improve command line parsing
 */
@Grab('joda-time:joda-time:2.9.4')
import org.joda.time.DateTime
import org.joda.time.format.*

verbose = false
serialNumber = ""
targetDevice = 0

def cli = new CliBuilder()
cli.with {
    v(longOpt: 'verbose', 'prints additional output')
    d(longOpt: 'device', 'Direct the adb command to the only attached USB device.')
    e(longOpt: 'emulator', 'Direct the adb command to the only running emulator instance.')
    s(longOpt: 'serialNumber', 'specify emulator/device instance, referred to by its adb-assigned serial number.', args: 1)
}
def opts = cli.parse(args)

if (!opts || opts.arguments().size() == 0) {
    Log.printUsage(false, "Not provided correct option")
}

if (opts.d && opts.e && opts.s || opts.d && opts.e || opts.s && opts.d || opts.s && opts.e) {
    Log.printUsage(false, "You should specify only 1 target.")
    Log.printUsage("You should specify only 1 target.")
}

if (opts.v) {
    verbose = true
}

//get args
String command
String[] options

if (opts.e) {
    targetDevice = ADBUtils.FLAG_TARGET_DEVICE_EMULATOR

    command = opts.arguments().get(0)
    options = opts.arguments().subList(1, opts.arguments().size())
} else if (opts.d) {
    targetDevice = ADBUtils.FLAG_TARGET_DEVICE_USB

    command = opts.arguments().get(0)
    options = opts.arguments().subList(1, opts.arguments().size())
} else if (opts.s) {
    targetDevice = ADBUtils.FLAG_TARGET_DEVICE_BY_SERIAL
    serialNumber = opts.s

    if (!Character.isDigit(serialNumber.charAt(1))) {
        Log.printUsage(false, "Not valid serial number: " + serialNumber)
    }
    command = opts.arguments().get(0)
    options = opts.arguments().subList(1, opts.arguments().size())
} else {
    command = opts.arguments().get(0)
    options = opts.arguments().subList(1, opts.arguments().size())
}

//get adb exec
ADBUtils adbUtils = new ADBUtils(targetDevice, verbose, serialNumber)

ICommand adbCommand
switch (command) {
    case "gfx":
        adbCommand = new GfxCommand()
        break
    case "layout":
        adbCommand = new LayoutCommand()
        break
    case "overdraw":
        adbCommand = new OverdrawCommand()
        break
    case "updates":
        adbCommand = new UpdatesCommand()
        break
    case "date":
        adbCommand = new DateCommand()
        break
    default:
        Log.printUsage(true, "Could not find the command $command you provided")
}

if (adbCommand != null && adbCommand.check(options)) {
    adbCommand.execute(options, ADBUtils.adbPath)
}

ServiceCallCommand serviceCallCommand = new ServiceCallCommand()
serviceCallCommand.execute(null, ADBUtils.adbPath)
System.exit(0)

interface ICommand {
    void execute(String[] options, String adbPath)

    boolean check(String[] options)

    void printUsage()
}

public class ADBUtils {
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
        adbPath = requestAdbPath()
    }

    public String requestAdbExec() {
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

    public String requestAdbPath() {
        def adbConnect = requestAdbExec() + " "
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

    public String getAdbPath() {
        return adbPath
    }

    private boolean isWindows() {
        return (System.properties['os.name'].toLowerCase().contains('windows'))
    }
}

public class Log {
    public static void printUsage(GfxCommand command, boolean isOptionError) {
        if (isOptionError) {
            println("You need to provide two arguments: command and option")
            println()
        }
        command.printUsage()
    }

    public static void printUsage(LayoutCommand command, boolean isOptionError) {
        if (isOptionError) {
            println("You need to provide two arguments: command and option")
            println()
        }
        command.printUsage()
    }

    public static void printUsage(OverdrawCommand command, boolean isOptionError) {
        if (isOptionError) {
            println("You need to provide two arguments: command and option")
            println()
        }
        command.printUsage()
    }

    public static void printUsage(UpdatesCommand command, boolean isOptionError) {
        if (isOptionError) {
            println("You need to provide two arguments: command and option")
            println()
        }
        command.printUsage()
    }

    public static void printUsage(DateCommand command, boolean isOptionError, String option) {
        if (isOptionError) {
            println("Not valid command option: " + option + " for: date")
            println()
        }
        command.printUsage()
    }

    public static void printUsage(boolean shouldLogCommands, String additionalMessage) {
        if (additionalMessage) {
            println(additionalMessage)
            println()
        }

        println("Usage: devtools.groovy [-v] command option")
        println()
        println("DEVTOOLS OPTIONS")
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

        if (!shouldLogCommands) {
            println("Run devtools --help for more details on how to use devtools.")
            println()

        } else {
            println("DEVTOOLS OPTIONS")
            println()

            printUsage(new GfxCommand(), false)
            printUsage(new LayoutCommand(), false)
            printUsage(new OverdrawCommand(), false)
            printUsage(new UpdatesCommand(), false)
            printUsage(new DateCommand(), false, null)
        }
        System.exit(-1)
    }
}

class ServiceCallCommand implements ICommand {

    @Override
    void execute(String[] options, String adbPath) {
        int SYSPROPS_TRANSACTION = 1599295570
        def adbCommand = adbPath + " shell service call activity $SYSPROPS_TRANSACTION"
        //println(adbCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        //println(proc.text)
    }

    @Override
    boolean check(String[] options) {
        return true;
    }

    @Override
    void printUsage() {
    }
}

class GfxCommand implements ICommand {
    def gfx_command_map = ['on': 'visual_bars', 'off': 'false', 'lines': 'visual_lines']

    @Override
    void execute(String[] options, String adbPath) {
        def adbCommand = adbPath + "shell setprop debug.hwui.profile " + gfx_command_map[options[0]]
        println(adbCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        println(proc.text)
    }

    @Override
    boolean check(String[] options) {
        if (options.size() != 1) {
            println("You need to provide two arguments: command and option")
            printUsage()
            return false;
        }
        return true;
    }

    @Override
    void printUsage() {
        println("Usage: devtools [-v] gfx option")
        println()
        println("on         visual_bars")
        println("off        false")
        println("lines      visual_lines")
        println()
    }
}

class LayoutCommand implements ICommand {
    def layout_command_map = ['on': 'true', 'off': 'false']

    @Override
    void execute(String[] options, String adbPath) {
        def adbCommand = adbPath + "shell setprop debug.layout " + layout_command_map[options[0]]
        println(adbCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        println(proc.text)
    }

    @Override
    boolean check(String[] options) {
        if (options.size() != 1) {
            println("You need to provide two arguments: command and option")
            printUsage()
            return false;
        }
        return true;
    }

    @Override
    void printUsage() {
        println("Usage: devtools.groovy [-v] layout option")
        println()
        println("on         true")
        println("off        false")
        println()
    }
}

class OverdrawCommand implements ICommand {
    def overdraw_command_map = ['on': 'show', 'off': 'false', 'deut': 'show_deuteranomaly']
    def overdraw_command_map_preKitKat = ['on': 'true', 'off': 'false']

    @Override
    void execute(String[] options, String adbPath) {
        def adbCommand = adbPath + "shell setprop debug.hwui.overdraw " + overdraw_command_map[options[0]]
        println(adbCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        println(proc.text)

        adbCommand = adbPath + "shell setprop debug.hwui.show_overdraw " + overdraw_command_map_preKitKat[options[0]]
        println(adbCommand)
        proc = adbCommand.execute()
        proc.waitFor()
        println(proc.text)
    }

    @Override
    boolean check(String[] options) {
        if (options.size() != 1) {
            println("You need to provide two arguments: command and option")
            printUsage()
            return false;
        }
        return true;
    }

    @Override
    void printUsage() {
        println("Usage: devtools.groovy [-v] layout option")
        println()
        println("on         true")
        println("off        false")
        println()
    }
}

class UpdatesCommand implements ICommand {
    def show_updates_map = ['on': '0', 'off': '1']

    @Override
    void execute(String[] options, String adbPath) {
        def adbCommand = adbPath + "shell service call SurfaceFlinger 1002 android.ui.ISurfaceComposer" + show_updates_map[options[0]]
        println(adbCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        println(proc.text)
    }

    @Override
    boolean check(String[] options) {
        if (options.size() != 1) {
            println("You need to provide two arguments: command and option")
            printUsage()
            return false;
        }
        return true;
    }

    @Override
    void printUsage() {
        println("Usage: devtools.groovy [-v] updates option")
        println()
        println("on         0")
        println("off        1")
        println()
    }
}

class DateCommand implements ICommand {
    String[] date_single_option_possibilites = ['reset']
    String[] date_format_supported = ['d', 'h', 'm', 's']
    String[] date_opration_supported = ['+', '-']
    private DateTime requestDate
    private DateTime requestedDate
    boolean isNorLater;

    public static DateTime getDeviceDateTime(String adbPath) {
        String command = adbPath + "shell date +%Y%m%d.%H%M%S"
        def proc = command.execute()
        proc.waitFor()
        String deviceDate = proc.text

        int year = Integer.valueOf(deviceDate.take(4))
        int month = Integer.valueOf(deviceDate[4..5])
        int day = Integer.valueOf(deviceDate[6..7])
        int hours = Integer.valueOf(deviceDate[9..10])
        int minutes = Integer.valueOf(deviceDate[11..12])
        int seconds = Integer.valueOf(deviceDate[13..14])

        return new DateTime(year, month, day, hours, minutes, seconds)
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

    private String buildCommand(String dateCommand) {
        if (isNorLater) {
            return "shell date " + dateCommand

        } else {
            return "shell date -s " + dateCommand
        }
    }

    private String formatDate(DateTime date) {
        String format
        if (isNorLater) {
            format = "MMddHHmmYYYY.ss"
        } else {
            format = "YYYYMMdd.HHmmss"
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(format)
        return date.toString(dateTimeFormatter)
    }

    private boolean isNOrLater(String adbPath) {
        String apiLevelCmd = adbPath + "shell getprop ro.build.version.sdk"
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

    @Override
    void execute(String[] options, String adbPath) {
        this.isNorLater = isNOrLater(adbPath)
        this.requestDate = DateTime.now()
        if (options.size() == 1 && isAValidDateSingleOption(options[0])) {
            // Reset Command
            println("Setting device date and time to now")
            this.requestedDate = DateTime.now();

        } else {
            DateTime deviceDateTime = getDeviceDateTime(adbPath)
            options.each { option ->
                if (option.length() > 4 || option.length() < 3) {
                    Log.printUsage(this, true, option)
                    System.exit(-1)
                }

                def operation = option.take(1).toString()
                def rangeType = option.reverse().take(1).reverse()

                if (!(operation in date_opration_supported)) {
                    Log.printUsage(this, true, option)
                    System.exit(-1)
                }

                if (!(rangeType in date_format_supported)) {
                    Log.printUsage(this, true, option)
                    System.exit(-1)
                }

                def range = option.substring(1, option.length() - 1)
                if (!range.isNumber()) {
                    Log.printUsage(this, true, option)
                    System.exit(-1)
                }

                deviceDateTime = applyRangeToDate(deviceDateTime, operation, Integer.valueOf(range), rangeType)
            }

            this.requestedDate = deviceDateTime
        }

        def adbCommand = adbPath + buildCommand(formatDate(requestedDate))
        println(adbCommand)
        def proc
        proc = adbCommand.execute()
        proc.waitFor()
        proc.text

        println("Date changed to " + requestedDate)
    }

    @Override
    boolean check(String[] options) {
        if (options.size() == 1 && isAValidDateSingleOption(options[0])) {
            return true
        } else {
            options.each { option ->
                def operation = option.take(1).toString()
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
            }
            return true
        }
    }

    @Override
    void printUsage() {
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
    }
}