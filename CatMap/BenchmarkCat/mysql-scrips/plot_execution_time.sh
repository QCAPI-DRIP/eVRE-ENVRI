#!/bin/bash


gnuplot -persist <<-EOFMarker
    set xlabel "Num. Of Workers"
    set ylabel "Execution Time, sec"
    set datafile separator "\t"
    
    plot "exec_time.tsv" using 1:2 with linespoints linetype 1, \
        "" using 1:2:3 with errorbars linetype 1
   
EOFMarker
