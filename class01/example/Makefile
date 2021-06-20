file="example.jj"
class="Example"

default:
	@javacc "${file}"
	@javac *.java
	@java "${class}"

clean:
	@rm -f *.java *.class
