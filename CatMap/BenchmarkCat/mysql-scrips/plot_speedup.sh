#!/bin/bash


gnuplot -persist <<-EOFMarker
    set xlabel "Num. Of Workers"
    set ylabel "Speedup Time, sec"
    set datafile separator "\t"
    plot "speedup_efficiency.tsv" using 1:2 with linespoints linetype 1, \
        "" using 1:3 with linespoints linetype 2
   
EOFMarker
