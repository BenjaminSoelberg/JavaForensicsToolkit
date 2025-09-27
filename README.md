# Easy and compact Java Forensics Toolkit

![broken java cup](BrokenJavaCup.jpg)

# Java Forensics Toolkit

The **Java Forensics Toolkit** is a lightweight JVM agent that allows you to **inspect, snapshot, and export all loaded classes** from a running Java process.<br>
Designed for **forensic analysis, debugging, and reverse engineering**, making it easier to discover unexpected or malicious code inside live JVMs.

## Features

- 📦 **Snapshot all loaded classes** – capture bytecode *after* any runtime transformations.
- 🔍 **Class loader analysis** – see which classes exist in which class loader.
- 🛡️ **Security investigations** – detect hidden, injected, or malicious classes.
- 🧩 **Reverse engineering** – dumped classes can be decompiled with standard tools.
- 🎯 **Flexible filters** – include/exclude classes with regex filters.
- ⚡ **Simple usage** – attach to any running JVM by PID with a single command.

## Installation

Download the latest release JAR from [here](https://github.com/BenjaminSoelberg/JavaForensicsToolkit/releases).

## build
```
mvn clean package
```

## Usage

```
java -jar JavaForensicsToolkit-<version>.jar 

---------------------------------------------------------
--> Java Forensics Toolkit v1.1.0 by Benjamin Sølberg <--
---------------------------------------------------------
https://github.com/BenjaminSoelberg/JavaForensicsToolkit

usage: java -jar JavaForensicsToolkit.jar [-v] [-e] [-d destination.jar] [-s] [-p] [-f filter]... [-x] <pid>

options:
-v      verbose agent logging
-e      agent will log to stderr instead of stdout
-d      jar file destination of dumped classes
        Relative paths will be relative with respect to the target process.
        A jar file in temp will be generated if no destination was provided.
-s      ignore system class loader (like java.lang.String)
-p      ignore platform class loader (like system extensions)
-f      regular expression class name filter
        Can be specified multiple times.
-x      exclude classes matching the filter
pid     process id of the target java process

example:
java -jar JavaForensicsToolkit.jar -d dump.jar -f 'java\\..*' -f 'sun\\..*' -f 'jdk\\..*' -f 'com\\.sun\\..*' -x 123456
```

## Example

Dump all non-JDK classes from a running process with PID 123456:

```
java -jar JavaForensicsToolkit.jar -v -d dump.jar -f java\\..* -f sun\\..* -f jdk\\..* -f com\\.sun\\..* -x 123456
```

## Typical Use Cases

- 🔐 **Malware hunting** – identify injected or malicious classes hidden inside a compromised JVM.
- 🛠️ **Debugging classpath issues** – find out which versions of classes are actually loaded and by which class loaders.
- 📊 **Security audits** – verify that only expected code is running inside production JVMs.
- 🕵️ **Incident response** – capture a live snapshot of loaded classes for later offline analysis.
- ⚙️ **Reverse engineering & research** – decompile transformed classes to understand runtime modifications (e.g. instrumentation, bytecode weaving, or AOP frameworks).

## How It Works

1) **Attach to target JVM**<br>The toolkit uses the standard com.sun.tools.attach API to connect to a running Java process by PID.
2) **Load agent**<br>Once attached, it dynamically loads a lightweight Java agent into the target JVM without requiring a restart.
3) **Enumerate classes**<br>The agent queries the JVM for all currently loaded classes and their associated class loaders.
4) **Dump bytecode**<br>Each class is retrieved as it exists in memory after any transformations (e.g. instrumentation, weaving, or obfuscation).
5) **Write output**<br>The classes are packaged into a JAR file (either user-specified or temporary) for convenient storage and analysis.

This approach ensures the dumped classes reflect the exact state of the JVM at runtime, providing a faithful snapshot for investigation.