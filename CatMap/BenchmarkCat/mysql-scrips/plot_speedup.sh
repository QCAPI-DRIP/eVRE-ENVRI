#!/bin/bash


gnuplot -persist <<-EOFMarker
    set xlabel "Num. Of Workers"
    set ylabel "Speedup Time, sec"
    set datafile separator "\t"
    plot "CSV/speedup_efficiency-100.tsv" using 1:2 with linespoints linetype 1, \
        "CSV/speedup_efficiency-100.tsv" using 1:3 with linespoints linetype 2,\
        "CSV/speedup_efficiency-200.tsv" using 1:2 with linespoints linetype 3, \
        "CSV/speedup_efficiency-200.tsv" using 1:3 with linespoints linetype 4
   
EOFMarker
