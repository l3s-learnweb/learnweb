package de.l3s.collabrec;

import java.io.Serial;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LearnwebEvent;
import de.l3s.learnweb.logging.LearnwebEventListener;
import de.l3s.learnweb.user.User;

@Dependent
public class GraphEventListener implements LearnwebEventListener {
    @Serial
    private static final long serialVersionUID = 231727426122723412L;
    private static final Logger log = LogManager.getLogger(GraphEventListener.class);

    @Inject
    private GraphClient graphClient;

    @Override
    public void onEvent(final LearnwebEvent event, User user, final String sessionId) {
        if (event.getAction() == Action.register || event.getAction() == Action.login) {
            if (event.getTargetUser() != null) {
                user = event.getTargetUser();
            }

            String query = """
                PREFIX schema: <https://schema.org/>
                PREFIX foaf: <https://xmlns.com/foaf/spec/#>
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX educor: <https://github.com/tibonto/educor#>
                PREFIX base: <https://github.com/l3s-learnweb/collabrec/> .

                INSERT DATA {
                  base:user%1$d a educor:User ;
                           schema:name "%2$s" ;
                           schema:email "%3$s" ;
                           educor:hasProfile base:userProfile%1$d .

                  base:userProfile%1$d a educor:UserProfile ;
                              base:interest "%4$s"@en ;
                              base:profession "%5$s" ;
                              base:username "%6$s"^^xsd:string .
                }
                """.formatted(user.getId(), user.getFullName(), user.getEmail(), user.getInterest(), user.getProfession(), user.getUsername());
            graphClient.updateData(query);
        }

        if (event.getAction() == Action.group_joining) {
            if (event.getTargetUser() != null) {
                user = event.getTargetUser();
            }

            String query = """
                PREFIX schema: <https://schema.org/>
                PREFIX foaf: <https://xmlns.com/foaf/spec/#>
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX educor: <https://github.com/tibonto/educor#>
                PREFIX base: <https://github.com/l3s-learnweb/collabrec/> .

                INSERT DATA {
                  # Create a User
                  # base:LearnwebIDofUser the base:user%1$d
                  base:user%1$d rdf:type educor:User ;
                           schema:name "%2$s" ;
                           schema:email "%3$s" ;
                           educor:hasProfile educor:UserProfile%1$d .

                  # Associate a UserProfile with the User
                  base:UserProfile%1$d rdf:type educor:UserProfile ;
                              base:interest "%4$s"@en ;
                              base:profession "%5$s" ;
                              base:username "%6$s"^^xsd:string .
                }
                """.formatted(user.getId(), user.getFullName(), user.getEmail(), user.getInterest(), user.getProfession(), user.getUsername());
            graphClient.updateData(query);
        }
    }
}
