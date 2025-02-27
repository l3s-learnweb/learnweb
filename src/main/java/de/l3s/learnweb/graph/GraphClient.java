package de.l3s.learnweb.graph;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class GraphClient implements Serializable {
    @Serial
    private static final long serialVersionUID = 8443696658680840510L;
    private static final Logger log = LogManager.getLogger(GraphClient.class);

    private static final String FUSEKI_URL = "http://localhost:3030";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "CEiKaDgy$4K6q7";
    private static final String DATASET = "ds";

    private RDFConnection createConnection(String dataset) {
        return RDFConnection.connectPW(FUSEKI_URL + "/" + dataset, ADMIN_USER, ADMIN_PASSWORD);
    }

    public void insertData(String dataset, Model model) {
        try (RDFConnection conn = createConnection(dataset)) {
            conn.load(model);
            log.debug("Inserted {} triples into dataset {}", model.size(), dataset);
        } catch (Exception e) {
            log.error("Failed to insert data into Fuseki", e);
            throw new RuntimeException("Failed to insert data into Fuseki", e);
        }
    }

    public void updateData(String dataset, String update) {
        try (RDFConnection conn = createConnection(dataset)) {
            conn.update(update);
            log.debug("Executed SPARQL update on dataset {}", dataset);
        } catch (Exception e) {
            log.error("Failed to update data in Fuseki", e);
            throw new RuntimeException("Failed to update data in Fuseki", e);
        }
    }

    public Model queryData(String dataset, String query) {
        try (RDFConnection conn = createConnection(dataset)) {
            return conn.queryConstruct(query);
        } catch (Exception e) {
            log.error("Failed to query data from Fuseki", e);
            throw new RuntimeException("Failed to query data from Fuseki", e);
        }
    }
}
