#!/bin/bash


gnuplot -persist <<-EOFMarker
    set datafile separator "\t"
    #plot for [i=1:16] 'CSV/gnatt'.i.'.tsv' using 2 : 0 : 3 : (0.0) : yticlabel(1) with vector as 1
    plot "CSV/gnatt2.tsv" using 2 : 0 : 3 : (0.0) : yticlabel(1) with vector as 1

EOFMarker
