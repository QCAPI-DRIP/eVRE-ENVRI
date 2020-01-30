/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.vre4eic.prise.controller;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.uva.sne.vre4eic.prise.service.ProvParser;
import nl.uva.sne.vre4eic.prise.service.WorkflowParser;
import nl.uva.sne.vre4eic.prise.util.Util;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProvIntegratorController {

    private ProvParser provParser;
    private WorkflowParser wfParser;

    private final String[] provExtensions = new String[]{"prov.ttl"};
    private final String[] workflowExtentions = new String[]{"t2flow"};

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody String submit(@RequestParam("files") MultipartFile[] files) {
        String output = new String();
        File provFile = null,
                wfFile = null;

        try {
            for (MultipartFile file : files) {
                if (file.getContentType() != null) {
                    for (String ext : workflowExtentions) {
                        if (file.getOriginalFilename().endsWith(ext)) {
                            wfFile = Util.convert(file);
                        }
                        break;
                    }

                    for (String ext : provExtensions) {
                        if (file.getOriginalFilename().endsWith(ext)) {
                            provFile = Util.convert(file);
                        }
                        break;
                    }
                }
            }

            wfParser = new WorkflowParser(wfFile);
            provParser = new ProvParser(provFile, wfParser.extractServices());
        } catch (Exception e) {
            Logger.getLogger(ProvIntegratorController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        String services = "\"services\":" + wfParser.getServicelist().toString();
        String workflow = "\"workflow\":" + provParser.getWorkflow().toString();
        return "{" + services + "," + workflow + "}";
    }
    

}
