package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.jboss.logging.Logger;

import de.l3s.learnweb.SubmissionManager.SubmittedResources;
import de.l3s.util.Mail;
import de.l3s.util.StringHelper;

public class PeerAssessmentManager
{
    private final static Logger log = Logger.getLogger(ForumManager.class);
    private final static String PAIR_COLUMNS = "`peerassessment_id`, `assessor_user_id`, `assessed_user_id`, `survey_resource_id`, assessment_survey_resource_id, `submission_id`";

    private final Learnweb learnweb;

    protected PeerAssessmentManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    public boolean canAssessResource(User user, Resource resource)
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM `lw_submit_resource` " +
                "JOIN lw_peerassessment_paring USING(submission_id) " +
                "WHERE `resource_id` = ? AND assessor_user_id = ?");)
        {
            select.setInt(1, resource.getId());
            select.setInt(2, user.getId());
            ResultSet rs = select.executeQuery();
            return rs.next();
        }
        catch(SQLException e)
        {
            log.fatal("user: " + user + "; resource: " + resource, e);
        }
        return false;
    }

    public boolean canAssessSubmission(int assessorUserId, int assessedUserId, int submissionId)
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM lw_peerassessment_paring " +
                "WHERE `submission_id` = ? AND assessor_user_id = ? AND assessed_user_id = ?");)
        {
            select.setInt(1, submissionId);
            select.setInt(2, assessorUserId);
            select.setInt(3, assessedUserId);
            ResultSet rs = select.executeQuery();
            return rs.next();
        }
        catch(SQLException e)
        {
            log.fatal("assessorUserId: " + assessorUserId + "; assessedUserId: " + assessedUserId + "; submissionId: " + submissionId, e);
        }
        return false;
    }

    private void savePeerAssesmentPair(PeerAssesmentPair pair) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_peerassessment_paring` (" + PAIR_COLUMNS + ") VALUES (?,?,?,?,?,?)");)
        {
            ps.setInt(1, pair.getId());
            ps.setInt(2, pair.getAssessorUserId());
            ps.setInt(3, pair.getAssessedUserId());
            ps.setInt(4, pair.getPeerAssessmentSurveyResourceId());
            ps.setInt(5, pair.getAssessmentSurveyResourceId());
            ps.setInt(6, pair.getSubmissionId());
            ps.executeUpdate();
        }
    }

    public List<PeerAssesmentPair> getPeerAssesmentPairsByPeerAssesmentId(int peerAssesmentId) throws SQLException
    {
        return getPeerAssesmentPairs("SELECT " + PAIR_COLUMNS + " FROM lw_peerassessment_paring WHERE peerassessment_id = ? ORDER BY survey_resource_id", peerAssesmentId);
    }

    private List<PeerAssesmentPair> getPeerAssesmentPairs(String query, int... parameters) throws SQLException
    {
        LinkedList<PeerAssesmentPair> pairs = new LinkedList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(query);)
        {
            int i = 1;
            for(int param : parameters)
                select.setInt(i++, param);

            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                pairs.add(new PeerAssesmentPair(rs.getInt("peerassessment_id"), rs.getInt("assessor_user_id"), rs.getInt("assessed_user_id"), rs.getInt("survey_resource_id"), rs.getInt("assessment_survey_resource_id"), rs.getInt("submission_id")));
            }
        }
        return pairs;
    }

    public class PeerAssesmentPair implements Serializable
    {
        private static final long serialVersionUID = -4241273453267455330L;
        private final int id;
        private final int assessorUserId;
        private final int assessedUserId;
        private final int peerAssessmentSurveyResourceId;
        private int assessmentSurveyResourceId;
        private final int submissionId;
        private transient User assessorUser;
        private transient User assessedUser;
        private transient SurveyUserAnswers peerAssessmentUserAnswers;
        private SurveyUserAnswers assessmentUserAnswers;

        /**
         *
         * @param id
         * @param assessorUserId
         * @param assessedUserId
         * @param peerAssessmentSurveyResourceId
         * @param assessmentSurveyResourceId
         * @param submissionId
         */
        public PeerAssesmentPair(int id, int assessorUserId, int assessedUserId, int peerAssessmentSurveyResourceId, int assessmentSurveyResourceId, int submissionId)
        {
            super();
            this.id = id;
            this.assessorUserId = assessorUserId;
            this.assessedUserId = assessedUserId;
            this.peerAssessmentSurveyResourceId = peerAssessmentSurveyResourceId;
            this.assessmentSurveyResourceId = assessmentSurveyResourceId;
            this.submissionId = submissionId;
        }

        public int getPeerAssessmentSurveyResourceId()
        {
            return peerAssessmentSurveyResourceId;
        }

        public int getAssessmentSurveyResourceId()
        {
            return assessmentSurveyResourceId;
        }

        public SurveyUserAnswers getPeerAssessmentUserAnswers() throws SQLException
        {
            if(null == peerAssessmentUserAnswers) // load survey details
                peerAssessmentUserAnswers = getPeerAssessment().getAnswersOfUser(assessorUserId);
            return peerAssessmentUserAnswers;
        }

        public SurveyUserAnswers getAssessmentUserAnswers() throws SQLException
        {
            if(null == assessmentUserAnswers) // load survey details
                assessmentUserAnswers = getAssessment().getAnswersOfUser(assessorUserId);
            return assessmentUserAnswers;
        }

        public User getAssessorUser() throws SQLException
        {
            if(assessorUser == null)
                assessorUser = Learnweb.getInstance().getUserManager().getUser(assessorUserId);
            return assessorUser;
        }

        public User getAssessedUser() throws SQLException
        {
            if(assessedUser == null)
                assessedUser = Learnweb.getInstance().getUserManager().getUser(assessedUserId);
            return assessedUser;
        }

        public SurveyResource getPeerAssessment() throws SQLException
        {
            return (SurveyResource) Learnweb.getInstance().getResourceManager().getResource(peerAssessmentSurveyResourceId);
        }

        public SurveyResource getAssessment() throws SQLException
        {
            return (SurveyResource) Learnweb.getInstance().getResourceManager().getResource(assessmentSurveyResourceId);
        }

        public int getId()
        {
            return id;
        }

        public int getAssessorUserId()
        {
            return assessorUserId;
        }

        public int getAssessedUserId()
        {
            return assessedUserId;
        }

        public int getSubmissionId()
        {
            return submissionId;
        }

        @Override
        public String toString()
        {
            return "PeerAssesmentPair [id=" + id + ", assessorUserId=" + assessorUserId + ", assessedUserId=" + assessedUserId + ", surveyResourceId=" + peerAssessmentSurveyResourceId + ", submissionId=" + submissionId + "]";
        }

    }

    /********************************************************
     * ******** helper methods to support courses ***********
     *****************************************************/

    public static void main(String[] args) throws Exception
    {

        Learnweb learnweb = Learnweb.createInstance(null);
        //learnweb.getPeerAssessmentManager().taskSetupPeerAssesmentRomeLeeds();
        //learnweb.getPeerAssessmentManager().sendInvitationMail(1);

        HashMap<String, Integer> taskAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskAssessmentSurveyMapping.put("About Us page", 204316);
        taskAssessmentSurveyMapping.put("Corporate Video", 204315);
        taskAssessmentSurveyMapping.put("Fanvid", 204490);
        taskAssessmentSurveyMapping.put("Video-Mediated Interaction", 204491);
        taskAssessmentSurveyMapping.put("Weblog", 204492);

        learnweb.getPeerAssessmentManager().taskSetupAssesment(1, taskAssessmentSurveyMapping, 443);

        learnweb.onDestroy();
    }

    @SuppressWarnings("unused")
    private void taskSetupPeerAssesmentRomeLeeds() throws SQLException
    {
        // setup for EU-Leeds and Rome
        int[] courseIds = { 1301, 1297 }; // EU-Leeds and Rome
        int peerAssesmentId = 1; // manually created for now
        int assessmentFolderId = 443; // the fodler inside the assessmentgroup to store all assessment surveys

        // these surveys are used directly. One for each submission topic
        HashMap<String, Integer> taskPeerAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskPeerAssessmentSurveyMapping.put("About Us page", 214925);
        taskPeerAssessmentSurveyMapping.put("Corporate Video", 214922);
        taskPeerAssessmentSurveyMapping.put("Fanvid", 214924);
        taskPeerAssessmentSurveyMapping.put("Video-Mediated Interaction", 214923);
        taskPeerAssessmentSurveyMapping.put("Weblog", 214921);

        taskSetupPeerAssesment(peerAssesmentId, courseIds, taskPeerAssessmentSurveyMapping);

        // these surveys are copied once for each assessment pair
        HashMap<String, Integer> taskAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskAssessmentSurveyMapping.put("About Us page", 204316);
        taskAssessmentSurveyMapping.put("Corporate Video", 204315);
        taskAssessmentSurveyMapping.put("Fanvid", 204490);
        taskAssessmentSurveyMapping.put("Video-Mediated Interaction", 204491);
        taskAssessmentSurveyMapping.put("Weblog", 204492);

        taskSetupAssesment(peerAssesmentId, taskAssessmentSurveyMapping, assessmentFolderId);
    }

    private void taskSetupAssesment(int peerAssesmentId, HashMap<String, Integer> taskAssessmentSurveyMapping, int assessmentFolderId) throws SQLException
    {
        List<PeerAssesmentPair> pairs = getPeerAssesmentPairsByPeerAssesmentId(peerAssesmentId);
        PeerAssessmentManager peerAssessmentManager = learnweb.getPeerAssessmentManager();

        for(PeerAssesmentPair pair : pairs)
        {
            String submissionTitle = learnweb.getSubmissionManager().getSubmissionById(pair.getSubmissionId()).getTitle();
            Integer baseResourceId = taskAssessmentSurveyMapping.get(submissionTitle);

            // copy base survey
            SurveyResource assessmentSurvey = (SurveyResource) learnweb.getResourceManager().getResource(baseResourceId);
            assessmentSurvey = assessmentSurvey.clone();

            log.debug(assessmentSurvey);
            assessmentSurvey.setUserId(10921);
            assessmentSurvey.setGroupId(1373);
            assessmentSurvey.setFolderId(assessmentFolderId);
            assessmentSurvey.setEditable(true);
            assessmentSurvey.save();

            pair.assessmentSurveyResourceId = assessmentSurvey.getId();
            peerAssessmentManager.savePeerAssesmentPair(pair);
        }

    }

    /**
     * Sends an email to each assessor
     *
     * @param peerAssementId
     * @throws SQLException
     * @throws MessagingException
     */
    @SuppressWarnings("unused")
    private void sendInvitationMail(int peerAssementId) throws SQLException, MessagingException
    {
        List<PeerAssesmentPair> pairs = getPeerAssesmentPairsByPeerAssesmentId(peerAssementId);

        for(PeerAssesmentPair pair : pairs)
        {
            String submissionUrl = "https://learnweb.l3s.uni-hannover.de/lw/myhome/submission_resources.jsf?user_id=" + pair.getAssessedUserId() + "&submission_id=" + pair.getSubmissionId();
            String surveyUrl = "https://learnweb.l3s.uni-hannover.de/lw/survey/survey.jsf?resource_id=" + pair.getPeerAssessmentSurveyResourceId();

            Mail mail = new Mail();
            mail.setSubject("EUMADE4LL peer assessment");
            mail.setText(getEUMADe4ALLMailText(pair.getAssessorUser().getRealUsername(), submissionUrl, surveyUrl));

            mail.setRecipient(RecipientType.BCC, new InternetAddress("kemkes@kbs.uni-hannover.de"));

            mail.setRecipient(RecipientType.TO, new InternetAddress(pair.getAssessorUser().getEmail()));

            log.debug("Send to: " + pair.getAssessorUser().getEmail());
            mail.sendMail();
            // "The subject line of the email should be:
        }
    }

    private String getEUMADe4ALLMailText(String username, String submissionUrl, String surveyUrl)
    {
        return "Dear " + username + ",\r\n\r\n" +
                "you have been matched with your peer-student and now you can assess his/her assignments. To see the resources please click here\r\n" +
                submissionUrl + "\r\n" +
                "\r\n" +
                "Read and analyse them carefully then fill in the peer-assessment grid and submit it. Click here for the grid\r\n" +
                surveyUrl + "\r\n" +
                "\r\n" +
                "For this last assignment, remember to follow the instructions provided by the guidelines and to submit it by 2nd May at noon. You can save it as many times as you need to, until you are ready to submit.\r\n" +
                "\r\n" +
                "Thank you very much for your work!\r\n";
    }

    /**
     * Pair students for a given course list
     *
     * @param taskSurveyMapping
     * @throws SQLException
     */
    private void taskSetupPeerAssesment(int peerAssesmentId, int[] courses, HashMap<String, Integer> taskSurveyMapping) throws SQLException
    {
        SubmissionManager submissionManager = learnweb.getSubmissionManager();
        //UserManager um = learnweb.getUserManager();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT submission_id, COUNT(*) FROM `lw_submit` " +
                "JOIN lw_submit_status USING(submission_id) " +
                "WHERE `course_id` IN (" + StringHelper.implodeInt(courses, ",") + ") AND deleted = 0 AND title = ? " +
                "GROUP BY submission_id ORDER BY COUNT(*) DESC");)
        {
            for(String submissionTitle : taskSurveyMapping.keySet())
            {
                log.debug("Prepare submission: " + submissionTitle);

                // create two lists of users. All users of one course are either assigned to A or B.
                LinkedList<Integer> usersA = new LinkedList<>();
                LinkedList<Integer> usersB = new LinkedList<>();
                LinkedList<Integer> submissionIds = new LinkedList<>(); // all submission ids that were used by the given courses

                select.setString(1, submissionTitle);
                ResultSet rs = select.executeQuery();
                while(rs.next())
                {
                    int submissionId = rs.getInt("submission_id");
                    submissionIds.add(submissionId);
                    List<SubmittedResources> userSubmissions = submissionManager.getSubmittedResourcesGroupedByUser(submissionId);

                    List<Integer> shorterUserList = shorterList(usersA, usersB);
                    for(SubmittedResources submission : userSubmissions)
                        shorterUserList.add(submission.getUserId());
                }

                // interlace the two lists
                LinkedList<Integer> usersFinal = new LinkedList<>();
                Iterator<Integer> iteratorA = usersA.iterator();
                Iterator<Integer> iteratorB = usersB.iterator();
                while(iteratorA.hasNext() || iteratorB.hasNext())
                {
                    if(iteratorA.hasNext())
                        usersFinal.add(iteratorA.next());
                    if(iteratorB.hasNext())
                        usersFinal.add(iteratorB.next());
                }

                // pair the users
                PreparedStatement selectSubmissionId = learnweb.getConnection().prepareStatement("SELECT submission_id FROM lw_submit_status  " +
                        "WHERE `submission_id` IN (" + StringHelper.implodeInt(submissionIds, ",") + ") AND user_id = ? ");

                int peerAssessmentsurveyResourceId = taskSurveyMapping.get(submissionTitle);
                int lastUserId = usersFinal.getLast();
                for(int userId : usersFinal)
                {
                    selectSubmissionId.setInt(1, userId);
                    ResultSet submissionRs = selectSubmissionId.executeQuery();
                    if(!submissionRs.next())
                        log.error("can't get submission id");
                    int submissionId = submissionRs.getInt(1);

                    savePeerAssesmentPair(new PeerAssesmentPair(peerAssesmentId, lastUserId, userId, peerAssessmentsurveyResourceId, 0, submissionId));
                    lastUserId = userId;
                }
                selectSubmissionId.close();
            }
        }
    }

    private static <T> List<T> shorterList(List<T> listA, List<T> listB)
    {
        if(listA.size() < listB.size())
            return listA;
        return listB;
    }

    /**
     *
     *
     * create submission for new course but copy old descriptions
     * insert into lw_submit (`course_id`,`title`,`description`, `open_datetime`, `close_datetime`,`number_of_resources`,`survey_resource_id`)
     * SELECT 1338 as `course_id`,`title`,`description`,'2018-03-26 00:00:00' as `open_datetime`,'2018-05-22 23:59:59' as
     * `close_datetime`,`number_of_resources`,`survey_resource_id` FROM `lw_submit` WHERE `deleted` = 0 AND `course_id` = 1301
     *
     */

}
