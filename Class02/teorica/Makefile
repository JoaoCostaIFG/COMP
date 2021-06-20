file="Calculator"

default:
	@jjtree "${file}.jjt"
	@javacc "${file}.jj"
	@javac *.java
	@java "${file}"

clean:
	@rm -f *.class
