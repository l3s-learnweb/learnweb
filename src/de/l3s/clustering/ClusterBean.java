package de.l3s.clustering;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BubbleChartModel;
import org.primefaces.model.chart.BubbleChartSeries;

import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.Sql;

@ManagedBean
@RequestScoped
public class ClusterBean extends ApplicationBean
{
    private String jsonTextFormat = null;
    private String classId;
    private HashMap<Integer, HashMap<String, Long>> listActivities = new HashMap<Integer, HashMap<String, Long>>();
    private int Min = 10000;
    private int Max = -10000;
    private int numOfPoints = 0;
    private List<Integer> cluster0 = new ArrayList<Integer>(), cluster1 = new ArrayList<Integer>(), cluster2 = new ArrayList<Integer>(), cluster3 = new ArrayList<Integer>(), cluster4 = new ArrayList<Integer>();
    private BubbleChartModel bubbleModel = new BubbleChartModel();
    private boolean showBubble = false;
    private String feature1, feature2, feature1_name, feature2_name;
    private HashMap<String, String> listFeatures = new HashMap<String, String>();

    public ClusterBean()
    {
	setShowBubble(false);

	listFeatures.put("Searching", "5");
	listFeatures.put("Downloading", "32");
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
    }

    // This info doesn't exist in the database
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
	case 32:
	    name = "Downloading";
	    break;
	}
	return name;
    }

    public void getListUsers() throws NumberFormatException, SQLException
    {
	UserManager usm = getLearnweb().getUserManager();
	List<User> l = new ArrayList<User>();
	l = usm.removeAdminUsers(usm.getUsersByCourseId(Integer.parseInt(classId)));
	System.out.println(l);

	for(User u : l)
	{
	    HashMap<String, Long> listAll = new HashMap<String, Long>();
	    listAll = getActivitiesClass(u.getId());
	    listActivities.put(u.getId(), listAll);
	    System.out.println("lista:" + u.getId() + "-" + listAll.toString());
	}
	System.out.println("____________");
    }

    public String[] selectValues(int userId, String codAc1, String codAc2) throws SQLException
    {
	String[] list = new String[4];

	Long valueX = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=" + String.valueOf(codAc1));
	Long valueY = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=" + String.valueOf(codAc2));
	list[0] = String.valueOf(userId);
	list[1] = String.valueOf(valueX);
	list[2] = String.valueOf(valueY);
	list[3] = "36";
	return list;
    }

    public void createBubblePlot() throws SQLException
    {
	String[] list0, list1, list2, list3, list4;
	bubbleModel = new BubbleChartModel();

	for(int i : cluster0)
	{
	    list0 = selectValues(i, this.feature1, this.feature2);
	    bubbleModel.add(new BubbleChartSeries(list0[0], Integer.valueOf(list0[1]), Integer.valueOf(list0[2]), 36));
	    bubbleModel.setSeriesColors("00cc66");
	}
	for(int i : cluster1)
	{
	    list1 = selectValues(i, this.feature1, this.feature2);
	    bubbleModel.add(new BubbleChartSeries(list1[0], Integer.valueOf(list1[1]), Integer.valueOf(list1[2]), 36));
	    bubbleModel.setSeriesColors("66cc66");
	}
	for(int i : cluster2)
	{
	    list2 = selectValues(i, this.feature1, this.feature2);
	    bubbleModel.add(new BubbleChartSeries(list2[0], Integer.valueOf(list2[1]), Integer.valueOf(list2[2]), 36));
	    bubbleModel.setSeriesColors("66ff66");
	}
	for(int i : cluster3)
	{
	    list3 = selectValues(i, this.feature1, this.feature2);
	    bubbleModel.add(new BubbleChartSeries(list3[0], Integer.valueOf(list3[1]), Integer.valueOf(list3[2]), 36));
	    bubbleModel.setSeriesColors("93b75f");
	}
	for(int i : cluster4)
	{
	    list4 = selectValues(i, this.feature1, this.feature2);
	    bubbleModel.add(new BubbleChartSeries(list4[0], Integer.valueOf(list4[1]), Integer.valueOf(list4[2]), 36));
	    bubbleModel.setSeriesColors("E7E658");
	}

	bubbleModel.setTitle("Bubble Chart - User Groups");
	bubbleModel.getAxis(AxisType.X).setLabel(getNamesFeatures(Integer.valueOf(this.feature1)));
	bubbleModel.setShadow(false);
	bubbleModel.setBubbleGradients(true);
	bubbleModel.setBubbleAlpha(0.5);

	bubbleModel.getAxis(AxisType.X).setTickAngle(-50);
	Axis yAxis = bubbleModel.getAxis(AxisType.Y);
	yAxis.setMin(0);
	yAxis.setMax(250);
	yAxis.setLabel(getNamesFeatures(Integer.valueOf(this.feature2)));
	yAxis.setTickAngle(50);

	this.setShowBubble(true);
    }

    public void executeClusters() throws NumberFormatException, SQLException
    {
	//metodo q for calcular os grupos
	getListUsers();
	//System.out.println("Max:" + Max + "- Min:" + Min);

	Iterator<Integer> itr = listActivities.keySet().iterator();
	KMeans kmeans = new KMeans();
	kmeans.setNUM_CLUSTERS(5);
	kmeans.setMAX_COORDINATE(Max);
	kmeans.setMIN_COORDINATE(Min);
	kmeans.init();

	while(itr.hasNext())
	{
	    int pos = itr.next();
	    kmeans.setListActivities(listActivities.get(pos));
	    //Add real values
	    kmeans.addRealPoints();
	    kmeans.calculate();

	    //organize the clusters
	    defineCluster(kmeans, pos);
	}
	plotClusters(0, cluster0);
	plotClusters(1, cluster1);
	plotClusters(2, cluster2);
	plotClusters(3, cluster3);
	plotClusters(4, cluster4);

	//Format and Set the dataset String
	//jsonTextConstrut();
	createBubblePlot();
    }

    public void defineCluster(KMeans k, int id)
    {
	int val0 = 0, val1 = 0, val2 = 0, val3 = 0, val4 = 0;
	val0 = k.getClusters().get(0).getPoints().size();
	val1 = k.getClusters().get(1).getPoints().size();
	val2 = k.getClusters().get(2).getPoints().size();
	val3 = k.getClusters().get(3).getPoints().size();
	val4 = k.getClusters().get(4).getPoints().size();
	System.out.println("val0:" + val0 + " val1:" + val1 + " val2:" + val2 + " val3:" + val3 + " val4:" + val4);

	int max = Math.max(val2, (Math.max(val0, val1)));
	int max1 = Math.max(val4, (Math.max(max, val3)));

	if(max1 == val0)
	    this.cluster0.add(id);
	if(max1 == val1)
	    this.cluster1.add(id);
	if(max1 == val2)
	    this.cluster2.add(id);
	if(max1 == val3)
	    this.cluster3.add(id);
	if(max1 == val4)
	    this.cluster4.add(id);
    }

    public HashMap<String, Long> getActivitiesClass(int userId) throws SQLException
    {
	HashMap<String, Long> listActUsers = new HashMap<String, Long>();

	Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=5");
	Long downloading = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=32");
	Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=15");
	Long delResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=14");
	Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=3");
	Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=7");
	Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=6");
	Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=8");

	Long rating = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=1");
	Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=0");
	Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=19");
	Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=2");
	Long deleteComments = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.classId + " AND A1.user_id =" + String.valueOf(userId) + " AND A1.action=17");

	listActUsers.put("Searching", searching);
	listActUsers.put("Downloading", downloading);
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
	while(itr.hasNext())
	{
	    Long current = listActUsers.get(itr.next());
	    if(Max < Integer.valueOf(current.toString()))
		Max = Integer.valueOf(current.toString());
	    if(Min > Integer.valueOf(current.toString()))
		Min = Integer.valueOf(current.toString());
	}
	return listActUsers;
    }

    public void plotClusters(int id, List<Integer> cluster)
    {
	System.out.println("[Cluster: " + id + "]");
	System.out.println("[Elements: \n");
	for(int p : cluster)
	{
	    System.out.println(p);
	}
	System.out.println("]");
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

    public int getMin()
    {
	return Min;
    }

    public void setMin(int min)
    {
	Min = min;
    }

    public int getMax()
    {
	return Max;
    }

    public void setMax(int max)
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

    public BubbleChartModel getBubbleModel()
    {
	return bubbleModel;
    }

    public void setBubbleModel(BubbleChartModel bubbleModel)
    {
	this.bubbleModel = bubbleModel;
    }

    public boolean isShowBubble()
    {
	return showBubble;
    }

    public void setShowBubble(boolean showBubble)
    {
	this.showBubble = showBubble;
    }

    public String getFeature2()
    {
	return feature2;
    }

    public void setFeature2(String feature2)
    {
	this.feature2 = feature2;
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

    public String getFeature2_name()
    {
	return feature2_name;
    }

    public void setFeature2_name(String feature2_name)
    {
	this.feature2_name = feature2_name;
    }

}
