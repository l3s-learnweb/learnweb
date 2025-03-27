package de.l3s.collabrec;

import java.io.Serial;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LearnwebEvent;
import de.l3s.learnweb.logging.LearnwebEventListener;
import de.l3s.learnweb.user.User;

@ApplicationScoped
public class GraphEventListener implements LearnwebEventListener {
    @Serial
    private static final long serialVersionUID = 231727426122723412L;
    private static final Logger log = LogManager.getLogger(GraphEventListener.class);

    @Inject
    private GraphClient graphClient;

    @Override
    public void onEvent(final LearnwebEvent event, User user, final String sessionId) {
        try {
            if (event.getAction() == Action.register || event.getAction() == Action.login) {
                if (event.getTargetUser() != null) {
                    user = event.getTargetUser();
                }

                var update = GraphClient.updateUserNode(user);
                graphClient.update(update);
            } else if (event.getAction() == Action.group_joining) {
                if (event.getTargetUser() != null) {
                    user = event.getTargetUser();
                }

                // Node userNode = NodeFactory.createURI(GraphClient.base + "user" + user.getId());
                // Node profileNode = NodeFactory.createURI(GraphClient.base + "UserProfile" +  user.getId());
                //
                // UpdateBuilder ub = new UpdateBuilder(GraphClient.prefixMapping)
                //     .addInsert(subj, "rdf:type", "educor:User")
                //     .addBind("schema:name", user.getFullName())
                //     .addBind("schema:email", user.getEmail())
                //     .addBind("educor:hasProfile", profile);

                // graphClient.updateData(query);
            } else if (event.getAction() == Action.group_creating) {

            }
        } catch (
            GraphException e) {
            log.error("Failed to update graph on: {}", event, e);
        }
    }
}
