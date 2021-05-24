package org.glygen.array.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.dao.SesameSparqlDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class RepositoryUpdateUtil {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");
    
    @Autowired
    SesameSparqlDAO sparqlDAO;
    
    
    @EventListener(ApplicationReadyEvent.class)
    public boolean runUpdateScripts() {
        try {
            File file = new File("updates/update.sparql");
            if (file.exists()) {
                // read the file line by line and execute the updates
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String query = scanner.nextLine();
                    sparqlDAO.update(query);
                }
                scanner.close();
                // move the file, so it won't be executed again.
                file.renameTo(new File("updates/executedupdate" + System.currentTimeMillis() + ".sparql"));
                return true;
            }
        } catch (FileNotFoundException e) {
            // nothing to do, there is no update file
            logger.info("update file not found", e);
        } catch (SparqlException e) {
            logger.error("update script cannot be executed", e);
        }
        return false;
    }

}
