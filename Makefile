default: build

build:
	javac hlt/*.java
	javac MyBot.java

run:
	java MyBot

test:
	python run.py --cmd "java MyBot" --round 2

clean:
	rm -f MyBot.class
	rm -f hlt/*.class
	rm -f *.log
	rm -f *.json
	rm -f *.hlt
