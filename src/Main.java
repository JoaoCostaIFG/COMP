import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public class Main implements JmmParser {
	public JmmParserResult parse(String jmmCode) {
		Jmm jmm = new Jmm(new StringReader(jmmCode));
		try {
    		SimpleNode root = jmm.Program(); // returns reference to root node
    		// root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, jmm.getReports());
		} catch(ParseException e) {
			List<Report> reports = jmm.getReports();
			e.setErrMsg("Failed to parse the given file");
			System.err.println(e.getErrMsg());
			reports.add(e.getReport());
			return new JmmParserResult(null, jmm.getReports());
		}
	}

    public static void main(String[] args) {
        System.out.println("Executing with args: " + Arrays.toString(args));
        if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }
    }
}