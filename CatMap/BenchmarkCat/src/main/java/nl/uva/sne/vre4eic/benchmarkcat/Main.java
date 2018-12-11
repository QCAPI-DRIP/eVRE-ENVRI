/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.benchmarkcat;

import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static void main(String[] args) {
        benchmarkConversion();
    }

    private static void benchmarkConversion() {
        Collection<Tag> tags = new ArrayList<>();
        String catalogueURL = null;
        tags.add(Tag.of("source", catalogueURL));
        String mappingName = null;
        tags.add(Tag.of("mapping.name", mappingName));
        String exportID = null;
        tags.add(Tag.of("exportID", exportID));
        Object numOfConsumers = null;
        tags.add(Tag.of("num.of.consumers", String.valueOf(numOfConsumers)));
        Object recordSize = null;
        tags.add(Tag.of("records.size", String.valueOf(recordSize)));
    }

}
