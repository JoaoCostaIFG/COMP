import Analysis.AnalysisStage;
import Backend.BackendStage;
import LLIR.OptimizationStage;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.StringReader;
import java.util.List;

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
            reports.add(e.getReport());
            return new JmmParserResult(null, jmm.getReports());
        }
    }

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        return new AnalysisStage().semanticAnalysis(parserResult);
    }

    public OllirResult llirStage(JmmSemanticsResult semanticsResult, boolean optimize, boolean debug) {
        return new OptimizationStage(debug).toOllir(semanticsResult, optimize);
    }

    public JasminResult backendStage(OllirResult ollirResult, int regLimit, boolean debug) {
        return new BackendStage(debug, regLimit).toJasmin(ollirResult);
    }

    public static void err(String errStr) {
        System.err.println(errStr);
        System.exit(1);
    }

    public static void usage() {
        err("Usage: Main [-o] [-r <num>] <class.jmm>");
    }

    public static void main(String[] args) {
        boolean verbose = false;
        boolean doOptimizations = false;
        int regLimit = 0;
        String filename = null;

        if (args.length == 0)
            usage();

        boolean noMoreArgs = false;
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.equals("-o") && !noMoreArgs) {
                doOptimizations = true;
            } else if (arg.equals("-v") && !noMoreArgs) {
                verbose = true;
            } else if (arg.equals("-r") && !noMoreArgs) {
                if (i == args.length - 1)
                    err("-r needs an argument, <num>.");
                regLimit = Integer.parseInt(args[++i]);
                if (regLimit < 0)
                    err("Registers can't be limited to a negative value.");
            } else if (arg.equals("--")) {
                noMoreArgs = true;
            } else {
                if (!noMoreArgs && arg.charAt(0) == '-')
                    err("Unknown option " + arg + ".");
                if (filename != null)
                    err("You can only compile a single file at a time.");

                filename = arg;
                if (!filename.matches(".*\\.jmm$"))
                    err("Can only compile '.jmm' files.");
            }
        }
        // we need to have a file
        if (filename == null)
            usage();
        String jmmCode = SpecsIo.read(filename);

        System.out.println("Optimizations are " + (doOptimizations ? "on" : "off"));
        if (regLimit == 0) System.out.println("Registers are not limited (register allocation)");
        else System.out.println("Registers are limited to: " + regLimit);

        Main main = new Main();
        // Syntatic analysis stage
        System.out.println("Syntatic analysis stage...");
        JmmParserResult parserResult = main.parse(jmmCode);
        for (Report report : parserResult.getReports()) {
            if (report.getType() == ReportType.ERROR)
                err("Found errors: " + parserResult.getReports());
        }
        // Semantic analysis stage
        System.out.println("Semantic analysis stage...");
        JmmSemanticsResult semanticsResult = main.semanticAnalysis(parserResult);
        for (Report report : semanticsResult.getReports()) {
            if (report.getType() == ReportType.ERROR)
                err("Found errors: " + semanticsResult.getReports());
        }
        // LLIR stage
        System.out.println("LLIR stage...");
        OllirResult ollirResult = main.llirStage(semanticsResult, doOptimizations, verbose);
        for (Report report : ollirResult.getReports()) {
            if (report.getType() == ReportType.ERROR)
                err("Found errors: " + ollirResult.getReports());
        }
        // Backend stage
        System.out.println("Backend stage...");
        JasminResult jasminResult = main.backendStage(ollirResult, regLimit, verbose);
        for (Report report : jasminResult.getReports()) {
            if (report.getType() == ReportType.ERROR)
                err("Found errors: " + jasminResult.getReports());
        }

        // write Jasmin file
        String outFile = SpecsIo.getResourceName(filename);
        outFile = outFile.substring(0, outFile.length() - 3) + "j";
        SpecsIo.write(new File(outFile), jasminResult.getJasminCode());

        // show reports (if any)
        List<Report> reports = jasminResult.getReports();
        if (reports.size() == 0) System.out.println("No reports.");
        else System.out.println("Reports: " + reports);
    }
}