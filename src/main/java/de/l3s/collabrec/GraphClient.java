package de.l3s.collabrec;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.ConfigProvider;

@ApplicationScoped
public class GraphClient implements Serializable {
    @Serial
    private static final long serialVersionUID = 8443696658680840510L;
    private static final Logger log = LogManager.getLogger(GraphClient.class);

    @Inject
    private ConfigProvider configProvider;

    private RDFConnection createConnection() {
        return RDFConnection.connectPW(
            configProvider.getProperty("fuseki_url"),
            configProvider.getProperty("fuseki_user"),
            configProvider.getProperty("fuseki_password")
        );
    }

    public void insertData(Model model) {
        try (RDFConnection conn = createConnection()) {
            conn.load(model);
            log.debug("Inserted {} triples into dataset {}", model.size());
        } catch (Exception e) {
            log.error("Failed to insert data into Fuseki", e);
            throw new RuntimeException("Failed to insert data into Fuseki", e);
        }
    }

    public void updateData(String update) {
        try (RDFConnection conn = createConnection()) {
            conn.update(update);
            log.debug("Executed SPARQL update on dataset {}");
        } catch (Exception e) {
            log.error("Failed to update data in Fuseki", e);
            throw new RuntimeException("Failed to update data in Fuseki", e);
        }
    }

    public Model queryData(String query) {
        try (RDFConnection conn = createConnection()) {
            return conn.queryConstruct(query);
        } catch (Exception e) {
            log.error("Failed to query data from Fuseki", e);
            throw new RuntimeException("Failed to query data from Fuseki", e);
        }
    }
}
