package de.l3s.learnweb.resource.peerAssessment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.jboss.logging.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.submission.SubmissionManager;
import de.l3s.learnweb.resource.submission.SubmissionManager.SubmittedResources;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;
import de.l3s.util.email.Mail;

public class PeerAssessmentManager
{
    private final static Logger log = Logger.getLogger(PeerAssessmentManager.class);
    private final static String PAIR_COLUMNS = "`peerassessment_id`, `assessor_user_id`, `assessed_user_id`, `survey_resource_id`, assessment_survey_resource_id, `submission_id`";

    private final Learnweb learnweb;

    public PeerAssessmentManager(Learnweb learnweb) throws SQLException
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

    private void savePeerAssessmentPair(PeerAssessmentPair pair) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("INSERT INTO `lw_peerassessment_paring` (" + PAIR_COLUMNS + ") VALUES (?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE peerassessment_id=VALUES(peerassessment_id), assessor_user_id=VALUES(assessor_user_id), survey_resource_id=VALUES(survey_resource_id), survey_resource_id=VALUES(survey_resource_id), assessment_survey_resource_id=VALUES(assessment_survey_resource_id), submission_id=VALUES(submission_id)");)
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

    /**
     * Returns all peer assessment pairs for the given assessment id
     *
     * @param peerAssessmentId
     * @return
     * @throws SQLException
     */
    public List<PeerAssessmentPair> getPairsByPeerAssessmentId(int peerAssessmentId) throws SQLException
    {
        return getPeerAssessmentPairs("SELECT " + PAIR_COLUMNS + " FROM lw_peerassessment_paring WHERE peerassessment_id = ? and deleted = 0 ORDER BY survey_resource_id", peerAssessmentId);
    }

    /**
     * Returns all assessment pairs that the given user has assessed
     *
     * @param userId
     * @return
     * @throws SQLException
     */
    public List<PeerAssessmentPair> getPairsByAssessorUserId(int userId) throws SQLException
    {
        return getPeerAssessmentPairs("SELECT " + PAIR_COLUMNS + " FROM lw_peerassessment_paring WHERE assessor_user_id = ?", userId);
    }

    /**
     * Returns all assessment pairs that the given user has been assessed by
     *
     * @param userId
     * @return
     * @throws SQLException
     */
    public List<PeerAssessmentPair> getPairsByAssessedUserId(int userId) throws SQLException
    {
        return getPeerAssessmentPairs("SELECT " + PAIR_COLUMNS + " FROM lw_peerassessment_paring WHERE assessed_user_id = ? and deleted = 0", userId);
    }

    /**
     * Checks whether the given users and the survey are part of a peer assessment
     *
     * @param peerAssessmentSurveyResourceId
     * @param assessedUserId
     * @return
     * @throws SQLException
     */
    public PeerAssessmentPair getPair(int peerAssessmentSurveyResourceId, int assessorUserId, int assessedUserId) throws SQLException
    {
        List<PeerAssessmentPair> pairs = getPeerAssessmentPairs("SELECT " + PAIR_COLUMNS + " FROM lw_peerassessment_paring WHERE survey_resource_id = ? AND assessor_user_id = ? AND assessed_user_id = ?", peerAssessmentSurveyResourceId, assessorUserId, assessedUserId);

        // this query can not return more than one result
        switch(pairs.size())
        {
        case 0:
            return null;
        case 1:
            return pairs.get(0);
        default:
            throw new IllegalStateException();
        }
    }

    private List<PeerAssessmentPair> getPeerAssessmentPairs(String query, int... parameters) throws SQLException
    {
        LinkedList<PeerAssessmentPair> pairs = new LinkedList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(query);)
        {
            int i = 1;
            for(int param : parameters)
                select.setInt(i++, param);

            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                pairs.add(new PeerAssessmentPair(rs.getInt("peerassessment_id"), rs.getInt("assessor_user_id"), rs.getInt("assessed_user_id"), rs.getInt("survey_resource_id"), rs.getInt("assessment_survey_resource_id"), rs.getInt("submission_id")));
            }
        }
        return pairs;
    }

    /********************************************************
     * ******** helper methods to setup courses ***********
     *****************************************************/

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception
    {

        Learnweb learnweb = Learnweb.createInstance(null);
        PeerAssessmentManager pam = learnweb.getPeerAssessmentManager();

        //pam.taskSetupPeerAssessmentAarhusLateSubmission();
        //pam.sendInvitationReminderMail(2);

        //pam.sendConsentMail();

        learnweb.onDestroy();
    }

    @SuppressWarnings("unused")
    private void taskSetupPeerAssessmentAarhusLateSubmission() throws SQLException
    {
        // setup for EU-AarhusFlorenceMessina
        int peerAssessmentId = 3; // manually created for now
        int assessmentFolderId = 466; // the folder inside the assessment group to store all assessment surveys

        // these surveys are used directly. One for each submission topic
        HashMap<String, Integer> taskPeerAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskPeerAssessmentSurveyMapping.put("About Us page", 217735);
        taskPeerAssessmentSurveyMapping.put("Corporate Videos", 217734);
        taskPeerAssessmentSurveyMapping.put("Weblogs", 217733);

        // generate base entries manually

        // these surveys are copied once for each assessment pair
        HashMap<String, Integer> taskAssessmentSurveyMapping = new HashMap<>();
        taskAssessmentSurveyMapping.put("About Us page", 204316);
        taskAssessmentSurveyMapping.put("Corporate Videos", 204315);
        taskAssessmentSurveyMapping.put("Weblogs", 204492);

        HashMap<Integer, String> userTaskyMapping = new HashMap<>();
        userTaskyMapping.put(11423, "About Us page");
        userTaskyMapping.put(11952, "Corporate Videos");
        userTaskyMapping.put(11394, "Weblogs");
        userTaskyMapping.put(11429, "Weblogs");
        userTaskyMapping.put(11448, "Weblogs");
        userTaskyMapping.put(11514, "Weblogs");

        List<PeerAssessmentPair> pairs = getPairsByPeerAssessmentId(peerAssessmentId);
        PeerAssessmentManager peerAssessmentManager = learnweb.getPeerAssessmentManager();

        for(PeerAssessmentPair pair : pairs)
        {
            String task = userTaskyMapping.get(pair.getAssessedUserId());
            Integer baseResourceId = taskAssessmentSurveyMapping.get(task);

            // copy base survey
            SurveyResource assessmentSurvey = (SurveyResource) learnweb.getResourceManager().getResource(baseResourceId);
            assessmentSurvey = assessmentSurvey.clone();

            log.debug(pair.getAssessedUserId() + " - " + assessmentSurvey);
            assessmentSurvey.setUserId(10921);
            assessmentSurvey.setGroupId(1373);
            assessmentSurvey.setFolderId(assessmentFolderId);
            assessmentSurvey.setSaveable(true);
            assessmentSurvey.save();

            pair.setAssessmentSurveyResourceId(assessmentSurvey.getId());
            peerAssessmentManager.savePeerAssessmentPair(pair);
        }

    }

    @SuppressWarnings("unused")
    private void taskSetupPeerAssessmentAarhusFlorenceMessina() throws SQLException
    {
        // setup for EU-AarhusFlorenceMessina
        int[] courseIds = { 1338, 1349, 1348 }; // AarhusFlorenceMessina
        int peerAssessmentId = 2; // manually created for now
        int assessmentFolderId = 463; // the folder inside the assessment group to store all assessment surveys

        // these surveys are used directly. One for each submission topic
        HashMap<String, Integer> taskPeerAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskPeerAssessmentSurveyMapping.put("About Us page", 217538);
        taskPeerAssessmentSurveyMapping.put("Corporate Video", 217539);
        taskPeerAssessmentSurveyMapping.put("Fanvid", 217537);
        taskPeerAssessmentSurveyMapping.put("Video-Mediated Interaction", 217536);
        taskPeerAssessmentSurveyMapping.put("Weblog", 217535);

        //taskSetupPeerAssessment(peerAssessmentId, courseIds, taskPeerAssessmentSurveyMapping);

        // these surveys are copied once for each assessment pair
        HashMap<String, Integer> taskAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskAssessmentSurveyMapping.put("About Us page", 204316);
        taskAssessmentSurveyMapping.put("Corporate Video", 204315);
        taskAssessmentSurveyMapping.put("Fanvid", 204490);
        taskAssessmentSurveyMapping.put("Video-Mediated Interaction", 204491);
        taskAssessmentSurveyMapping.put("Weblog", 204492);

        taskSetupAssessment(peerAssessmentId, taskAssessmentSurveyMapping, assessmentFolderId);
    }

    @SuppressWarnings("unused")
    private void taskSetupPeerAssessmentRomeLeeds() throws SQLException
    {
        // setup for EU-Leeds and Rome
        int[] courseIds = { 1301, 1297 }; // EU-Leeds and Rome
        int peerAssessmentId = 1; // manually created for now
        int assessmentFolderId = 443; // the folder inside the assessment group to store all assessment surveys

        // these surveys are used directly. One for each submission topic
        HashMap<String, Integer> taskPeerAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskPeerAssessmentSurveyMapping.put("About Us page", 214925);
        taskPeerAssessmentSurveyMapping.put("Corporate Video", 214922);
        taskPeerAssessmentSurveyMapping.put("Fanvid", 214924);
        taskPeerAssessmentSurveyMapping.put("Video-Mediated Interaction", 214923);
        taskPeerAssessmentSurveyMapping.put("Weblog", 214921);

        taskSetupPeerAssessment(peerAssessmentId, courseIds, taskPeerAssessmentSurveyMapping);

        // these surveys are copied once for each assessment pair
        HashMap<String, Integer> taskAssessmentSurveyMapping = new HashMap<String, Integer>();
        taskAssessmentSurveyMapping.put("About Us page", 204316);
        taskAssessmentSurveyMapping.put("Corporate Video", 204315);
        taskAssessmentSurveyMapping.put("Fanvid", 204490);
        taskAssessmentSurveyMapping.put("Video-Mediated Interaction", 204491);
        taskAssessmentSurveyMapping.put("Weblog", 204492);

        taskSetupAssessment(peerAssessmentId, taskAssessmentSurveyMapping, assessmentFolderId);
    }

    private void taskSetupAssessment(int peerAssessmentId, HashMap<String, Integer> taskAssessmentSurveyMapping, int assessmentFolderId) throws SQLException
    {
        List<PeerAssessmentPair> pairs = getPairsByPeerAssessmentId(peerAssessmentId);
        PeerAssessmentManager peerAssessmentManager = learnweb.getPeerAssessmentManager();

        for(PeerAssessmentPair pair : pairs)
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
            assessmentSurvey.setSaveable(true);
            assessmentSurvey.save();

            pair.setAssessmentSurveyResourceId(assessmentSurvey.getId());
            peerAssessmentManager.savePeerAssessmentPair(pair);
        }

    }

    @SuppressWarnings("unused")
    private void sendInvitationReminderMail(int peerAssementId) throws SQLException, MessagingException
    {
        List<PeerAssessmentPair> pairs = getPairsByPeerAssessmentId(peerAssementId);

        for(PeerAssessmentPair pair : pairs)
        {
            if(pair.getPeerAssessmentUserAnswers().isSubmitted())
                continue;

            String submissionUrl = "https://learnweb.l3s.uni-hannover.de/lw/myhome/submission_resources.jsf?user_id=" + pair.getAssessedUserId() + "&submission_id=" + pair.getSubmissionId();
            String surveyUrl = "https://learnweb.l3s.uni-hannover.de/lw/survey/survey.jsf?resource_id=" + pair.getPeerAssessmentSurveyResourceId();

            Mail mail = new Mail();
            mail.setSubject("EUMADE4LL peer assessment");
            mail.setText("You haven't submitted yet.\nThe deadline has been extended until midnight.\n\nOriginal message:\n--------\n\n" + getEUMADe4ALLInvitationMailText(pair.getAssessorUser().getRealUsername(), submissionUrl, surveyUrl));

            mail.setRecipient(RecipientType.BCC, new InternetAddress("kemkes@kbs.uni-hannover.de"));

            mail.setRecipient(RecipientType.TO, new InternetAddress(pair.getAssessorUser().getEmail()));

            String otherMailAdress = pair.getAssessorUser().getStudentId() + "@post.au.dk";
            if(!otherMailAdress.equals(pair.getAssessorUser().getEmail()))
                mail.setRecipient(RecipientType.CC, new InternetAddress(otherMailAdress));

            log.debug("Send to: " + pair.getAssessorUser().getEmail());
            mail.sendMail();

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
        List<PeerAssessmentPair> pairs = getPairsByPeerAssessmentId(peerAssementId);

        for(PeerAssessmentPair pair : pairs)
        {
            String submissionUrl = "https://learnweb.l3s.uni-hannover.de/lw/myhome/submission_resources.jsf?user_id=" + pair.getAssessedUserId() + "&submission_id=" + pair.getSubmissionId();
            String surveyUrl = "https://learnweb.l3s.uni-hannover.de/lw/survey/survey.jsf?resource_id=" + pair.getPeerAssessmentSurveyResourceId();

            Mail mail = new Mail();
            mail.setSubject("EUMADE4LL peer assessment");
            mail.setText(getEUMADe4ALLInvitationMailText(pair.getAssessorUser().getRealUsername(), submissionUrl, surveyUrl));

            mail.setRecipient(RecipientType.BCC, new InternetAddress("kemkes@kbs.uni-hannover.de"));

            mail.setRecipient(RecipientType.TO, new InternetAddress(pair.getAssessorUser().getEmail()));

            log.debug("Send to: " + pair.getAssessorUser().getEmail());
            mail.sendMail();
        }
    }

    /**
     * Sends the results of the assessment to the assessed users
     *
     * @param peerAssementId
     * @throws SQLException
     * @throws MessagingException
     */
    @SuppressWarnings("unused")
    private void sendResultMail(int peerAssementId) throws SQLException, MessagingException
    {
        List<PeerAssessmentPair> pairs = getPairsByPeerAssessmentId(peerAssementId);

        for(PeerAssessmentPair pair : pairs)
        {
            // TODO check if peer assessment was submitted
            String peerAssessmentUrl = "https://learnweb.l3s.uni-hannover.de/lw/survey/answer.jsf?resource_id=" + pair.getPeerAssessmentSurveyResourceId() + "&user_id=" + pair.getAssessorUserId();
            String teacherAssessmentUrl = "https://learnweb.l3s.uni-hannover.de/lw/survey/answer.jsf?resource_id=" + pair.getAssessmentSurveyResourceId(); // TODO get teacher id

            Mail mail = new Mail();
            mail.setSubject("EUMADE4LL assessment");
            mail.setText(getEUMADe4ALLResultMailText(pair.getAssessedUser().getRealUsername(), peerAssessmentUrl, teacherAssessmentUrl));

            mail.setRecipient(RecipientType.BCC, new InternetAddress("kemkes@kbs.uni-hannover.de"));

            //mail.setRecipient(RecipientType.TO, new InternetAddress(pair.getAssessedUser().getEmail()));

            log.debug("Send to: " + pair.getAssessedUser().getEmail());
            mail.sendMail();
        }
    }

    /**
     * Sends an email to all eumade4all students who submitted successfully
     *
     * @throws SQLException
     * @throws MessagingException
     */
    @SuppressWarnings("unused")
    private void sendConsentMail() throws SQLException, MessagingException
    {
        for(int peerAssessmentId = 1; peerAssessmentId <= 3; peerAssessmentId++)
        {
            List<PeerAssessmentPair> pairs = getPairsByPeerAssessmentId(peerAssessmentId);

            for(PeerAssessmentPair pair : pairs)
            {
                // check if user is part of aarhus course
                Stream<Course> courses = pair.getAssessedUser().getCourses().stream();
                boolean aarhusUser = 1 == courses.filter(course -> course.getTitle().equals("EU-Aarhus")).collect(Collectors.counting());

                if(!aarhusUser)
                {
                    System.out.println(pair.getAssessedUser().getRealUsername() + " <" + pair.getAssessedUser().getEmail() + ">");
                    //System.out.println(pair.getAssessedUser().getEmail());
                }
                /*
                Mail mail = new Mail();
                mail.setSubject("EUMADE4LL assessment");
                mail.setText(getEUMADe4ALLConsentMailText(pair.getAssessedUser().getRealUsername()));

                mail.setRecipient(RecipientType.BCC, new InternetAddress("kemkes@kbs.uni-hannover.de"));
                */

                //mail.setRecipient(RecipientType.TO, new InternetAddress(pair.getAssessedUser().getEmail()));

                //log.debug("Send to: " + pair.getAssessedUser().getEmail());
                // mail.sendMail();
            }
        }
    }

    private String getEUMADe4ALLInvitationMailText(String username, String submissionUrl, String surveyUrl)
    {
        return "Dear " + username + ",\r\n\r\n" +
                "you have been matched with your peer-student and now you can assess his/her assignments. To see the resources please click here\r\n" +
                submissionUrl + "\r\n" +
                "\r\n" +
                "Read and analyse them carefully then fill in the peer-assessment grid and submit it. Click here for the grid\r\n" +
                surveyUrl + "\r\n" +
                "\r\n" +
                "For this last assignment, remember to follow the instructions provided by the guidelines and to submit it by May 31st at noon. You can save it as many times as you need to, until you are ready to submit.\r\n" +
                "\r\n" +
                "Thank you very much for your work!\r\n";

        // TODO change text
    }

    private String getEUMADe4ALLResultMailText(String username, String peerAssessmentUrl, String assessmentUrl)
    {
        return "Dear " + username + ",\r\n\r\n" +
                "teacher assessment: \r\n" +
                assessmentUrl + "\r\n" +
                "\r\n" +
                "student assessment\r\n" +
                peerAssessmentUrl + "\r\n" +
                "\r\n" +
                "For this last assignment, remember to follow the instructions provided by the guidelines and to submit it by 2nd May at noon. You can save it as many times as you need to, until you are ready to submit.\r\n" +
                "\r\n" +
                "Thank you very much for your work!\r\n";
    }

    @SuppressWarnings("unused")
    private String getEUMADe4ALLConsentMailText(String username)
    {
        return "Dear " + username + ",\r\n\r\n" +
                "teacher assessment: \r\n" +
                "\r\n" +
                "\r\n" +
                "student assessment\r\n" +
                "\r\n" +
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
    private void taskSetupPeerAssessment(int peerAssessmentId, int[] courses, HashMap<String, Integer> taskSurveyMapping) throws SQLException
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

                int peerAssessmentSurveyResourceId = taskSurveyMapping.get(submissionTitle);
                int lastUserId = usersFinal.getLast();
                for(int userId : usersFinal)
                {
                    selectSubmissionId.setInt(1, userId);
                    ResultSet submissionRs = selectSubmissionId.executeQuery();
                    if(!submissionRs.next())
                        log.error("can't get submission id");
                    int submissionId = submissionRs.getInt(1);

                    savePeerAssessmentPair(new PeerAssessmentPair(peerAssessmentId, lastUserId, userId, peerAssessmentSurveyResourceId, 0, submissionId));
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
