package de.l3s.collabrec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

class GraphClientTest {

    @Test
    void insertData() {
    }

    @Test
    void testUserUpdateQuery() {
        User user = new User();
        user.setId(5);
        user.setUsername("hello_user");
        user.setFullName("Hello User");
        user.setEmail("hello@example.com");
        user.setInterest("Programming");
        user.setProfession("Software Engineer");

        String expected = """
            PREFIX schema: <https://schema.org/>
            PREFIX foaf: <https://xmlns.com/foaf/spec/#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX educor: <https://github.com/tibonto/educor#>
            PREFIX base: <https://github.com/l3s-learnweb/collabrec/> .

            INSERT DATA {
              base:user%1$d rdf:type educor:User ;
                       schema:name "%2$s" ;
                       schema:email "%3$s" ;
                       educor:hasProfile educor:UserProfile%1$d .

              base:UserProfile%1$d rdf:type educor:UserProfile ;
                          base:interest "%4$s"@en ;
                          base:profession "%5$s" ;
                          base:username "%6$s"^^xsd:string .
            }
            """.formatted(user.getId(), user.getFullName(), user.getEmail(), user.getInterest(), user.getProfession(), user.getUsername());

        assertEquals(expected, GraphClient.createUserUpdate(user).buildRequest().toString());
    }

    @Test
    void testCreateGroupUpdate() {
        Group group = new Group();
        group.setId(1);
        group.setTitle("Group Title");
        group.setDescription("Group Description");

        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  educor: <https://github.com/tibonto/educor#>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  owl:  <http://www.w3.org/2002/07/owl#>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  dc:   <http://purl.org/dc/elements/1.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            INSERT {
              base:group1 rdf:type foaf:Group .
              base:group1 schema:name "Group Title" .
              base:group1 schema:description "Group Description" .
            }
            WHERE
              { MINUS
                  { base:group1  rdf:type  foaf:Group }
              }
            """, GraphClient.createGroupUpdate(group).buildRequest().toString());
    }

    @Test
    void testUpdateGroupUpdate() {
        Group group = new Group();
        group.setId(1);
        group.setTitle("Group Title");
        group.setDescription("Group Description");

        assertEquals("""
            PREFIX  schema: <https://schema.org/>
            PREFIX  educor: <https://github.com/tibonto/educor#>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX  owl:  <http://www.w3.org/2002/07/owl#>
            PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX  foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX  dc:   <http://purl.org/dc/elements/1.1/>
            PREFIX  base: <https://github.com/l3s-learnweb/collabrec/>

            DELETE {
              base:group0 schema:name ?oldName .
              base:group0 schema:description ?oldDesc .
            }
            INSERT {
              base:group0 rdf:type foaf:Group .
              base:group0 schema:name "Group Title" .
              base:group0 schema:description "Group Description" .
            }
            WHERE
              { OPTIONAL
                  { base:group0  schema:name  ?oldName}
                OPTIONAL
                  { base:group0  schema:description  ?oldDesc}
              }
            """, GraphClient.updateGroupUpdate(group).buildRequest().toString());
    }
}
