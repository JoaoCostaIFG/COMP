import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.List;
import java.util.Scanner;

public class Main implements JmmParser {
    public JmmParserResult parse(String jmmCode) {
        Jmm jmm = new Jmm(new StringReader(jmmCode));
        try {
            SimpleNode root = jmm.Program(); // returns reference to root node
            // root.dump(""); // prints the tree on the screen

            return new JmmParserResult(root, jmm.getReports());
        } catch (ParseException e) {
            List<Report> reports = jmm.getReports();
            e.setErrMsg("Failed to parse the given file");
            System.err.println(e.getErrMsg());
            reports.add(e.getReport());
            return new JmmParserResult(null, jmm.getReports());
        }
    }

    public static void err(String errStr) {
        System.err.println(errStr);
        System.exit(1);
    }

    public static void usage() {
        err("Usage: Main [-o] [-r <num>] <class.jmm>");
    }

    public static void main(String[] args) {
        boolean doOptimizations = false;
        int regLimit = 0;
        String filename = null;

        if (args.length == 0)
            usage();

        boolean noMoreArgs = false;
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            switch (arg) {
                case "-o":
                    if (!noMoreArgs)
                        err("Unknown file " + arg + ".");
                    doOptimizations = true;
                    break;
                case "-r":
                    if (!noMoreArgs)
                        err("Unknown file " + arg + ".");
                    if (i == args.length - 1)
                        err("-r needs an argument, <num>.");
                    regLimit = Integer.parseInt(args[++i]);
                    break;
                case "--":
                    noMoreArgs = true;
                    break;
                default:
                    if (!noMoreArgs && arg.charAt(0) == '-')
                        err("Unknown option " + arg + ".");
                    if (filename != null)
                        err("You can only compile a single file at a time.");

                    filename = arg;
                    if (!filename.matches(".*\\.jmm$"))
                        err("Can only compile '.jmm' files.");
                    break;
            }
        }

        File f = new File(filename);
        Scanner fReader = null;
        try {
            fReader = new Scanner(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        StringBuilder stringBuilder = new StringBuilder();
        while (fReader.hasNextLine())
            stringBuilder.append(fReader.nextLine());
        String jmmCode = stringBuilder.toString();
        new Main().parse(jmmCode);
    }
}