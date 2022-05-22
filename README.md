# Java Forensics Toolkit

![broken java cup](BrokenJavaCup.jpg)

A little *Java Forensics Toolkit* that can dump all loaded classes within a JVM
```
usage: java -jar JavaForensicsToolkit.jar [-e] [-d destination.jar] [-f filter]... [-x] <pid>

options:
-v	verbose agent logging
-e	agent will log to stderr instead of stdout
-d	jar file destination of dumped classes
	Relative paths will be relative with respect to the target process.
	A jar file in temp will be generated if no destination was provided.
-f	regular expression class name filter
	Can be specified multiple times.
-x	exclude classes matching the filter
pid	process id of the target java process

example:
java -jar JavaForensicsToolkit.jar -d dump.jar -f java\\..* -f sun\\..* -f jdk\\..* -f com\\.sun\\..* -x 123456
```

A small report with information about each class is generated and placed in the destination jar as well.

Here is an example:
```
$ java -jar JavaForensicsToolkit.jar -v 24576
--------------------------------------------------------
--> Java Forensics Toolkit v1.00 by Benjamin Sølberg <--
--------------------------------------------------------
https://github.com/BenjaminSoelberg/JavaForensicsToolkit

Injecting agent into JVM with pid: 24576
Dumping classes to: /tmp/dump-24651-9890604330559988075.jar
--------------------------------------------------------
--> Java Forensics Toolkit v1.00 by Benjamin Sølberg <--
--------------------------------------------------------
https://github.com/BenjaminSoelberg/JavaForensicsToolkit

Agent loaded with options: -v -d /tmp/dump-24651-9890604330559988075.jar -f .* 24576

Querying classes...

Dumping started...
Ignoring io/github/benjaminsoelberg/jft/Transformer
Ignoring io/github/benjaminsoelberg/jft/ParserException
Ignoring io/github/benjaminsoelberg/jft/Utils
Ignoring io/github/benjaminsoelberg/jft/Options
Ignoring io/github/benjaminsoelberg/jft/ClassInfo
Ignoring io/github/benjaminsoelberg/jft/Report
Ignoring io/github/benjaminsoelberg/jft/ClassDumper
Ignoring io/github/benjaminsoelberg/jft/DummyRunner
Dumping java.io.FileOutputStream$1
Dumping java.util.Vector$Itr
Dumping java.util.concurrent.ConcurrentLinkedQueue$CLQSpliterator
Dumping java.util.zip.ZipOutputStream$XEntry
...
Dumping java.lang.Throwable
Dumping java.lang.System
Dumping java.lang.ClassLoader
Dumping java.lang.Cloneable
Dumping java.lang.Class
Dumping java.lang.reflect.Type
Dumping java.lang.reflect.GenericDeclaration
Dumping java.lang.reflect.AnnotatedElement
Dumping java.lang.String
Dumping java.lang.CharSequence
Dumping java.lang.Comparable
Dumping java.io.Serializable
Dumping java.lang.Object

Creating jar...
Class info: java.io.FileOutputStream$1@null@0 726 bytes
Class info: java.util.Vector$Itr@null@0 2492 bytes
Class info: java.util.concurrent.ConcurrentLinkedQueue$CLQSpliterator@null@0 3608 bytes
Class info: java.util.zip.ZipOutputStream$XEntry@null@0 563 bytes
Class info: java.time.chrono.IsoChronology@null@0 11739 bytes
Class info: java.time.chrono.AbstractChronology@null@0 15281 bytes
Class info: java.time.chrono.Chronology@null@0 8639 bytes
Class info: java.time.temporal.TemporalAdjusters@null@0 6423 bytes
Class info: java.time.LocalDate@null@0 27720 bytes
...
Class info: java.time.chrono.ChronoLocalDate@null@0 9575 bytes
Class info: java.time.zone.ZoneOffsetTransition@null@0 6109 bytes
Class info: java.time.temporal.ValueRange@null@0 4463 bytes
Class info: java.math.BigInteger@null@0 59485 bytes
Class info: java.time.Duration@null@0 18086 bytes
Class info: java.time.temporal.TemporalAmount@null@0 399 bytes
Class info: java.lang.Comparable@null@0 235 bytes
Class info: java.io.Serializable@null@0 113 bytes
Class info: java.lang.Object@null@0 1944 bytes

Dumped classes, including report.txt, can be found in: /tmp/dump-24651-9890604330559988075.jar
Done
```
