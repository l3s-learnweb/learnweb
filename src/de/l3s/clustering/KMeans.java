package de.l3s.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class KMeans
{
    private static final Logger log = Logger.getLogger(KMeans.class);
    //Number of Clusters. This metric should be related to the number of points
    private int NUM_CLUSTERS;
    //Min and Max X and Y
    private int MIN_COORDINATE;
    private int MAX_COORDINATE;

    private List<Point> points;
    private List<Cluster> clusters;

    private HashMap<String, Long> listActivities = new HashMap<>();

    public KMeans()
    {
        this.points = new ArrayList<>();
        this.clusters = new ArrayList<>();
    }

    //Initializes the process
    public void init()
    {
        Point p = new Point();

        //Create Clusters
        //Set Random Centroids
        log.debug("MIN:" + MIN_COORDINATE);
        log.debug("MAX:" + MAX_COORDINATE);
        for(int i = 0; i < NUM_CLUSTERS; i++)
        {
            Cluster cluster = new Cluster(i);
            Point centroid = p.createRandomPoint(MIN_COORDINATE, MAX_COORDINATE);
            cluster.setCentroid(centroid);
            this.clusters.add(cluster);
        }
        //Print Initial state
        plotClusters();
    }

    public void addRealPoints()
    {
        //Add real points
        this.points = listOfPoints();
    }

    public void plotClusters()
    {
        for(int i = 0; i < NUM_CLUSTERS; i++)
        {
            Cluster c = clusters.get(i);
            c.plotCluster();
        }
    }

    //Add the list of points into a vector
    public List<Point> listOfPoints()
    {
        List<Point> p = new ArrayList<>();
        log.debug("listOfPoints");

        Point pt = new Point(listActivities.get("Searching"), listActivities.get("Open Resource"));
        p.add(pt);
        Point pt2 = new Point(listActivities.get("Open Resource"), listActivities.get("Add Resource"));
        p.add(pt2);
        Point pt3 = new Point(listActivities.get("Add Resource"), listActivities.get("Delete Resource"));
        p.add(pt3);
        Point pt4 = new Point(listActivities.get("Delete Resource"), listActivities.get("Create Group"));
        p.add(pt4);
        Point pt5 = new Point(listActivities.get("Create Group"), listActivities.get("Group Joining"));
        p.add(pt5);
        Point pt6 = new Point(listActivities.get("Group Joining"), listActivities.get("Group Leaving"));
        p.add(pt6);
        Point pt7 = new Point(listActivities.get("Group Leaving"), listActivities.get("Rating"));
        p.add(pt7);
        Point pt8 = new Point(listActivities.get("Rating"), listActivities.get("Tagging"));
        p.add(pt8);
        Point pt9 = new Point(listActivities.get("Tagging"), listActivities.get("Comments"));
        p.add(pt9);
        Point pt10 = new Point(listActivities.get("Comments"), listActivities.get("Edit Resource"));
        p.add(pt10);
        Point pt11 = new Point(listActivities.get("Edit Resource"), listActivities.get("Deleting Comments"));
        p.add(pt11);
        Point pt12 = new Point(listActivities.get("Deleting Comments"), listActivities.get("Searching"));
        p.add(pt12);

        //for(Point pp : p)
        //log.debug(pp);

        /*Iterator<String> itr = listActivities.keySet().iterator();
        Iterator<String> itr2 = listActivities.keySet().iterator();
        while(itr.hasNext())
        {
            Long current = listActivities.get(itr.next());
            int value = Integer.valueOf(current.toString());
            while(itr2.hasNext())
            {
        	Long current2 = listActivities.get(itr2.next());
        	int value2 = Integer.valueOf(current2.toString());
        	Point pt = new Point(value, value2);
        	p.add(pt);
        	log.debug(pt.toString());
            }
        }*/

        return p;

        /*List<Point> p = new ArrayList<Point>();
        log.debug("listOfPoints");
        Iterator<String> itr = listActivities.keySet().iterator();
        while(itr.hasNext())
        {
            Long current = listActivities.get(itr.next());
            int value = Integer.valueOf(current.toString());
            Point pt = new Point(value, value);
            p.add(pt);
        }
        return p;*/
    }

    //The process to calculate the K Means, with iterating method.
    public void calculate()
    {
        boolean finish = false;
        //int iteration = 0;

        // Add in new data, one at a time, recalculating centroids with each new one. 
        while(!finish)
        {
            //Clear cluster state
            clearClusters();

            List<Point> lastCentroids = getCentroids();

            //Assign points to the closer cluster
            assignCluster();

            //Calculate new centroids.
            calculateCentroids();

            //iteration++;

            List<Point> currentCentroids = getCentroids();

            //Calculates total distance between new and old Centroids
            double distance = 0;
            Point p = new Point();
            for(int i = 0; i < lastCentroids.size(); i++)
            {
                distance += p.distance(lastCentroids.get(i), currentCentroids.get(i));
            }
            //log.debug("#################");
            //log.debug("Iteration: " + iteration);
            //log.debug("Centroid distances: " + distance);

            if(distance == 0)
            {
                finish = true;
            }
        }
    }

    private void clearClusters()
    {
        for(Cluster cluster : clusters)
        {
            cluster.clear();
        }
    }

    private List<Point> getCentroids()
    {
        List<Point> centroids = new ArrayList<>(NUM_CLUSTERS);
        for(Cluster cluster : clusters)
        {
            Point aux = cluster.getCentroid();
            Point point = new Point(aux.getX(), aux.getY());
            centroids.add(point);
        }
        return centroids;
    }

    private void assignCluster()
    {
        double max = Double.MAX_VALUE;
        double min = max;
        int cluster = 0;
        double distance = 0.0;

        for(Point point : points)
        {
            min = max;
            Point p = new Point();
            for(int i = 0; i < NUM_CLUSTERS; i++)
            {
                Cluster c = clusters.get(i);
                distance = p.distance(point, c.getCentroid());
                if(distance < min)
                {
                    min = distance;
                    cluster = i;
                }
            }
            point.setCluster(cluster);
            clusters.get(cluster).addPoint(point);
        }
    }

    private void calculateCentroids()
    {
        for(Cluster cluster : clusters)
        {
            double sumX = 0;
            double sumY = 0;
            List<Point> list = cluster.getPoints();
            int n_points = list.size();

            for(Point point : list)
            {
                sumX += point.getX();
                sumY += point.getY();
            }

            Point centroid = cluster.getCentroid();
            if(n_points > 0)
            {
                double newX = sumX / n_points;
                double newY = sumY / n_points;
                centroid.setX(newX);
                centroid.setY(newY);
            }
        }
    }

    public HashMap<String, Long> getListActivities()
    {
        return listActivities;
    }

    public void setListActivities(HashMap<String, Long> listActivities)
    {
        this.listActivities = listActivities;
    }

    public int getNUM_CLUSTERS()
    {
        return NUM_CLUSTERS;
    }

    public void setNUM_CLUSTERS(int nUM_CLUSTERS)
    {
        NUM_CLUSTERS = nUM_CLUSTERS;
    }

    public int getMIN_COORDINATE()
    {
        return MIN_COORDINATE;
    }

    public void setMIN_COORDINATE(int mIN_COORDINATE)
    {
        MIN_COORDINATE = mIN_COORDINATE;
    }

    public int getMAX_COORDINATE()
    {
        return MAX_COORDINATE;
    }

    public void setMAX_COORDINATE(int mAX_COORDINATE)
    {
        MAX_COORDINATE = mAX_COORDINATE;
    }

    public List<Point> getPoints()
    {
        return points;
    }

    public void setPoints(List<Point> points)
    {
        this.points = points;
    }

    public List<Cluster> getClusters()
    {
        return clusters;
    }

    public void setClusters(List<Cluster> clusters)
    {
        this.clusters = clusters;
    }
}
