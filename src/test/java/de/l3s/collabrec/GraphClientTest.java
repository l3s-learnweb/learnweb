package de.l3s.collabrec;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

class GraphClientTest {

    @Test
    void testUpdateUserNode() {
        User user = new User();
        user.setId(5);
        user.setUsername("hello_user");
        user.setFullName("Hello User");
        user.setInterest("Programming");
        user.setProfession("Software Engineer");

        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  educor: <https://github.com/tibonto/educor/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            DELETE {
              base:user5 schema:name ?old_name .
              base:userProfile5 base:interest ?old_interest .
              base:userProfile5 base:profession ?old_profession .
              base:userProfile5 base:username ?old_username .
            }
            INSERT {
              base:user5 rdf:type educor:User .
              base:user5 educor:hasProfile base:userProfile5 .
              base:userProfile5 rdf:type educor:UserProfile .
              base:user5 schema:name "Hello User" .
              base:userProfile5 base:interest "Programming" .
              base:userProfile5 base:profession "Software Engineer" .
              base:userProfile5 base:username "hello_user" .
            }
            WHERE
              { OPTIONAL
                  { base:user5  schema:name  ?old_name}
                OPTIONAL
                  { base:userProfile5
                              base:interest  ?old_interest}
                OPTIONAL
                  { base:userProfile5
                              base:profession  ?old_profession}
                OPTIONAL
                  { base:userProfile5
                              base:username  ?old_username}
              }
            """, GraphClient.updateUserNode(user).buildRequest().toString());
    }

    @Test
    void testUpdateUserNodeSkipsNullValues() {
        User user = new User();
        user.setId(5);
        user.setUsername("hello_user");

        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  educor: <https://github.com/tibonto/educor/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            DELETE {
              base:user5 schema:name ?old_name .
              base:userProfile5 base:interest ?old_interest .
              base:userProfile5 base:profession ?old_profession .
              base:userProfile5 base:username ?old_username .
            }
            INSERT {
              base:user5 rdf:type educor:User .
              base:user5 educor:hasProfile base:userProfile5 .
              base:userProfile5 rdf:type educor:UserProfile .
              base:userProfile5 base:username "hello_user" .
            }
            WHERE
              { OPTIONAL
                  { base:user5  schema:name  ?old_name}
                OPTIONAL
                  { base:userProfile5
                              base:interest  ?old_interest}
                OPTIONAL
                  { base:userProfile5
                              base:profession  ?old_profession}
                OPTIONAL
                  { base:userProfile5
                              base:username  ?old_username}
              }
            """, GraphClient.updateUserNode(user).buildRequest().toString());
    }

    @Test
    void testUpdateGroupNode() {
        Group group = new Group();
        group.setId(1);
        group.setTitle("Group Title");
        group.setDescription("Group Description");
        group.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));

        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  educor: <https://github.com/tibonto/educor/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            DELETE {
              base:group1 schema:name ?old_name .
              base:group1 schema:description ?old_description .
              base:group1 schema:dateCreated ?old_dateCreated .
            }
            INSERT {
              base:group1 rdf:type foaf:Group .
              base:group1 schema:name "Group Title" .
              base:group1 schema:description "Group Description" .
              base:group1 schema:dateCreated "2025-01-01T12:00:00"^^xsd:dateTime .
            }
            WHERE
              { OPTIONAL
                  { base:group1  schema:name  ?old_name}
                OPTIONAL
                  { base:group1  schema:description  ?old_description}
                OPTIONAL
                  { base:group1  schema:dateCreated  ?old_dateCreated}
              }
            """, GraphClient.updateGroupNode(group).buildRequest().toString());
    }

    @Test
    void testAssociateUserGroup() {
        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  educor: <https://github.com/tibonto/educor/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            INSERT DATA {
              base:user5 schema:memberOf base:group1 .
            }
            """, GraphClient.associateUserGroup(5, 1).buildRequest().toString());
    }

    @Test
    void testDissociateUserGroup() {
        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  educor: <https://github.com/tibonto/educor/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            DELETE DATA {
              base:user5 schema:memberOf base:group1 .
            }
            """, GraphClient.dissociateUserGroup(5, 1).buildRequest().toString());
    }
}
