# LFG
Log File Generator (LFG) allows generating Network Event Sequence (NES) data of any length based on a small piece of real log data.

For running LFG the simple logfile clustering tool (SLCT) is required. Therefore download slct-0.05 from  http://ristov.github.io/slct/. In slct.c before compiling the following lines have to be modified to be compatible with LFG:

To avoid storage allocation errors in case of using large input files:

-) In lines  290, 463, 586, 656, 1017 change

char words[MAXWORDS][MAXWORDLEN];

to

static char words[MAXWORDS][MAXWORDLEN];

Since in our test files "\asterisk" occurred as single "word" we changed the wildcard symbol to "<\asterisk":

-) In line 1204

printf("%s\asterisk%s ", buffer, ptr2->word + ptr2->tail);

has to be changed to

printf("%s<\asterisk%s ", buffer, ptr2->word + ptr2->tail);

-) In line 1209

} else { printf("\asterisk "); }

has to be changed to

} else { printf("<\asterisk "); }

Make sure that the compiled executeable is named slct.exe

The actual LFG prototype was only tested on windows 7. Since SLCT is executed via command line in case of using a different OS a change in LogLineClustering.java in line number 53 might be required.

Main functionalities:

1) Calculating log line clusters applying SLCT
2) Calculating clusterValue, which specifies the significance of the SLCT clusters
3) Sorting log lines to the SLCT clusters
4) Finding the most accurate cluster for each log line based on the clusterValue
5) Calculate transition matrix
6) Generate NESData using a markov chain approach
7) Generate timestamps for the NESData
8) Fill NESData with log line content

Input arguments:

args[0] <- Input log file
args[1] <- Filename of the output of the preprocessed input log file  (log files have to be preprocessed and stored in a file before executing SLCT)
args[2] <- Integer, specifying the first x bits of the log line, which should be not considered for clustering (time stamp)
args[3] <- Integer, support threshold value for SLCT (for further explanations go to http://ristov.github.io/slct/slct.html)
args[4] <- Filename, SLCT output (cluster descriptions)
args[5] <- Integer, number of log lines in the generated log file
args[6] <- Timestamp, date, when the generated log file should start ("MMM dd HH:mm:ss")

In case of large input files use appropriate values for the VM argument -Xmx.

Output:

clustersLFG.txt <- stores the cluster descriptions.
outliersLFG.txt <- stores the outliersLFG.
NESData.txt <- Stores raw NES data. This means only the cluster numbers as placeholders for log line content.
fillesNESData.txt <- The placeholders are replaced with timestamps and log line content.

Author:
Markus Wurzenberger <mwurzenberger@gmail.com>
Generated January 2016
