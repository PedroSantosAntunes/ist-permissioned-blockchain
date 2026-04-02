# BlockchainIST

Distributed Systems Project 2026

**Group T46**

*(choose one of the following levels and erase the other one)*  
**Difficulty level: I am Death incarnate!**


### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name              | User                                    | Email                                               |
|--------|-------------------|-----------------------------------------|-----------------------------------------------------|
| 63484  | Michael Maycock   | <https://github.com/updatemike>         | <mailto:michael.maycock@tecnico.ulisboa.pt>         |
| 110306 | Diogo Fernandes   | <https://github.com/diiogofer>          | <mailto:diogo.sendim.fernandes@tecnico.ulisboa.pt>  |
| 106526 | Pedro Antunes     | <https://github.com/PedroSantosAntunes> | <mailto:pedro.santos.antunes@tecnico.ulisboa.pt>    |

## Getting Started

The overall system is made up of several modules.
The definition of messages and services is in _Contract_.

See the [Project Statement](https://github.com/tecnico-distsys/BlockchainIST-2026) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.


## How to Run
Compile in the root directory:  
```s
mvn clean install
```

Open directory /sequencer and run:
```s
mvn exec:java
```

On another terminal open /node and run:
```s
mvn exec:java
```

Finally
 - either open /client on another terminal and run:
    ```s
    mvn exec:java
    ```

 - Or open /tests on another terminal and run:
    ```s
    ./run_tests.sh
    ```


## Debug

An extra argument can be used to run on Debug (`-Ddebug=1`) mode that adds extra information.
```s
mvn -Ddebug=1 exec:java
```

## Arguments

Pass runtime arguments with `-Dexec.args`, for example on the sequencer (port number, blocksize , createBlockTimeout).
```s
mvn exec:java -Dexec.args="5000 4 5"
```

## Run tests

Open directory /tests and run:
```s
./start_run_tests.sh
```