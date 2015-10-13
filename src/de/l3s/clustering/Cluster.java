package de.l3s.clustering;

import java.util.ArrayList;
import java.util.List;

public class Cluster
{
    public List<Point> points;
    public Point centroid;
    public int id;

    //Creates a new Cluster
    public Cluster(int id)
    {
	this.id = id;
	this.points = new ArrayList<Point>();
	this.centroid = null;
    }

    public List<Point> getPoints()
    {
	return points;
    }

    public void addPoint(Point point)
    {
	points.add(point);
    }

    public void setPoints(List<Point> points)
    {
	this.points = points;
    }

    public Point getCentroid()
    {
	return centroid;
    }

    public void setCentroid(Point centroid)
    {
	this.centroid = centroid;
    }

    public int getId()
    {
	return id;
    }

    public void clear()
    {
	points.clear();
    }

    public void plotCluster()
    {
	System.out.println("[Cluster: " + id + "]");
	System.out.println("[Centroid: " + centroid + "]");
	System.out.println("[Points: \n");
	for(Point p : points)
	{
	    System.out.println(p);
	}
	System.out.println("]");
    }

}
