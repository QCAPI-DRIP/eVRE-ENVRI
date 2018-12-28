#!/bin/bash


gnuplot -persist <<-EOFMarker
    set xlabel "Num. Of Workers"
    set ylabel "Execution Time, sec"
    set datafile separator "\t"
    
    plot "CSV/exec_time-100.tsv" using 1:2 with linespoints linetype 1, \
        "CSV/exec_time-100.tsv" using 1:2:3 with errorbars linetype 1, \
    "CSV/exec_time-200.tsv" using 1:2 with linespoints linetype 2, \
    "CSV/exec_time-200.tsv" using 1:2:3 with errorbars linetype 2
   
EOFMarker
