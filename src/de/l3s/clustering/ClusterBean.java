package de.l3s.clustering;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.Sql;

@Named
@SessionScoped
public class ClusterBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -8913025420503947035L;
    private static final Logger log = Logger.getLogger(ClusterBean.class);

    private String jsonTextFormat = null;
    //Load from jsf pages
    private String classId;
    private String groupId;

    private List<Group> groups = new ArrayList<Group>();
    private List<List<Integer>> cluster = new ArrayList<List<Integer>>();
    private List<Integer> clusS;
    private HashMap<Integer, HashMap<String, Long>> listActivities = new HashMap<Integer, HashMap<String, Long>>();
    private HashMap<String, String> listFeatures = new HashMap<String, String>();
    private List<StudentClusterInfo> listData = new ArrayList<StudentClusterInfo>();

    //Load info to create the bubbles
    private List<StudentClusterInfo> cars1 = new ArrayList<StudentClusterInfo>();
    private List<Point> listPositions = new ArrayList<Point>();

    //Info to create the clusters
    private int[] Min = { 0, 10000 };
    private int[] Max = { 0, -10000 };
    private int numOfPoints = 0;
    private String numCluster;

    private boolean showBubble = false;
    private String feature1, feature1_name;

    //Initialize the chart
    public ClusterBean()
    {
        setShowBubble(false);
        listFeatures.put("Searching", "5");
        listFeatures.put("Open Resource", "15");
        listFeatures.put("Add Resource", "14");
        listFeatures.put("Delete Resource", "3");
        listFeatures.put("Create Group", "7");
        listFeatures.put("Group Joining", "6");
        listFeatures.put("Group Leaving", "8");

        listFeatures.put("Rating", "1");
        listFeatures.put("Tagging", "0");
        listFeatures.put("Comments", "19");
        listFeatures.put("Edit Resource", "2");
        listFeatures.put("Deleting Comments", "17");

        this.listPositions.add(new Point(131.76, 163.75));
        this.listPositions.add(new Point(361.37, 363.75));
        this.listPositions.add(new Point(146.85, 133.05));
        this.listPositions.add(new Point(323, 343.8));
        this.listPositions.add(new Point(104, 153.72));
        this.listPositions.add(new Point(307.34, 385.63));
        this.listPositions.add(new Point(144.20, 197.21));
        this.listPositions.add(new Point(382.25, 389.09));
        this.listPositions.add(new Point(189.39, 157.81));
        this.listPositions.add(new Point(384.90, 324.40));
        this.listPositions.add(new Point(155.10, 284.19));
        this.listPositions.add(new Point(315.82, 312.31));
        this.listPositions.add(new Point(293.96, 331.08));
        this.listPositions.add(new Point(280.04, 347.77));
        this.listPositions.add(new Point(285.40, 367.09));
        this.listPositions.add(new Point(168.25, 192.91));
        this.listPositions.add(new Point(295.3, 412.22));
        this.listPositions.add(new Point(315.72, 411.27));
        this.listPositions.add(new Point(329.64, 424.14));
        this.listPositions.add(new Point(149.15, 227.55));
        this.listPositions.add(new Point(367.90, 418.30));
        this.listPositions.add(new Point(286.52, 214.87));
        this.listPositions.add(new Point(489.4, 259));
        this.listPositions.add(new Point(467.9, 259.03));
        this.listPositions.add(new Point(460.6, 221.26));
        this.listPositions.add(new Point(404.71, 166.03));
        this.listPositions.add(new Point(377.11, 170.92));
        this.listPositions.add(new Point(361.43, 192.28));
        this.listPositions.add(new Point(261.58, 237.49));
        this.listPositions.add(new Point(222.75, 243.67));
        this.listPositions.add(new Point(189.12, 439.64));
        this.listPositions.add(new Point(227.99, 466.23));
        this.listPositions.add(new Point(519.23, 418.4));
        this.listPositions.add(new Point(553.74, 418.40));
        this.listPositions.add(new Point(572.74, 416.61));
        this.listPositions.add(new Point(580.33, 334.32));
        this.listPositions.add(new Point(576.6, 304.32));
        this.listPositions.add(new Point(562.44, 283.18));
        this.listPositions.add(new Point(565.44, 283.18));
        this.listPositions.add(new Point(583.0, 263.1));
        this.listPositions.add(new Point(538, 241.5));
        this.listPositions.add(new Point(418.74, 117.73));

    }

    // This info doesn't exist in the database, so I have to include it
    public String getNamesFeatures(int feature)
    {
        String name = "";
        switch(feature)
        {
        case 0:
            name = "Tagging";
            break;
        case 1:
            name = "Rating";
            break;
        case 2:
            name = "Edit Resource";
            break;
        case 3:
            name = "Delete Resource";
            break;
        case 5:
            name = "Searching";
            break;
        case 6:
            name = "Group Joining";
            break;
        case 7:
            name = "Create Group";
            break;
        case 8:
            name = "Group Leaving";
            break;
        case 14:
            name = "Add Resource";
            break;
        case 15:
            name = "Open Resource";
            break;
        case 17:
            name = "Deleting Comments";
            break;
        case 19:
            name = "Comments";
            break;
        }
        return name;
    }

    //Load and update info related to the groups
    public void changeGroup()
    {
        int idCourse = Integer.valueOf(this.classId);
        try
        {
            GroupManager usm = getLearnweb().getGroupManager();
            this.setGroups(usm.getGroupsByCourseId(idCourse));
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    //Function responsible to obtain the list of users
    public void getListUsers() throws NumberFormatException, SQLException
    {
        UserManager usm = getLearnweb().getUserManager();

        List<User> l = new ArrayList<User>();
        if(this.groupId.compareTo("0") == 0)
        {
            l = UserAssessmentBean.removeAdminUsers(usm.getUsersByCourseId(Integer.valueOf(this.classId)));
            //log.debug(l);
        }
        else
        {
            l = UserAssessmentBean.removeAdminUsers(usm.getUsersByGroupId(Integer.valueOf(this.groupId)));
            //log.debug(l);
        }

        for(User u : l)
        {
            HashMap<String, Long> listAll = new HashMap<String, Long>();
            listAll = getActivitiesClass(u.getId());
            listActivities.put(u.getId(), listAll);
            //log.debug("lista:" + u.getId() + "-" + listAll.toString());
        }
        //log.debug("-------------------------------");
    }

    //APAGAR
    public String[] selectValues(int userId, String codAc1, String codAc2) throws SQLException
    {
        String[] list = new String[3];

        Long valueX = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=" + String.valueOf(codAc1));
        Long valueY = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=" + String.valueOf(codAc2));
        list[0] = String.valueOf(userId);
        list[1] = String.valueOf(valueX);
        list[2] = String.valueOf(valueY);
        return list;
    }

    //Clean some lists related to the clusters and users
    public void cleanClusters()
    {
        this.cluster = new ArrayList<List<Integer>>();
        this.listData = new ArrayList<StudentClusterInfo>();
    }

    //Load the user's name according to his Id
    public String getUserNamebyId(int cod)
    {
        try
        {
            String userName = (String) Sql.getSingleResult("SELECT username FROM lw_user WHERE user_id=" + cod);
            return userName;
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            return null;
        }
    }

    //Function called the clusters method and 
    public void executeGeneralClusters() throws NumberFormatException, SQLException
    {
        cleanClusters();

        //getUsers by id group
        getListUsers();

        this.setShowBubble(false);

        Iterator<Integer> itr = listActivities.keySet().iterator();
        KMeans kmeans = new KMeans();
        kmeans.setNUM_CLUSTERS(Integer.valueOf(numCluster));
        kmeans.setMAX_COORDINATE(Max[0] / 35);
        kmeans.setMIN_COORDINATE(Min[0]);
        kmeans.init();

        while(itr.hasNext())
        {
            int pos = itr.next();
            kmeans.setListActivities(listActivities.get(pos));
            kmeans.addRealPoints();
            kmeans.calculate();

            //organize the clusters
            defineCluster(kmeans, pos);
        }

        int count = 0;
        for(int i = 0; i < Integer.valueOf(numCluster); i++)
        {
            int value = 0;
            //plotClusters(i, cluster.get(i)); //print the groups on console (for me)
            double r_old = 0;

            for(int j : cluster.get(i))
            {
                double r = interactionaProfileFunction(listActivities.get(j));

                StudentClusterInfo st = new StudentClusterInfo();
                st.setRadius(r);
                st.setNome(this.getUserNamebyId(j));
                st.setInfo(this.loadInfoBubble(listActivities.get(j), this.getUserNamebyId(j)));
                st.setCx(listPositions.get(value).getX() + r_old + r);
                st.setCy(listPositions.get(value).getY());

                value++;

                switch(count)
                {
                case 0:
                    st.setColor("#3182BD");
                    break;
                case 1:
                    st.setColor("#3D93C4");
                    break;
                case 2:
                    st.setColor("#42A9E5");
                    break;
                case 3:
                    st.setColor("#48B2DE");
                    break;
                case 4:
                    st.setColor("#6BAED6");
                    break;
                default:
                    break;
                }
                listData.add(st);
                st.toString();
            }
            count++;
        }

        /*log.debug("ListaData Final");
        for(StudentClusterInfo s : listData)
            log.debug("-" + s);
        */
    }

    //Verify some null values in cluster
    public boolean otherValuesNull(List<Integer> s, int pos)
    {
        int count = 0;
        for(int i = 0; i < s.size(); i++)
        {
            if((i != pos) && (s.get(i) == 0))
            {
                count++;
            }
        }
        if(count == s.size() - 1)
            return true;
        else
            return false;
    }

    public void updateVar()
    {
        for(int i = 0; i < Integer.valueOf(numCluster); i++)
        {
            this.clusS.add(i, getClusS(i));
        }
    }

    //verify if all point of cluster interaction are null
    public boolean nullAllPoints(Cluster clusters)
    {
        int notNull = 0;
        for(Point p : clusters.getPoints())
        {
            if(p.getX() == 0)
                notNull++;
        }

        if(notNull == clusters.getPoints().size())
            return true;
        return false;
    }

    //Function responsible for include the users in its cluster according the most predominant behavior
    public void defineCluster(KMeans k, int id)
    {
        List<Integer> s = new ArrayList<Integer>();

        for(int i = 0; i < Integer.valueOf(numCluster); i++)
        {
            if(nullAllPoints(k.getClusters().get(i)) == true)
                s.add(0);
            else
                s.add(k.getClusters().get(i).getPoints().size());
        }

        //log.debug("Id-" + id + ":" + s.get(0) + "," + s.get(1) + "," + s.get(2));

        int max = 0;
        for(int a = 0; a < s.size(); a++)
        {
            if((s.get(a) > max) || (s.get(a) == max))
                max = s.get(a);
        }

        for(int i = 0; i < Integer.valueOf(numCluster); i++)
        {
            ArrayList<Integer> list = new ArrayList<Integer>();
            cluster.add(list);
        }

        int add = 0;
        for(int i = 0; i < Integer.valueOf(numCluster); i++)
        {
            if((max == s.get(i)) && (add == 0))
            {
                cluster.get(i).add(id);
                add++;
            }
        }
    }

    public String loadInfoBubble(HashMap<String, Long> list, String userName)
    {
        String outcome = "-- " + userName + " -- \n";
        outcome = outcome.concat("Searching: " + list.get("Searching") + "\n");
        outcome = outcome.concat("Open Resource: " + list.get("Open Resource") + "\n");
        outcome = outcome.concat("Add Resource: " + list.get("Add Resource") + "\n");
        outcome = outcome.concat("Delete Resource: " + list.get("Delete Resource") + "\n");
        outcome = outcome.concat("Create Group: " + list.get("Create Group") + "\n");
        outcome = outcome.concat("Group Joining: " + list.get("Group Joining") + "\n");
        outcome = outcome.concat("Group Leaving: " + list.get("Group Leaving") + "\n");
        outcome = outcome.concat("Rating: " + list.get("Rating") + "\n");
        outcome = outcome.concat("Tagging: " + list.get("Tagging") + "\n");
        outcome = outcome.concat("Comments: " + list.get("Comments") + "\n");
        outcome = outcome.concat("Edit Resource: " + list.get("Edit Resource") + "\n");
        return outcome;
    }

    //Get info about user in group or class
    public HashMap<String, Long> getActivitiesClass(int userId) throws SQLException
    {
        Long searching, addResource, delResource, openResource, createGroup, groupJoining, groupLeaving;
        Long rating, tagging, editResource, commenting, deleteComments;
        HashMap<String, Long> listActUsers = new HashMap<String, Long>();

        if(this.groupId.compareTo("0") == 0)
        {
            searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=5");
            addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=15");
            delResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=14");
            openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=3");
            createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=7");
            groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=6");
            groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=8");

            rating = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=1");
            tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=0");
            editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=19");
            commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=2");
            deleteComments = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=17");
        }
        else
        {
            searching = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=5");
            addResource = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=15");
            delResource = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=14");
            openResource = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=3");
            createGroup = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=7");
            groupJoining = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=6");
            groupLeaving = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=8");

            rating = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=1");
            tagging = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=0");
            editResource = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=19");
            commenting = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=2");
            deleteComments = (Long) Sql.getSingleResult(
                    "SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + this.groupId + " AND A1.user_id=" + String.valueOf(userId) + " AND A1.action=17");

        }
        listActUsers.put("Searching", searching);
        listActUsers.put("Open Resource", openResource);
        listActUsers.put("Add Resource", addResource);
        listActUsers.put("Delete Resource", delResource);
        listActUsers.put("Create Group", createGroup);
        listActUsers.put("Group Joining", groupJoining);
        listActUsers.put("Group Leaving", groupLeaving);

        listActUsers.put("Rating", rating);
        listActUsers.put("Tagging", tagging);
        listActUsers.put("Comments", commenting);
        listActUsers.put("Edit Resource", editResource);
        listActUsers.put("Deleting Comments", deleteComments);

        Iterator<String> itr = listActUsers.keySet().iterator();
        int i = 0, im = 0;
        while(itr.hasNext())
        {
            Long current = listActUsers.get(itr.next());
            if(Max[1] < Integer.valueOf(current.toString()))
            {
                Max[0] = Max[1];
                Max[1] = Integer.valueOf(current.toString());
                if(i == 0)
                {
                    Max[0] = Max[1];
                    i++;
                }
            }
            if(Min[1] > Integer.valueOf(current.toString()))
            {
                Min[0] = Min[1];
                Min[1] = Integer.valueOf(current.toString());
                if(im == 0)
                {
                    Min[0] = Min[1];
                    im++;
                }

            }
        }
        return listActUsers;
    }

    //Radius of the bubble
    public double interactionaProfileFunction(HashMap<String, Long> listUsers)
    {
        int w1 = 4, w2 = 2, w3 = 2, w4 = 2;
        long s1, s2, s3, s4;
        double fi = 0;

        s1 = listUsers.get("Comments") + listUsers.get("Add Resource") + listUsers.get("Tagging") + listUsers.get("Rating") + listUsers.get("Create Group");
        s2 = listUsers.get("Searching") + listUsers.get("Edit Resource") + listUsers.get("Open Resource");
        s3 = listUsers.get("Group Joining") + listUsers.get("Delete Resource");
        s4 = listUsers.get("Deleting Comments") + listUsers.get("Group Leaving");

        fi = (w1 * s1 + w2 * s2 + w3 * s3 + w4 * s4) / 10;

        return fi;

    }

    public void plotClusters(int id, List<Integer> cluster) throws SQLException
    {
        // TODO
        /* use the logger instead of println
        log.debug("[Cluster: " + id + "]");
        log.debug("[Elements: \n");
        for(int p : cluster)
        {
        String u = (String) Sql.getSingleResult("SELECT username FROM lw_user WHERE user_id=" + p);
        log.debug(u);
        
        }
        log.debug("]");
        */
    }

    public String getJsonTextFormat()
    {
        return jsonTextFormat;
    }

    public void setJsonTextFormat(String jsonTextFormat)
    {
        this.jsonTextFormat = jsonTextFormat;
    }

    public String getClassId()
    {
        return classId;
    }

    public void setClassId(String classId)
    {
        this.classId = classId;
    }

    public int[] getMin()
    {
        return Min;
    }

    public void setMin(int[] min)
    {
        Min = min;
    }

    public int[] getMax()
    {
        return Max;
    }

    public void setMax(int[] max)
    {
        Max = max;
    }

    public int getNumOfPoints()
    {
        return numOfPoints;
    }

    public void setNumOfPoints(int numOfPoints)
    {
        this.numOfPoints = numOfPoints;
    }

    public boolean isShowBubble()
    {
        return showBubble;
    }

    public void setShowBubble(boolean showBubble)
    {
        this.showBubble = showBubble;
    }

    public String getFeature1()
    {
        return feature1;
    }

    public void setFeature1(String feature1)
    {
        this.feature1 = feature1;
    }

    public HashMap<String, String> getListFeatures()
    {
        return listFeatures;
    }

    public void setListFeatures(HashMap<String, String> listFeatures)
    {
        this.listFeatures = listFeatures;
    }

    public String getFeature1_name()
    {
        return feature1_name;
    }

    public void setFeature1_name(String feature1_name)
    {
        this.feature1_name = feature1_name;
    }

    public List<List<Integer>> getCluster()
    {
        return cluster;
    }

    public void setCluster(List<List<Integer>> cluster)
    {
        this.cluster = cluster;
    }

    public String getNumCluster()
    {
        return numCluster;
    }

    public void setNumCluster(String numCluster)
    {
        this.numCluster = numCluster;
    }

    public List<Integer> getClusS()
    {
        return clusS;
    }

    public void setClusS(List<Integer> clusS)
    {
        this.clusS = clusS;
    }

    public int getClusS(int i)
    {
        int val = clusS.get(i);
        return val;
    }

    public List<StudentClusterInfo> getListData()
    {
        return listData;
    }

    public void setListData(List<StudentClusterInfo> listData)
    {
        this.listData = listData;
    }

    public List<StudentClusterInfo> getCars1()
    {
        return cars1;
    }

    public void setCars1(List<StudentClusterInfo> cars1)
    {
        this.cars1 = cars1;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public List<Group> getGroups()
    {
        return groups;
    }

    public void setGroups(List<Group> groups)
    {
        this.groups = groups;
    }

}
