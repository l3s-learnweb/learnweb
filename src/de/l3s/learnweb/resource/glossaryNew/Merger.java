/**
 *
 */
package de.l3s.learnweb.resource.glossaryNew;

/**
 * Merger class for merging old glossary to new
 *
 * @author Rishita
 *
 */
public class Merger
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub
        //TODO:: Get * resource with type==glossary
        /*for all glossary resource, for all glossary_id{
            GlossaryResource g = new GlossaryResource();
            //lw_glossary_resource
            1. resource_id from main resultset
            2. allowed_languages -> get language_one and language_two for lw_resource_glossary_main where resource_id is eq to r_id
            SAVE THIS HERE
            for all glossary_id{
            GlossaryEntry e = new GlossaryEntry();
            E DETAILS for lw_glossary_entry
            1. get topic1, topic2, topic3 and description from lw_glossary_details
            2. user_id from main result
            SAVE
            //lw_glossary_term
            1. get all terms details from lw_resource_glossary_terms where glossary_id is same
            2. for all terms(){
                GlossaryTerm t = new GlossaryTerm();
                add all details
                e.add(term)
                SAVE

            }

            }

        }*/

    }

}
